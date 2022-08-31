package ca.bc.gov.educ.api.dataconversion.entity.student;

import ca.bc.gov.educ.api.dataconversion.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.sql.Date;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "STUDENT_OPTIONAL_PROGRAM_HISTORY")
public class StudentOptionalProgramHistoryEntity extends BaseEntity {

	@Id
	@GeneratedValue(generator = "UUID")
	@GenericGenerator(
		name = "UUID",
		strategy = "org.hibernate.id.UUIDGenerator"
	)
    @Column(name = "STUDENT_OPTIONAL_PROGRAM_HISTORY_ID", nullable = false)
    private UUID historyId;

    @Column(name = "HISTORY_ACTIVITY_CODE", nullable = true)
    private String activityCode;

    @Column(name = "STUDENT_OPTIONAL_PROGRAM_ID", nullable = false)
    private UUID studentOptionalProgramID;

    @Column(name = "OPTIONAL_PROGRAM_ID", nullable = false)
    private UUID optionalProgramID;
    
//    @Lob
//    @Column(name = "PROGRAM_NOTE", columnDefinition="CLOB")
    @Transient
    private String studentSpecialProgramData;

    @Column(name = "COMPLETION_DATE", nullable = true)
    private Date specialProgramCompletionDate;  
    
    @Column(name = "GRADUATION_STUDENT_RECORD_ID", nullable = false)
    private UUID studentID;

}