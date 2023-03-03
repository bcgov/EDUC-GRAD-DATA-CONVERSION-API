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
    private String mincode;
    private String birthDate;  // yyyymmdd
    private String gradReqtYear; // yyyy
    private String studentGrade;
    private Long gradDate; // yyyymm
    private String gradMessage;
    private Long updateDate; // yyyymmdd
}
