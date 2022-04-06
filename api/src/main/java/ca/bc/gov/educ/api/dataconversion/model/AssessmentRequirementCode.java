package ca.bc.gov.educ.api.dataconversion.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.stereotype.Component;

import java.sql.Date;

@Data
@EqualsAndHashCode(callSuper = false)
@Component
public class AssessmentRequirementCode {

	private String assmtRequirementCode;
	private String label;
	private String description;
	private Date effectiveDate;
	private Date expiryDate;

	private String createdBy;
	private java.util.Date createdTimestamp;
	private String updatedBy;
	private java.util.Date updatedTimestamp;
}
