package ca.bc.gov.educ.api.dataconversion.model;

import lombok.Data;

@Data
public class StudentDemographicDTO {
    private String lastName;
    private String firstName;
    private String middleName;

    private String birthday;
}
