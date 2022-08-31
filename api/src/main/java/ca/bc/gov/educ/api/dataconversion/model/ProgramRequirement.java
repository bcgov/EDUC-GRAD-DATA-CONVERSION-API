package ca.bc.gov.educ.api.dataconversion.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper=false)
@Component
public class ProgramRequirement extends BaseModel {

	private UUID programRequirementID; 
	private String graduationProgramCode;
	private ProgramRequirementCode programRequirementCode;
}
