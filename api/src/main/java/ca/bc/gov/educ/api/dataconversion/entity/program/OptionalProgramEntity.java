package ca.bc.gov.educ.api.dataconversion.entity.program;

import ca.bc.gov.educ.api.dataconversion.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper=false)
@Entity
@Table(name = "OPTIONAL_PROGRAM")
public class OptionalProgramEntity extends BaseEntity {
   
	@Id
	@GeneratedValue(generator = "UUID")
	@GenericGenerator(
		name = "UUID",
		strategy = "org.hibernate.id.UUIDGenerator"
	)
	@Column(name = "OPTIONAL_PROGRAM_ID", nullable = false)
    private UUID optionalProgramID; 
	
	@Column(name = "OPTIONAL_PROGRAM_CODE", nullable = true)
    private String optProgramCode;
	
	@Column(name = "LABEL", nullable = true)
    private String optionalProgramName;
	
	@Column(name = "DESCRIPTION", nullable = true)
    private String description;
	
	@Column(name = "DISPLAY_ORDER", nullable = true)
    private int displayOrder; 
	
	@Column(name = "EFFECTIVE_DATE", nullable = true)
    private Date effectiveDate;
	
	@Column(name = "EXPIRY_DATE", nullable = true)
    private Date expiryDate;

	@Column(name = "GRADUATION_PROGRAM_CODE", nullable = true)
    private String graduationProgramCode;
}