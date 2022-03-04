package ca.bc.gov.educ.api.dataconversion.entity.student;

import ca.bc.gov.educ.api.dataconversion.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "STUDENT_CAREER_PROGRAM")
public class StudentCareerProgramEntity extends BaseEntity {
   
	@Id
	@Column(name = "STUDENT_CAREER_PROGRAM_ID", nullable = false)
    private UUID id;
	
	@Column(name = "CAREER_PROGRAM_CODE", nullable = false)
    private String careerProgramCode;
	
	@Column(name = "GRADUATION_STUDENT_RECORD_ID", nullable = false)
    private UUID studentID;
	
}