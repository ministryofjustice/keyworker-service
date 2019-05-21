package uk.gov.justice.digital.hmpps.keyworker.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.MultiValueMap;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@ApiModel(description = "A specification of how to select a subset of users by caseload and role membership.  Selected users will be assigned 'rolesToAssign' in the NWEB caseload.  The 'rolesToRemove' will then be removed from each selected user at each of the specified caseloads.")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleAssignmentsSpecification {
    @ApiModelProperty(required = true, value = "The caseloads to search for users having roles matching 'rolesToMatch'.", position = 0)
    @NotEmpty(message = "Expected at least one 'caseload'")
    private List<String> caseloads;

    @ApiModelProperty(required = true, value = "users within the caseloads will be selected if they have at least one role matching the codes in rolesToMatch.", position = 1)
    @NotEmpty(message = "Expected at least one 'rolesToMatch'")
    private List<String> rolesToMatch;

    @ApiModelProperty(value = "Users with the named caseloads, having roles matching rolesToMatch will be assigned these roles in the 'NWEB' caseload.", position = 2)
    @NotNull
    private List<String> rolesToAssign;

    @ApiModelProperty(value = "For each caseload in caseloads; find the users having at least one role matching 'rolesToMatch'. For each matched user at the current caseload remove each of the roles in 'rolesToRemove' at that caseload.", position = 3)
    @NotNull
    private List<String> rolesToRemove;


    public static RoleAssignmentsSpecification fromForm(MultiValueMap<String, String> form) {
        return RoleAssignmentsSpecification.builder()
                .caseloads(fromFormField("caseloads", form))
                .rolesToMatch(fromFormField("rolesToMatch", form))
                .rolesToAssign(fromFormField("rolesToAssign", form))
                .rolesToRemove(fromFormField("rolesToRemove", form))
                .build();
    }

    private static List<String> fromFormField(String key, MultiValueMap<String, String> form) {
        return form.getOrDefault(key, Collections.emptyList())
                .stream()
                .filter(value -> !value.isBlank())
                .collect(Collectors.toList());
    }
}
