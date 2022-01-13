package ca.bc.gov.educ.api.dataconversion.model;

import lombok.Data;

import java.util.UUID;

@Data
public class StudentGradDTO extends  StudentCommonDTO {
    private UUID studentID;
}
