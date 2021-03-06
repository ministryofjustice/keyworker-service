package uk.gov.justice.digital.hmpps.keyworker.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.digital.hmpps.keyworker.model.AllocationReason;
import uk.gov.justice.digital.hmpps.keyworker.model.AllocationType;
import uk.gov.justice.digital.hmpps.keyworker.model.DeallocationReason;
import uk.gov.justice.digital.hmpps.keyworker.model.OffenderKeyworker;
import uk.gov.justice.digital.hmpps.keyworker.model.OffenderKeyworker.OffenderKeyworkerBuilder;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
@WithMockUser
class OffenderKeyworkerRepositoryTest {

    private static final LocalDateTime ASSIGNED_DATE_TIME = LocalDateTime.of(2016, 1, 2, 3, 4, 5);
    private static final LocalDateTime EXPIRY_DATE_TIME = LocalDateTime.of(2020, 12, 30, 9, 34, 56);
    private static final String AGENCY_ID_LEI = "LEI";
    private static long currentId;

    @Autowired
    private OffenderKeyworkerRepository repository;

    @Test
    void findByOffenderNoIn_ShouldReturnAllOffendersInList() {
        final var offender1 = validEntityBuilder()
            .offenderNo("A1111AA")
            .build();
        repository.save(offender1);
        final var offender2 = validEntityBuilder()
            .offenderNo("A2222AA")
            .build();
        repository.save(offender2);
        final var offender3 = validEntityBuilder()
            .offenderNo("A3333AA")
            .build();
        repository.save(offender3);

        final var foundOffenders = repository.findByOffenderNoIn(List.of(offender1.getOffenderNo(), offender2.getOffenderNo()));

        assertThat(foundOffenders).containsExactlyInAnyOrder(offender1, offender2);

        repository.deleteAll();
    }

    @Test
    void givenATransientOffenderKeyworkerWhenPersistedItShoudBeRetrievableById() {

        final var transientEntity = transientEntity();

        final var entity = transientEntity.toBuilder().build();

        final var persistedEntity = repository.save(entity);

        TestTransaction.flagForCommit();
        TestTransaction.end();

        assertThat(persistedEntity.getOffenderKeyworkerId()).isNotNull();

        TestTransaction.start();

        final var retrievedEntity = repository.findById(entity.getOffenderKeyworkerId()).orElseThrow();

        // equals only compares the business key columns: offenderBookingId, staffId and assignedDateTime
        assertThat(retrievedEntity).isEqualTo(transientEntity);

        assertThat(retrievedEntity.isActive()).isEqualTo(transientEntity.isActive());
        assertThat(retrievedEntity.getAllocationType()).isEqualTo(transientEntity.getAllocationType());
        assertThat(retrievedEntity.getUserId()).isEqualTo(transientEntity.getUserId());
        assertThat(retrievedEntity.getPrisonId()).isEqualTo(transientEntity.getPrisonId());
        assertThat(retrievedEntity.getExpiryDateTime()).isEqualTo(transientEntity.getExpiryDateTime());
        assertThat(retrievedEntity.getDeallocationReason()).isEqualTo(transientEntity.getDeallocationReason());
        assertThat(retrievedEntity.getCreateUserId()).isEqualTo("user");
        assertThat(retrievedEntity.getCreationDateTime()).isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.HOURS));
        assertThat(retrievedEntity.getCreationDateTime()).isCloseTo(persistedEntity.getCreationDateTime(), within(1, ChronoUnit.MINUTES));
        assertThat(retrievedEntity.getModifyDateTime()).isCloseTo(persistedEntity.getCreationDateTime(), within(1, ChronoUnit.MINUTES));
        assertThat(retrievedEntity.getModifyDateTime()).isCloseTo(persistedEntity.getModifyDateTime(), within(1, ChronoUnit.MINUTES));
        assertThat(retrievedEntity.getModifyUserId()).isEqualTo("user");
    }

    @Test
    void givenAPersistentInstanceThenNullableValuesAreUpdateable() throws InterruptedException {

        final var entity = repository.save(transientEntity());
        TestTransaction.flagForCommit();
        TestTransaction.end();

        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("Modify User Id", "pw"));
        TestTransaction.start();
        final var retrievedEntity = repository.findById(entity.getOffenderKeyworkerId()).orElseThrow();

        assertThat(retrievedEntity.getModifyDateTime()).isEqualTo(retrievedEntity.getCreationDateTime());
        assertThat(retrievedEntity.getModifyUserId()).isEqualTo(retrievedEntity.getCreateUserId());

        retrievedEntity.setDeallocationReason(DeallocationReason.TRANSFER);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        Thread.sleep(2L); // just long enough to make the modifyDateTime different to the creationDateTime
        TestTransaction.start();

        final var persistedUpdates = repository.findById(entity.getOffenderKeyworkerId()).orElseThrow();

        assertThat(persistedUpdates.getModifyDateTime()).isAfter(persistedUpdates.getCreationDateTime());
        assertThat(persistedUpdates.getModifyUserId()).isEqualTo("Modify User Id");
    }

    @Test
    void shouldReturnCountByStaffIdAndPrisonIdAndActive() {
        final var UNKNOWN_STAFFID = 98765L;

        final var nextId = nextId();
        repository.save(buildEntity(nextId));

        final var nextIdProvisional = nextId();
        final var provisionalAllocation = buildEntity(nextIdProvisional);
        provisionalAllocation.setAllocationType(AllocationType.PROVISIONAL);
        repository.save(provisionalAllocation);

        TestTransaction.flagForCommit();
        TestTransaction.end();

        assertThat(repository.countByStaffIdAndPrisonIdAndActiveAndAllocationTypeIsNot(nextId, AGENCY_ID_LEI, true, AllocationType.PROVISIONAL)).isEqualTo(1);
        assertThat(repository.countByStaffIdAndPrisonIdAndActiveAndAllocationTypeIsNot(nextId, AGENCY_ID_LEI, false, AllocationType.PROVISIONAL)).isEqualTo(0);
        assertThat(repository.countByStaffIdAndPrisonIdAndActiveAndAllocationTypeIsNot(UNKNOWN_STAFFID, AGENCY_ID_LEI, true, AllocationType.PROVISIONAL)).isEqualTo(0);

        repository.deleteAll();
    }

    @Test
    void shouldDeleteProvisionalRows() {

        var entity1 = buildEntity(nextId());
        var entity2 = buildEntity(nextId());
        entity1.setAllocationType(AllocationType.PROVISIONAL);
        entity2.setAllocationType(AllocationType.PROVISIONAL);
        entity1 = repository.save(entity1);
        entity2 = repository.save(entity2);
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        assertThat(repository.deleteExistingProvisionals(AGENCY_ID_LEI)).isEqualTo(2);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();
        assertThat(repository.findById(entity1.getOffenderKeyworkerId())).isEmpty();
        assertThat(repository.findById(entity2.getOffenderKeyworkerId())).isEmpty();
    }

    @Test
    void shouldUpdateProvisionalRows() {

        var entity1 = buildEntity(nextId());
        var entity2 = buildEntity(nextId());
        entity1.setAllocationType(AllocationType.PROVISIONAL);
        entity2.setAllocationType(AllocationType.PROVISIONAL);
        entity1 = repository.save(entity1);
        entity2 = repository.save(entity2);
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        assertThat(repository.confirmProvisionals(AGENCY_ID_LEI)).isEqualTo(2);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        assertThat(repository.findById(entity1.getOffenderKeyworkerId()).orElseThrow().getAllocationType()).isEqualTo(AllocationType.AUTO);
        assertThat(repository.findById(entity2.getOffenderKeyworkerId()).orElseThrow().getAllocationType()).isEqualTo(AllocationType.AUTO);
    }

    @Test
    public void deleteByOffenderNo() {
        final var offenderNo = "A12345B";
        final var transientEntity = transientEntity().toBuilder().offenderNo(offenderNo).build();
        repository.save(transientEntity);
        final var transientEntity2 = transientEntity().toBuilder().offenderNo(offenderNo).build();
        repository.save(transientEntity2);
        assertThat(repository.findByOffenderNo(offenderNo)).hasSize(2);

        assertThat(repository.deleteByOffenderNo(offenderNo)).isEqualTo(2);

        assertThat(repository.findByOffenderNo(offenderNo)).hasSize(0);
    }

    private OffenderKeyworker transientEntity() {
        return buildEntity(nextId());
    }

    private OffenderKeyworker buildEntity(final Long staffId) {
        return OffenderKeyworker
                .builder()
                .offenderNo("A1234AA")
                .staffId(staffId)
                .assignedDateTime(ASSIGNED_DATE_TIME)
                .active(true)
                .allocationReason(AllocationReason.MANUAL)
                .allocationType(AllocationType.AUTO)
                .userId("The Assigning User")
                .prisonId(AGENCY_ID_LEI)
                .expiryDateTime(EXPIRY_DATE_TIME)
                .deallocationReason(DeallocationReason.KEYWORKER_STATUS_CHANGE)
                //.createUpdate(creationTimeInfo())
                .build();
    }

    private OffenderKeyworkerBuilder validEntityBuilder() {
        return OffenderKeyworker
            .builder()
            .offenderNo("A1234AA")
            .staffId(123L)
            .assignedDateTime(ASSIGNED_DATE_TIME)
            .allocationReason(AllocationReason.MANUAL)
            .allocationType(AllocationType.AUTO)
            .userId("The Assigning User")
            .prisonId(AGENCY_ID_LEI);
    }

    private static long nextId() {
        return currentId++;
    }
}
