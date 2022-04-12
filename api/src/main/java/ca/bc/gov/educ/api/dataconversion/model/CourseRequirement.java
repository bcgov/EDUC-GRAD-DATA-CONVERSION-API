package ca.bc.gov.educ.api.dataconversion.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = false)
public class CourseRequirement extends BaseModel {

	private UUID courseRequirementId;
	private String courseCode;
    private String courseLevel;
    private CourseRequirementCodeDTO ruleCode;

    public String getCourseCode() {
        if (courseCode != null)
            courseCode = courseCode.trim();
        return courseCode;
    }

    public String getCourseLevel() {
        if (courseLevel != null)
            courseLevel = courseLevel.trim();
        return courseLevel;
    }
}
