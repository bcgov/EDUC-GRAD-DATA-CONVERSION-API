package ca.bc.gov.educ.api.dataconversion.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GradCourseRestriction {
    private String mainCourse;
    private String mainCourseLevel;
    private String restrictedCourse;
    private String restrictedCourseLevel;
    private String restrictionStartDate;
    private String restrictionEndDate;
}
