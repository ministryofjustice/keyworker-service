package uk.gov.justice.digital.hmpps.keyworker.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import uk.gov.justice.digital.hmpps.keyworker.dto.*;
import uk.gov.justice.digital.hmpps.keyworker.model.CreateUpdate;
import uk.gov.justice.digital.hmpps.keyworker.model.OffenderKeyworker;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Helper to convert model objects to DTOs and vice versa.
 */
public final class ConversionHelper {
    private ConversionHelper() {
    }

    public static List<OffenderKeyworker> convertOffenderKeyworkerDto2Model(List<OffenderKeyworkerDto> dtos) {
        Validate.notNull(dtos);

        return dtos.stream().map(ConversionHelper::convertOffenderKeyworkerDto2Model).collect(Collectors.toList());
    }

    public static OffenderKeyworker convertOffenderKeyworkerDto2Model(OffenderKeyworkerDto dto) {
        Validate.notNull(dto);

        CreateUpdate createUpdate = CreateUpdate.builder()
                .creationDateTime(dto.getCreated())
                .createUserId(dto.getCreatedBy())
                .modifyDateTime(dto.getModified())
                .modifyUserId(dto.getModifiedBy())
                .build();

        return OffenderKeyworker.builder()
                .offenderNo(dto.getOffenderNo())
                .staffId(dto.getStaffId())
                .agencyId(dto.getAgencyId())
                .active(StringUtils.equals("Y", dto.getActive()))
                .assignedDateTime(dto.getAssigned())
                .expiryDateTime(dto.getExpired())
                .userId(dto.getUserId())
                .createUpdate(createUpdate)
                .build();
    }

    public static List<OffenderKeyworkerDto> convertOffenderKeyworkerModel2Dto(List<OffenderKeyworker> models) {
        Validate.notNull(models);

        return models.stream().map(ConversionHelper::convertOffenderKeyworkerModel2Dto).collect(Collectors.toList());
    }

    public static OffenderKeyworkerDto convertOffenderKeyworkerModel2Dto(OffenderKeyworker model) {
        Validate.notNull(model);

        return OffenderKeyworkerDto.builder()
                .offenderKeyworkerId(model.getOffenderKeyworkerId())
                .offenderNo(model.getOffenderNo())
                .staffId(model.getStaffId())
                .agencyId(model.getAgencyId())
                .active(model.isActive() ? "Y" : "N")
                .assigned(model.getAssignedDateTime())
                .expired(model.getExpiryDateTime())
                .userId(model.getUserId())
                .build();
    }


    public static OffenderKeyworker getOffenderKeyworker(KeyworkerAllocationDto newAllocation, String userId) {
        return OffenderKeyworker.builder()
                .offenderNo(newAllocation.getOffenderNo())
                .staffId(newAllocation.getStaffId())
                .agencyId(newAllocation.getAgencyId())
                .allocationReason(newAllocation.getAllocationReason())
                .active(true)
                .assignedDateTime(LocalDateTime.now())
                .allocationType(newAllocation.getAllocationType())
                .userId(userId)
                .build();
    }

    public static List<KeyworkerAllocationDetailsDto> convertOffenderKeyworkerModel2KeyworkerAllocationDetailsDto(List<OffenderKeyworker> models) {
        Validate.notNull(models);

        return models.stream().map(ConversionHelper::convertOffenderKeyworkerModel2KeyworkerAllocationDetailsDto).collect(Collectors.toList());
    }

    public static KeyworkerAllocationDetailsDto convertOffenderKeyworkerModel2KeyworkerAllocationDetailsDto(OffenderKeyworker model) {
        Validate.notNull(model);

        return KeyworkerAllocationDetailsDto.builder()
                .offenderNo(model.getOffenderNo())
                .staffId(model.getStaffId())
                .agencyId(model.getAgencyId())
                .assigned(model.getAssignedDateTime())
                .allocationType(model.getAllocationType())
                .build();
    }

    public static KeyworkerDto getKeyworkerDto(StaffLocationRoleDto dto) {
        return KeyworkerDto.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .staffId(dto.getStaffId())
                .thumbnailId(dto.getThumbnailId())
                .scheduleType(dto.getScheduleTypeDescription())
                .agencyDescription(dto.getAgencyDescription())
                .agencyId(dto.getAgencyId())
                .build();
    }
}
