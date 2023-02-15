package ca.bc.gov.educ.api.dataconversion.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = false)
@Component
public class StudentOptionalProgramReq extends BaseModel{

	private UUID id;
    private String pen;
    private String optionalProgramCompletionDate;
    private String optionalProgramCode;
    private String mainProgramCode;
    private UUID studentID;
				
}
