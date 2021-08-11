package ca.bc.gov.educ.api.dataconversion.model;

import lombok.Data;

@Data
public class LoadStudentData {

	private String pen;
	private String programCode;
	private String programCompletionDate;
    private String gpa;
    private String honoursStanding;
	private String school;
	private String studentGrade;
	private String studentStatus;
	
}
