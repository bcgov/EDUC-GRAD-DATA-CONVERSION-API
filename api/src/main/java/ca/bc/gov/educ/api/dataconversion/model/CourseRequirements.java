package ca.bc.gov.educ.api.dataconversion.model;

import lombok.Data;

import java.util.List;

@Data
public class CourseRequirements {
    List<CourseRequirement> courseRequirementList;
}
