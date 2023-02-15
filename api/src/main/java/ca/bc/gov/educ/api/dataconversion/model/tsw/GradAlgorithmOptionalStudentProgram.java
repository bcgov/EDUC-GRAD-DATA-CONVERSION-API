package ca.bc.gov.educ.api.dataconversion.model.tsw;

import ca.bc.gov.educ.api.dataconversion.model.StudentCareerProgram;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Data
@Component
public class GradAlgorithmOptionalStudentProgram {

	private String pen;
    private UUID optionalProgramID;
    private String optionalProgramCode;
    private String studentOptionalProgramData;
    private String optionalProgramCompletionDate;
    private StudentCourses optionalStudentCourses;
    private StudentAssessments optionalStudentAssessments; // null
    private boolean isOptionalGraduated;
    private List<GradRequirement> optionalNonGradReasons; // null
    private List<GradRequirement> optionalRequirementsMet;
    private UUID studentID;
    private List<StudentCareerProgram> cpList;
    private String optionalProgramName;
}
