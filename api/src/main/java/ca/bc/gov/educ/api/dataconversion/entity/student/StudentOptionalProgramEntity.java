package ca.bc.gov.educ.api.dataconversion.entity.student;

import ca.bc.gov.educ.api.dataconversion.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.sql.Date;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "STUDENT_OPTIONAL_PROGRAM")
public class StudentOptionalProgramEntity extends BaseEntity {

	@Id
	@Column(name = "STUDENT_OPTIONAL_PROGRAM_ID", nullable = false)
    private UUID id;

    @Column(name = "OPTIONAL_PROGRAM_ID", nullable = false)
    private UUID optionalProgramID;

    @Lob
    @Column(name = "PROGRAM_NOTE", columnDefinition="CLOB")
    private String studentSpecialProgramData;

    @Column(name = "COMPLETION_DATE", nullable = true)
    private Date specialProgramCompletionDate;  
    
    @Column(name = "GRADUATION_STUDENT_RECORD_ID", nullable = false)
    private UUID studentID;

    @Transient
    private String pen;

}