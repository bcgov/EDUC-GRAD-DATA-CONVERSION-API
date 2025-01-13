package ca.bc.gov.educ.api.dataconversion.model.institute;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.stereotype.Component;

@Data
@EqualsAndHashCode
@Component("instituteSchool")
@JsonIgnoreProperties(ignoreUnknown = true)
public class School {

    private String schoolId;
    private String districtId;
    private String mincode;
    private String independentAuthorityId;
    private String schoolNumber;
    private String faxNumber;
    private String phoneNumber;
    private String email;
    private String website;
    private String displayName;
    private String displayNameNoSpecialChars;
    private String schoolReportingRequirementCode;
    private String schoolOrganizationCode;
    private String schoolCategoryCode;
    private String facilityTypeCode;
    private String openedDate;
    private String closedDate;
    private boolean canIssueTranscripts;
    private boolean canIssueCertificates;

}
