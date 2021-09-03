package ca.bc.gov.educ.api.dataconversion.entity.conv;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
@Data
public class GraduationCourseKey implements Serializable {
    private static final long serialVersionUID = 1L;

//    @Size(max = 5)
    @Column(name = "CRSE_CODE", insertable = false, updatable = false, length = 5)
    private String courseCode;

//    @Size(max = 3)
    @Column(name = "CRSE_LEVEL", insertable = false, updatable = false, length = 3)
    private String courseLevel;

//    @Size(max = 4)
    @Column(name = "GRAD_REQT_YEAR", insertable = false, updatable = false, length = 4)
    private String gradReqtYear;

//    @Size(max = 6)
//    @Column(name = "START_SESSION", insertable = false, updatable = false, length = 6)
//    private String startSession;

    public GraduationCourseKey() {
    }

    public GraduationCourseKey(String courseCode, String courseLevel, String gradReqtYear) {
        this.courseCode = courseCode;
        this.courseLevel = courseLevel;
        this.gradReqtYear = gradReqtYear;
    }
}
