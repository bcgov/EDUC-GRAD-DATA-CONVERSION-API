package ca.bc.gov.educ.api.dataconversion.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = false)
@Component
public class GradSpecialProgram extends BaseModel{

	private UUID id;
    private String specialProgramName;
    private String specialProgramCode;
    private String programCode;

}
