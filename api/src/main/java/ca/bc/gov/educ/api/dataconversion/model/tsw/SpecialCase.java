package ca.bc.gov.educ.api.dataconversion.model.tsw;

import ca.bc.gov.educ.api.dataconversion.model.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.stereotype.Component;

import java.sql.Date;

@Data
@EqualsAndHashCode(callSuper=false)
@Component
public class SpecialCase extends BaseModel {

	private String spCase;	
	private String label;	
	private int displayOrder; 
	private String description;
	private String passFlag;
	private Date effectiveDate; 
	private Date expiryDate;
	
	@Override
	public String toString() {
		return "SpecialCase [spCase=" + spCase + ", label=" + label + ", displayOrder=" + displayOrder
				+ ", description=" + description + ", passFlag=" + passFlag + ", effectiveDate=" + effectiveDate
				+ ", expiryDate=" + expiryDate + "]";
	}
	
	
}
