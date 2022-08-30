package ca.bc.gov.educ.api.dataconversion.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.stereotype.Component;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = false)
@Component
public class RequirementTypeCode extends BaseModel {

	private String reqTypeCode;
	private String label;
	private int displayOrder; 
	private String description;
	private Date effectiveDate; 
	private Date expiryDate;
	
	
	@Override
	public String toString() {
		return "RequirementTypeCode [reqTypeCode=" + reqTypeCode + ", label=" + label + ", displayOrder=" + displayOrder
				+ ", description=" + description + ", effectiveDate=" + effectiveDate + ", expiryDate=" + expiryDate
				+ "]";
	}
	
	
	
}