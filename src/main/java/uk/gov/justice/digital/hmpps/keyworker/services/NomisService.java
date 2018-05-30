package uk.gov.justice.digital.hmpps.keyworker.services;

import org.springframework.http.ResponseEntity;
import uk.gov.justice.digital.hmpps.keyworker.dto.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NomisService {
    String URI_ACTIVE_OFFENDERS_BY_AGENCY = "/bookings?query=agencyId:eq:'{prisonId}'";
    String URI_ACTIVE_OFFENDER_BY_AGENCY = URI_ACTIVE_OFFENDERS_BY_AGENCY + "&offenderNo={offenderNo}&iepLevel=true";
    String URI_CUSTODY_STATUSES = "/custody-statuses?fromDateTime={fromDateTime}&movementDate={movementDate}";
    String URI_STAFF = "/staff/{staffId}";
    String URI_AVAILABLE_KEYWORKERS = "/key-worker/{agencyId}/available";
    String URI_KEY_WORKER_GET_ALLOCATION_HISTORY = "/key-worker/{agencyId}/allocationHistory";
    String GET_STAFF_IN_SPECIFIC_PRISON = "/staff/roles/{agencyId}/role/KW";

    List<PrisonerCustodyStatusDto> getPrisonerStatuses(LocalDateTime threshold, LocalDate movementDate);

    Optional<OffenderLocationDto> getOffenderForPrison(String prisonId, String offenderNo);

    ResponseEntity<List<StaffLocationRoleDto>> getActiveStaffKeyWorkersForPrison(String prisonId, Optional<String> nameFilter, PagingAndSortingDto pagingAndSorting);

    Optional<StaffLocationRoleDto> getStaffKeyWorkerForPrison(String prisonId, Long staffId);

    BasicKeyworkerDto getBasicKeyworkerDtoForOffender(String offenderNo);

    ResponseEntity<List<KeyworkerDto>> getAvailableKeyworkers(String prisonId);

    List<OffenderLocationDto> getOffendersAtLocation(String prisonId, String sortFields, SortOrder sortOrder);

    StaffLocationRoleDto getBasicKeyworkerDtoForStaffId(Long staffId);

    List<OffenderKeyworkerDto> getOffenderKeyWorkerPage(String prisonId, long offset, long limit);

    StaffUser getStaffDetailByUserId(String userId);
}
