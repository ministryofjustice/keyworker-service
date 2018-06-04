package uk.gov.justice.digital.hmpps.keyworker.services;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import uk.gov.justice.digital.hmpps.keyworker.dto.*;
import uk.gov.justice.digital.hmpps.keyworker.model.*;
import uk.gov.justice.digital.hmpps.keyworker.repository.KeyworkerRepository;
import uk.gov.justice.digital.hmpps.keyworker.repository.OffenderKeyworkerRepository;
import uk.gov.justice.digital.hmpps.keyworker.security.AuthenticationFacade;
import uk.gov.justice.digital.hmpps.keyworker.utils.ConversionHelper;

import javax.persistence.EntityNotFoundException;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static uk.gov.justice.digital.hmpps.keyworker.model.KeyworkerStatus.INACTIVE;

@Service
@Transactional
@Validated
@Slf4j
public class KeyworkerService  {
    private final AuthenticationFacade authenticationFacade;
    private final OffenderKeyworkerRepository repository;
    private final KeyworkerRepository keyworkerRepository;
    private final KeyworkerAllocationProcessor processor;
    private final PrisonSupportedService prisonSupportedService;
    private final NomisService nomisService;
    private final DeallocateJob deallocateJob;

    public KeyworkerService(AuthenticationFacade authenticationFacade,
                            OffenderKeyworkerRepository repository,
                            KeyworkerRepository keyworkerRepository,
                            KeyworkerAllocationProcessor processor,
                            PrisonSupportedService prisonSupportedService,
                            NomisService nomisService,
                            DeallocateJob deallocateJob) {
        this.authenticationFacade = authenticationFacade;
        this.repository = repository;
        this.keyworkerRepository = keyworkerRepository;
        this.processor = processor;
        this.prisonSupportedService = prisonSupportedService;
        this.nomisService = nomisService;
        this.deallocateJob = deallocateJob;
    }

    public List<KeyworkerDto> getAvailableKeyworkers(String prisonId) {

        ResponseEntity<List<KeyworkerDto>> responseEntity = nomisService.getAvailableKeyworkers(prisonId);
        final List<KeyworkerDto> returnedList = responseEntity.getBody();

        final int prisonCapacityDefault = getPrisonCapacityDefault(prisonId);

        returnedList.forEach(keyworkerDto -> keyworkerDto.setAgencyId(prisonId));

        return returnedList.stream()
                .map(k -> decorateWithKeyworkerData(k, prisonCapacityDefault))
                .filter(k -> k.getStatus() != INACTIVE)
                .sorted(Comparator.comparing(KeyworkerDto::getNumberAllocated)
                                  .thenComparing(KeyworkerService::getKeyWorkerFullName))
                .collect(Collectors.toList());
    }

    private static String getKeyWorkerFullName(KeyworkerDto keyworkerDto) {
        return StringUtils.lowerCase(StringUtils.join(Arrays.asList(keyworkerDto.getLastName(), keyworkerDto.getFirstName()), " "));
    }

    public List<KeyworkerDto> getKeyworkersAvailableForAutoAllocation(String prisonId) {
        final List<KeyworkerDto> availableKeyworkers = getAvailableKeyworkers(prisonId);
        return availableKeyworkers.stream().filter(KeyworkerDto::getAutoAllocationAllowed).collect(Collectors.toList());
    }

    public Page<KeyworkerAllocationDetailsDto> getAllocations(AllocationsFilterDto allocationFilter, PagingAndSortingDto pagingAndSorting) {

        final String prisonId = allocationFilter.getPrisonId();
        final List<OffenderKeyworker> allocations =
                allocationFilter.getAllocationType().isPresent() ?
                        repository.findByActiveAndPrisonIdAndAllocationType(true, prisonId, allocationFilter.getAllocationType().get())
                        :
                        repository.findByActiveAndPrisonIdAndAllocationTypeIsNot(true, prisonId, AllocationType.PROVISIONAL);
        List<OffenderLocationDto> allOffenders = nomisService.getOffendersAtLocation(prisonId, pagingAndSorting.getSortFields(), pagingAndSorting.getSortOrder());

        final List<KeyworkerAllocationDetailsDto> results = processor.decorateAllocated(allocations, allOffenders);

        return new Page<>(results, (long) allocations.size(), 0L, (long) allocations.size());
    }


    public List<OffenderLocationDto> getUnallocatedOffenders(String prisonId, String sortFields, SortOrder sortOrder) {

        prisonSupportedService.verifyPrisonMigrated(prisonId);
        List<OffenderLocationDto> allOffenders = nomisService.getOffendersAtLocation(prisonId, sortFields, sortOrder);
        return processor.filterByUnallocated(allOffenders);
    }


    public List<OffenderKeyworkerDto> getOffenderKeyworkerDetailList(String prisonId, Collection<String> offenderNos) {
        final List<OffenderKeyworker> results =
                CollectionUtils.isEmpty(offenderNos)
                        ? repository.findByActiveAndPrisonIdAndAllocationTypeIsNot(true, prisonId, AllocationType.PROVISIONAL)
                        : repository.findByActiveAndPrisonIdAndOffenderNoInAndAllocationTypeIsNot(true, prisonId, offenderNos, AllocationType.PROVISIONAL);
        return ConversionHelper.convertOffenderKeyworkerModel2Dto(results);
    }

    public Optional<BasicKeyworkerDto> getCurrentKeyworkerForPrisoner(String prisonId, String offenderNo) {
         BasicKeyworkerDto currentKeyworker = null;
        if (prisonSupportedService.isMigrated(prisonId)) {
            OffenderKeyworker activeOffenderKeyworker = repository.findByOffenderNoAndActiveAndAllocationTypeIsNot(offenderNo, true, AllocationType.PROVISIONAL);
            if (activeOffenderKeyworker != null) {
                StaffLocationRoleDto staffDetail = nomisService.getBasicKeyworkerDtoForStaffId(activeOffenderKeyworker.getStaffId());
                if (staffDetail != null) {
                    currentKeyworker = BasicKeyworkerDto.builder()
                            .firstName(staffDetail.getFirstName())
                            .lastName(staffDetail.getLastName())
                            .staffId(staffDetail.getStaffId())
                            .email(staffDetail.getEmail())
                            .build();
                }
            }
        } else {
            currentKeyworker = nomisService.getBasicKeyworkerDtoForOffender(offenderNo);
        }
        return Optional.ofNullable(currentKeyworker);
    }

    public KeyworkerDto getKeyworkerDetails(String prisonId, Long staffId) {
        StaffLocationRoleDto staffKeyWorker = nomisService.getStaffKeyWorkerForPrison(prisonId, staffId).orElseGet(() -> nomisService.getBasicKeyworkerDtoForStaffId(staffId));
        final int prisonCapacityDefault = getPrisonCapacityDefault(prisonId);
        return decorateWithKeyworkerData(ConversionHelper.getKeyworkerDto(staffKeyWorker), prisonCapacityDefault);
    }

    @PreAuthorize("hasAnyRole('KW_ADMIN', 'OMIC_ADMIN')")
    public void allocate(@Valid @NotNull KeyworkerAllocationDto keyworkerAllocation) {
        prisonSupportedService.verifyPrisonMigrated(keyworkerAllocation.getPrisonId());
        doAllocateValidation(keyworkerAllocation);
        doAllocate(keyworkerAllocation);
    }

    private void doAllocateValidation(KeyworkerAllocationDto keyworkerAllocation) {
        Validate.notBlank(keyworkerAllocation.getOffenderNo(), "Missing prisoner number.");
        Validate.notNull(keyworkerAllocation.getStaffId(), "Missing staff id.");

        Optional<OffenderLocationDto> offender = nomisService.getOffenderForPrison(keyworkerAllocation.getPrisonId(), keyworkerAllocation.getOffenderNo());

        Validate.isTrue(offender.isPresent(), format("Prisoner %s not found at agencyId %s",
                keyworkerAllocation.getOffenderNo(), keyworkerAllocation.getPrisonId()));

        KeyworkerDto keyworkerDetails = getKeyworkerDetails(keyworkerAllocation.getPrisonId(), keyworkerAllocation.getStaffId());
        Validate.notNull(keyworkerDetails, format("Keyworker %d not found at agencyId %s.",
                keyworkerAllocation.getStaffId(), keyworkerAllocation.getPrisonId()));
    }

    private void doAllocate(KeyworkerAllocationDto newAllocation) {

        // Remove current allocation if any
        final List<OffenderKeyworker> entities = repository.findByActiveAndOffenderNo(
                true, newAllocation.getOffenderNo());
        final LocalDateTime now = LocalDateTime.now();
        entities.forEach(e -> {
            e.setActive(false);
            e.setExpiryDateTime(now);
            e.setDeallocationReason(newAllocation.getDeallocationReason());
        });

        OffenderKeyworker allocation = ConversionHelper.getOffenderKeyworker(newAllocation, authenticationFacade.getCurrentUsername());

        allocate(allocation);
    }

    /**
     * Creates a new offender - Key worker allocation record.
     *
     * @param allocation allocation details.
     */
    @PreAuthorize("hasAnyRole('KW_ADMIN', 'OMIC_ADMIN')")
    public void allocate(OffenderKeyworker allocation) {
        Validate.notNull(allocation);

        // This service method creates a new allocation record, therefore it will apply certain defaults automatically.
        LocalDateTime now = LocalDateTime.now();

        allocation.setActive(true);
        allocation.setAssignedDateTime(now);

        if (StringUtils.isBlank(allocation.getUserId())) {
            allocation.setUserId(authenticationFacade.getCurrentUsername());
        }

        repository.save(allocation);
    }


    public List<OffenderKeyworker> getAllocationHistoryForPrisoner(String offenderNo) {
        return repository.findByOffenderNo(offenderNo);
    }

    public Optional<OffenderKeyWorkerHistory> getFullAllocationHistory(String offenderNo) {
        final List<OffenderKeyworker> keyworkers = repository.findByOffenderNo(offenderNo);
        OffenderKeyWorkerHistory offenderKeyWorkerHistory = null;

        if (!keyworkers.isEmpty()) {
            List<KeyWorkerAllocation> keyWorkerAllocations = keyworkers.stream().map(
                    kw -> {
                        StaffLocationRoleDto staffKw = nomisService.getBasicKeyworkerDtoForStaffId(kw.getStaffId());

                        return KeyWorkerAllocation.builder()
                                .offenderKeyworkerId(kw.getOffenderKeyworkerId())
                                .firstName(staffKw.getFirstName())
                                .lastName(staffKw.getLastName())
                                .staffId(kw.getStaffId())
                                .active(kw.isActive() ? "Yes" : "No")
                                .allocationReason(kw.getAllocationReason())
                                .allocationType(kw.getAllocationType())
                                .assigned(kw.getAssignedDateTime())
                                .expired(kw.getExpiryDateTime())
                                .deallocationReason(kw.getDeallocationReason())
                                .prisonId(kw.getPrisonId())
                                .userId(nomisService.getStaffDetailByUserId(kw.getUserId()))
                                .createdByUser(nomisService.getStaffDetailByUserId(kw.getCreateUserId()))
                                .creationDateTime(kw.getCreationDateTime())
                                .lastModifiedByUser(nomisService.getStaffDetailByUserId(kw.getModifyUserId()))
                                .modifyDateTime(kw.getModifyDateTime())
                                .build();
                    }

            ).sorted(Comparator
                    .comparing(KeyWorkerAllocation::getAssigned).reversed())
                    .collect(Collectors.toList());

            // use prison for most recent allocation
            OffenderLocationDto offenderDetail = nomisService.getOffenderForPrison(keyWorkerAllocations.get(0).getPrisonId(), offenderNo)
                    .orElseGet(OffenderLocationDto::new);

            offenderKeyWorkerHistory = OffenderKeyWorkerHistory.builder()
                    .offender(offenderDetail)
                    .allocationHistory(keyWorkerAllocations)
                    .build();

        }
        return Optional.ofNullable(offenderKeyWorkerHistory);
    }

    public List<OffenderKeyworker> getAllocationsForKeyworker(Long staffId) {
        return repository.findByStaffId(staffId);
    }

    public List<KeyworkerAllocationDetailsDto> getAllocationsForKeyworkerWithOffenderDetails(String prisonId, Long staffId, boolean skipOffenderDetails) {

        prisonSupportedService.verifyPrisonMigrated(prisonId);

        final List<OffenderKeyworker> allocations = repository.findByStaffIdAndPrisonIdAndActiveAndAllocationTypeIsNot(staffId, prisonId, true, AllocationType.PROVISIONAL);

        final List<KeyworkerAllocationDetailsDto> detailsDtoList;
        if (skipOffenderDetails) {
            detailsDtoList = allocations.stream()
                    .map(allocation ->  KeyworkerAllocationDetailsDto.builder()
                            .offenderNo(allocation.getOffenderNo())
                            .staffId(allocation.getStaffId())
                            .agencyId(allocation.getPrisonId()) //TODO: remove
                            .prisonId(allocation.getPrisonId())
                            .assigned(allocation.getAssignedDateTime())
                            .allocationType(allocation.getAllocationType())
                            .build())
                    .collect(Collectors.toList());
        } else {
            detailsDtoList = allocations.stream()
                    .map(allocation -> decorateWithOffenderDetails(prisonId, allocation))
                    //remove allocations from returned list that do not have associated booking records
                    .filter(dto -> dto.getBookingId() != null)
                    .sorted(Comparator
                            .comparing(KeyworkerAllocationDetailsDto::getLastName)
                            .thenComparing(KeyworkerAllocationDetailsDto::getFirstName))
                    .collect(Collectors.toList());
        }

        log.debug("Retrieved allocations for keyworker {}:\n{}", staffId, detailsDtoList);

        return detailsDtoList;
    }

    private KeyworkerAllocationDetailsDto decorateWithOffenderDetails(String prisonId, OffenderKeyworker allocation) {
        KeyworkerAllocationDetailsDto dto;

        Optional<OffenderLocationDto> offender = nomisService.getOffenderForPrison(prisonId, allocation.getOffenderNo());

        if (offender.isPresent()) {
            final OffenderLocationDto offenderSummaryDto = offender.get();
            dto = KeyworkerAllocationDetailsDto.builder()
                    .bookingId(offenderSummaryDto.getBookingId())
                    .offenderNo(allocation.getOffenderNo())
                    .firstName(offenderSummaryDto.getFirstName())
                    .middleNames(offenderSummaryDto.getMiddleName())
                    .lastName(offenderSummaryDto.getLastName())
                    .staffId(allocation.getStaffId())
                    .agencyId(allocation.getPrisonId()) //TODO: remove
                    .prisonId(allocation.getPrisonId())
                    .assigned(allocation.getAssignedDateTime())
                    .allocationType(allocation.getAllocationType())
                    .internalLocationDesc(offenderSummaryDto.getAssignedLivingUnitDesc())
                    .build();
        } else {
            log.error(format("Allocation does not have associated booking, removing from keyworker allocation list:\noffender %s in agency %s not found using nomis service", allocation.getOffenderNo(), prisonId));
            dto =  KeyworkerAllocationDetailsDto.builder().build();
        }
        return dto;
    }


    public Page<KeyworkerDto> getKeyworkers(String prisonId, Optional<String> nameFilter, PagingAndSortingDto pagingAndSorting) {

        ResponseEntity<List<StaffLocationRoleDto>> response = nomisService.getActiveStaffKeyWorkersForPrison(prisonId, nameFilter, pagingAndSorting);
        final int prisonCapacityDefault = getPrisonCapacityDefault(prisonId);

        final List<KeyworkerDto> convertedKeyworkerDtoList = response.getBody().stream().distinct()
                .map(dto -> decorateWithKeyworkerData(ConversionHelper.getKeyworkerDto(dto), prisonCapacityDefault))
                .collect(Collectors.toList());
        return new Page<>(convertedKeyworkerDtoList, response.getHeaders());
    }

    private int getPrisonCapacityDefault(String prisonId) {
        Prison prisonDetail = prisonSupportedService.getPrisonDetail(prisonId);
        return prisonDetail != null ? prisonDetail.getCapacityTier1() : 0;
    }

    private KeyworkerDto decorateWithKeyworkerData(KeyworkerDto keyworkerDto, int capacityDefault) {
        if (keyworkerDto != null && keyworkerDto.getAgencyId() != null) {
            final Keyworker keyworker = keyworkerRepository.findOne(keyworkerDto.getStaffId());
            final Integer allocationsCount = repository.countByStaffIdAndPrisonIdAndActiveAndAllocationTypeIsNot(keyworkerDto.getStaffId(), keyworkerDto.getAgencyId(), true, AllocationType.PROVISIONAL);

            keyworkerDto.setCapacity((keyworker != null && keyworker.getCapacity() != null) ? keyworker.getCapacity() : capacityDefault);
            keyworkerDto.setStatus(keyworker != null ? keyworker.getStatus() : KeyworkerStatus.ACTIVE);
            keyworkerDto.setNumberAllocated(allocationsCount);
            keyworkerDto.setAgencyId(keyworkerDto.getAgencyId());
            keyworkerDto.setAutoAllocationAllowed(keyworker != null ? keyworker.getAutoAllocationFlag() : true);
        }

        return keyworkerDto;
    }

    @PreAuthorize("hasRole('KW_MIGRATION')")
    public void runDeallocateBatchProcess(LocalDateTime checkFromDateTime) {
        deallocateJob.checkMovements(checkFromDateTime);
    }

    @PreAuthorize("hasAnyRole('KW_ADMIN', 'OMIC_ADMIN')")
    public void addOrUpdate(Long staffId, String prisonId, KeyworkerUpdateDto keyworkerUpdateDto) {

        Validate.notNull(staffId, "Missing staff id");
        Keyworker keyworker = keyworkerRepository.findOne(staffId);

        if (keyworker == null) {

            keyworkerRepository.save(Keyworker.builder()
                    .staffId(staffId)
                    .capacity(keyworkerUpdateDto.getCapacity())
                    .status(keyworkerUpdateDto.getStatus())
                    .autoAllocationFlag(true)
                    .build());

        } else {
            keyworker.setCapacity(keyworkerUpdateDto.getCapacity());
            keyworker.setStatus(keyworkerUpdateDto.getStatus());
            if (keyworkerUpdateDto.getStatus() == KeyworkerStatus.ACTIVE){
                keyworker.setAutoAllocationFlag(true);
            }
        }

        final KeyworkerStatusBehaviour behaviour = keyworkerUpdateDto.getBehaviour();
        if (behaviour != null) applyStatusChangeBehaviour(staffId, prisonId, behaviour);
    }

    private void applyStatusChangeBehaviour(Long staffId, String prisonId, KeyworkerStatusBehaviour behaviour) {

        if (behaviour.isRemoveAllocations()) {
            final LocalDateTime now = LocalDateTime.now();
            final List<OffenderKeyworker> allocations = repository.findByStaffIdAndPrisonIdAndActive(staffId, prisonId, true);
            allocations.forEach(ok -> {
                ok.setDeallocationReason(DeallocationReason.KEYWORKER_STATUS_CHANGE);
                ok.setActive(false);
                ok.setExpiryDateTime(now);
            });
        }

        if (behaviour.isRemoveFromAutoAllocation()) {
            keyworkerRepository.findOne(staffId).setAutoAllocationFlag(false);
        }
    }

    @PreAuthorize("hasAnyRole('KW_ADMIN', 'OMIC_ADMIN')")
    public void deallocate(String offenderNo) {
        final List<OffenderKeyworker> offenderKeyworkers = repository.findByActiveAndOffenderNo(true, offenderNo);

        if (offenderKeyworkers.isEmpty()) {
            throw new EntityNotFoundException(String.format("Offender No %s not allocated or does not exist", offenderNo));
        }

        // There shouldnt ever be more than 1, but just in case
        final LocalDateTime now = LocalDateTime.now();
        offenderKeyworkers.forEach(offenderKeyworker -> {
            offenderKeyworker.deallocate(now, DeallocationReason.MANUAL);
            log.info("De-allocated offender {} from KW {} at {}", offenderNo, offenderKeyworker.getStaffId(), offenderKeyworker.getPrisonId());
        });
    }
}
