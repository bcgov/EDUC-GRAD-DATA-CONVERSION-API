package ca.bc.gov.educ.api.dataconversion.entity.course;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

/**
 * Embeddable JPA Entity composite primary key consisting of student number,
 * course code, course level and course session. This class is used as an
 * embedded primary key for the entities which do not have a unique primary key
 * defined by the database view they mirror. The Entities must have these named
 * attributes or have mapped attributes which override these names in order to
 * use this class.
 *
 * @author CGI Information Management Consultants Inc.
 */
@Embeddable
@Data
public class StudentCourseId implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "PEN", insertable = false, updatable = false)
    private String pen;
    @Column(name = "CRSE_CODE", insertable = false, updatable = false)
    private String courseCode;
    @Column(name = "CRSE_LEVEL", insertable = false, updatable = false)
    private String courseLevel;
    @Column(name = "CRSE_SESSION", insertable = false, updatable = false)
    private String sessionDate;
    @Column(name = "PROV_EXAM_CRSE", insertable = false, updatable = false)
    private String provExamCourse;

    public StudentCourseId() {
    }

    /**
     * Constructor method used by JPA to create a composite primary key.
     *
     * @param studNo
     * @param crseCode
     * @param crseLevel
     * @param crseSession
     * @param provExamCourse
     */
    public StudentCourseId(String studNo, String crseCode, String crseLevel, String crseSession,String provExamCourse) {
        this.pen = studNo;
        this.courseCode = crseCode;
        this.courseLevel = crseLevel;
        this.sessionDate = crseSession;
        this.provExamCourse = provExamCourse;
    }
}
