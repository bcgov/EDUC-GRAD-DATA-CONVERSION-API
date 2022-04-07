package ca.bc.gov.educ.api.dataconversion.model;

import lombok.Data;
import org.springframework.stereotype.Component;

@Data
@Component
public class CareerProgram {
	
	private String code;
	private String description;
	private String startDate;
	private String endDate;
	
	public String getCode() {
    	return code != null ? code.trim():null;
    }
	
	public String getDescription() {
		return description != null ? description.trim(): null;
	}
	
	@Override
	public String toString() {
		return "GradCareerProgram [code=" + code + ", description=" + description + ", startDate=" + startDate
				+ ", endDate=" + endDate + "]";
	}
	
}
