package ca.bc.gov.educ.api.dataconversion.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = false)
@Component
public class GradSpecialProgram extends BaseModel{

	private UUID optionalProgramID;
    private String optionalProgramName;
    private String optProgramCode;
    private String graduationProgramCode;

    private String description;
    private int displayOrder;
    private Date effectiveDate;
    private Date expiryDate;
}
