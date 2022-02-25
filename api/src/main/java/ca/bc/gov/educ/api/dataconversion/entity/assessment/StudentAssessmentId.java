package ca.bc.gov.educ.api.dataconversion.entity.assessment;

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
public class StudentAssessmentId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "PEN", insertable = false, updatable = false)
    private String pen;
    
    @Column(name = "ASSM_CODE", nullable = true)
    private String assessmentCode;
    
    @Column(name = "ASSM_SESSION", nullable = true)
    private String sessionDate;

    public StudentAssessmentId() {
    }

    /**
     * Constructor method used by JPA to create a composite primary key.
     *
     * @param studNo
     * @param assessmentCode
     * @param assmSession
     */
    public StudentAssessmentId(String studNo, String assessmentCode, String assmSession) {
        this.pen = studNo;
        this.assessmentCode = assessmentCode;
        this.sessionDate = assmSession;
    }
}
