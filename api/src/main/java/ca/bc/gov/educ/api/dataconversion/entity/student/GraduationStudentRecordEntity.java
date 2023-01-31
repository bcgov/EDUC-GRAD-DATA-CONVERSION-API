package ca.bc.gov.educ.api.dataconversion.entity.student;

import java.util.Date;
import java.util.UUID;

import javax.persistence.*;

import ca.bc.gov.educ.api.dataconversion.entity.BaseEntity;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;

import lombok.Data;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "GRADUATION_STUDENT_RECORD")
public class GraduationStudentRecordEntity extends BaseEntity {

    @Transient
    private String pen;

    @Lob
    @Column(name = "STUDENT_GRAD_DATA", columnDefinition="CLOB")
    private String studentGradData;

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
    
    @Column(name = "SCHOOL_OF_RECORD", nullable = true)
    private String schoolOfRecord;
    
    @Column(name = "STUDENT_GRADE", nullable = true)
    private String studentGrade;
    
    @Column(name = "STUDENT_STATUS_CODE", nullable = false)
    private String studentStatus;

    @Id
    @Column(name = "GRADUATION_STUDENT_RECORD_ID", nullable = false)
    private UUID studentID;

    @Column(name = "SCHOOL_AT_GRADUATION", nullable = true)
    private String schoolAtGrad;

    @Column(name = "RECALCULATE_PROJECTED_GRAD", nullable = true)
    private String recalculateProjectedGrad;

    @Column(name = "BATCH_ID", nullable = true)
    private Long batchId;

    @Column(name = "CONSUMER_EDUC_REQT_MET", nullable = true)
    private String consumerEducationRequirementMet;

    @Column(name = "STUDENT_CITIZENSHIP_CODE", nullable = true)
    private String studentCitizenship;

    @Column(name = "ADULT_START_DATE", nullable = true)
    private Date adultStartDate;

    // Mappings for Student_Master
    @Transient
    private String frenchCert;
    @Transient
    private String englishCert;
    @Transient
    private boolean dualDogwood = false;
}