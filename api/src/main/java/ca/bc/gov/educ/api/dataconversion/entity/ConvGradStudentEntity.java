package ca.bc.gov.educ.api.dataconversion.entity;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

@Data
@Entity
@Table(name = "CONV_GRAD_STUDENT")
public class ConvGradStudentEntity extends BaseEntity {

    @Id
    @Column(name = "STUDENT_ID", nullable = false)
    private UUID studentID;

    @Column(name = "pen", nullable = false)
    private String pen;

    @Column(name = "FK_GRAD_PROGRAM_CODE", nullable = true)
    private String program;

    @Column(name = "PROGRAM_COMPLETION_DT", nullable = true)
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

    @Column(name = "SCHOOL_AT_GRAD", nullable = true)
    private String schoolAtGrad;

    @Column(name = "STUD_GRADE", nullable = true)
    private String studentGrade;

    @Column(name = "FK_GRAD_STUDENT_STUDENT_STATUS", nullable = false)
    private String studentStatus;
    
}