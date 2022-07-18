package ca.bc.gov.educ.api.dataconversion.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The type TSW Transcript Course.
 */
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TranscriptStudentCourse {
    private String studNo;
    private String courseCode;
    private String courseLevel;

    private String reportType;
    private String courseName;
    private String foundationReq;
    private String specialCase;
    private String courseSession;
    private String schoolPercentage;
    private String examPercentage;
    private String finalPercentage;
    private String finalLG;
    private String interimMark;
    private String numberOfCredits;
    private String courseType;
    private String usedForGrad;
    private String metLitNumReqt;
    private String relatedCourse;
    private String relatedCourseLevel;
    private Long updateDate; // yyyymmdd
}
