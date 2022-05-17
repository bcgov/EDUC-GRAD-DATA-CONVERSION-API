package ca.bc.gov.educ.api.dataconversion.model;

import lombok.*;

import java.util.UUID;

@Data
@Builder
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
