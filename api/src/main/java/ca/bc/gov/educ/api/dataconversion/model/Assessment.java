package ca.bc.gov.educ.api.dataconversion.model;

import lombok.Data;

import java.sql.Date;

@Data
public class Assessment {

	private String assessmentCode;
    private String assessmentName;
    private String language;    
    private Date startDate;
    private Date endDate;
    
	@Override
	public String toString() {
		return "Assessment [assessmentCode=" + assessmentCode + ", assessmentName=" + assessmentName + ", language="
				+ language + ", startDate=" + startDate + ", endDate=" + endDate + "]";
	}
    
			
}
