package ca.bc.gov.educ.api.dataconversion.model.tsw;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class GradSearchStudent {

	private String studentID;
	private String pen;
	private String legalFirstName;
	private String legalMiddleNames;
	private String legalLastName;
	private String dob;
	private String sexCode;
	private String genderCode;
	private String studentCitizenship;
	private String usualFirstName;
	private String usualMiddleNames;
	private String usualLastName;
	private String email;
	private String emailVerified;
	private String deceasedDate;
	private String postalCode;
	private String mincode;
	
	private String localID;
	private String gradeCode;
	private String gradeYear;
	private String demogCode;
	private String statusCode;
	private String memo;
	private String trueStudentID;
	private String program;
	private String schoolOfRecord;
	private String schoolOfRecordName;
	private String schoolOfRecordindependentAffiliation;
	private String studentGrade;
	private String studentStatus;
	  
}
