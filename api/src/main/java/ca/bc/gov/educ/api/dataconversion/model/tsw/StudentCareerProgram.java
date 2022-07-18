package ca.bc.gov.educ.api.dataconversion.model.tsw;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = false)
@Component
public class StudentCareerProgram {

	private UUID id;	
	private String careerProgramCode;	
	private String careerProgramName;
	private UUID studentID;
	
}
