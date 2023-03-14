package ca.bc.gov.educ.api.dataconversion.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@Builder
@Data
public class CourseRequirementDTO {

	private String courseCode;
    private String courseLevel;
    private String ruleCode;

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

    public String getRuleCode() {
        if (ruleCode != null)
            ruleCode = ruleCode.trim();
        return ruleCode;
    }
}
