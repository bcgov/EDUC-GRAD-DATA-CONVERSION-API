package ca.bc.gov.educ.api.dataconversion.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
public class CourseRestriction extends BaseModel {
    private UUID courseRestrictionId;
    private String mainCourse;
    private String mainCourseLevel;
    private String restrictedCourse;
    private String restrictedCourseLevel;
    private String restrictionStartDate;
    private String restrictionEndDate;
}
