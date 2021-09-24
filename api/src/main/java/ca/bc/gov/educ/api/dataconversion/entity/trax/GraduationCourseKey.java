package ca.bc.gov.educ.api.dataconversion.entity.trax;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
@Data
public class GraduationCourseKey implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "CRSE_CODE", insertable = false, updatable = false, length = 5)
    private String courseCode;

    @Column(name = "CRSE_LEVEL", insertable = false, updatable = false, length = 3)
    private String courseLevel;

    @Column(name = "GRAD_REQT_YEAR", insertable = false, updatable = false, length = 4)
    private String gradReqtYear;


    public GraduationCourseKey() {
    }

    public GraduationCourseKey(String courseCode, String courseLevel, String gradReqtYear) {
        this.courseCode = courseCode;
        this.courseLevel = courseLevel;
        this.gradReqtYear = gradReqtYear;
    }
}
