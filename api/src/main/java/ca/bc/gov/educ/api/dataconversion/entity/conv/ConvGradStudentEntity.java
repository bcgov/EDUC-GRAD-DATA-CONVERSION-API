package ca.bc.gov.educ.api.dataconversion.entity.conv;

import ca.bc.gov.educ.api.dataconversion.entity.BaseEntity;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

@Data
@Entity
@Table(name = "CONV_GRADUATION_STUDENT_RECORD")
public class ConvGradStudentEntity extends BaseEntity {

    @Id
    @Column(name = "GRADUATION_STUDENT_RECORD_ID", nullable = false)
    private UUID studentID;

    @Transient
    private String pen;

    @Column(name = "GRADUATION_PROGRAM_CODE", nullable = true)
    private String program;

    @Column(name = "PROGRAM_COMPLETION_DATE", nullable = true)
    private Date programCompletionDate;

    @Column(name = "GPA", nullable = true)
    private String gpa;

    @Column(name = "HONOURS_STANDING", nullable = true)
    private String honoursStanding;

    @Column(name = "RECALCULATE_GRAD_STATUS", nullable = true)
    private String recalculateGradStatus;

    @Lob
    @Column(name = "STUDENT_GRAD_DATA", columnDefinition="CLOB")
    private String studentGradData;

    @Column(name = "SCHOOL_OF_RECORD", nullable = true)
    private String schoolOfRecord;

    @Column(name = "SCHOOL_AT_GRADUATION", nullable = true)
    private String schoolAtGrad;

    @Column(name = "STUDENT_GRADE", nullable = true)
    private String studentGrade;

    @Column(name = "STUDENT_STATUS_CODE", nullable = false)
    private String studentStatus;
    
}