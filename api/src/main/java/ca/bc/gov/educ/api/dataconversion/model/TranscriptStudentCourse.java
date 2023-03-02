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

    public String getReportType() {
        return reportType!=null?reportType.trim():null;
    }

    public String getFoundationReq() {
        return foundationReq!=null?foundationReq.trim():null;
    }

    public String getSpecialCase() {
        return specialCase!=null?specialCase.trim():null;
    }

    public String getSchoolPercentage() {
        return schoolPercentage!=null?schoolPercentage.trim():null;
    }

    public String getFinalPercentage() {
        return finalPercentage!=null?finalPercentage.trim():null;
    }

    public String getFinalLG() {
        return finalLG!=null?finalLG.trim():null;
    }

    public String getInterimMark() {
        return interimMark!=null?interimMark.trim():null;
    }

    public String getNumberOfCredits() {
        return numberOfCredits!=null?numberOfCredits.trim():null;
    }

    public String getCourseType() {
        return courseType!=null?courseType.trim():null;
    }

    public String getUsedForGrad() {
        return usedForGrad!=null?usedForGrad.trim():null;
    }

    public String getMetLitNumReqt() {
        return metLitNumReqt!=null?metLitNumReqt.trim():null;
    }

    public String getRelatedCourseLevel() {
        return relatedCourseLevel!=null?relatedCourseLevel.trim():null;
    }

    public String getStudNo() {
        return studNo != null ? studNo.trim():null;
    }

    public String getCourseCode() {
        return courseCode != null ? courseCode.trim(): null;
    }
    public String getCourseName() {
        return courseName != null ? courseName.trim(): null;
    }

    public String getCourseLevel() {
        return courseLevel != null ? courseLevel.trim(): null;
    }
}
