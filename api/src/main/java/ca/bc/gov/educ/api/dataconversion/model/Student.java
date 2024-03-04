package ca.bc.gov.educ.api.dataconversion.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Student {

    String studentID;
    String pen;
    String legalFirstName;
    String legalMiddleNames;
    String legalLastName;
    String dob;
    String sexCode;
    String genderCode;
    String usualFirstName;
    String usualMiddleNames;
    String usualLastName;
    String email;
    String emailVerified;
    String deceasedDate;
    String postalCode;
    String mincode;
    String localID;
    String gradeCode;
    String gradeYear;
    String demogCode;
    String statusCode;
    String memo;
    String trueStudentID;
    String historyActivityCode;

    public String createUser;
    public String updateUser;
    public String createDate;
    public String updateDate;

    String truePen;
}
