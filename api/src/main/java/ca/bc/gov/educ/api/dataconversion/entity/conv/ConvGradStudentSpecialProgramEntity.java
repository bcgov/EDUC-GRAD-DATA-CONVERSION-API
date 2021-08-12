package ca.bc.gov.educ.api.dataconversion.entity.conv;

import java.sql.Date;
import java.util.UUID;

import javax.persistence.*;

import ca.bc.gov.educ.api.dataconversion.entity.BaseEntity;
import org.hibernate.annotations.GenericGenerator;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "CONV_STUDENT_OPTIONAL_PROGRAM")
public class ConvGradStudentSpecialProgramEntity extends BaseEntity {

	@Id
	@GeneratedValue(generator = "UUID")
	@GenericGenerator(
		name = "UUID",
		strategy = "org.hibernate.id.UUIDGenerator"
	)
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