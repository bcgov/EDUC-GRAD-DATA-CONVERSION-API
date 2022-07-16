package ca.bc.gov.educ.api.dataconversion.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The type TSW Transcript Demographics entity.
 */
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TranscriptStudentDemog {
    private String studNo;
    private String logoType;
    private String archiveFlag;
    // School info
    private String mincode;
    private String schoolName;
    private String address1;
    private String city;
    private String provCode;
    private String postal;
    private String localId;
    private String earlyAdmission;

    // Student Demographics
    private String birthDate;  // yyyymmdd
    private String gradReqtYear; // yyyy
    private String studentGrade;
    private String studCitiz;
    private String studGender;
    private String programCode;
    private String lastName;
    private String firstName;
    private String middleName;
    private Long gradDate; // yyyymm
    private String gradFlag;
    private String totalCredits;
    private String gradMessage;
    private String gradTextMessage;
    private String currentFormerFlag;
    private Long updateDate; // yyyymmdd
}
