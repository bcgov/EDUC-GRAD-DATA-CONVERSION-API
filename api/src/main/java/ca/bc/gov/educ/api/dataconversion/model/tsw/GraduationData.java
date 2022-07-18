package ca.bc.gov.educ.api.dataconversion.model.tsw;

import ca.bc.gov.educ.api.dataconversion.model.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GraduationData {
    private GradSearchStudent gradStudent;
    private GradAlgorithmGraduationStudentRecord gradStatus;
    private List<GradAlgorithmOptionalStudentProgram> optionalGradStatus;
    private School school;
    private StudentCourses studentCourses;
    private StudentAssessments studentAssessments;   // nullable
    private StudentExams studentExams; // nullable
    private List<GradRequirement> nonGradReasons; // nullable
    private List<GradRequirement> requirementsMet;
    private String gradMessage;
    //Student Career Programs
    private boolean dualDogwood;
    private boolean isGraduated;
    private ExceptionMessage exception;
    private GraduationProgramCode gradProgram;
    
}
