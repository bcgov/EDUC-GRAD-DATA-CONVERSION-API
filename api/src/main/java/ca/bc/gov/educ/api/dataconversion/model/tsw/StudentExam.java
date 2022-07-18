package ca.bc.gov.educ.api.dataconversion.model.tsw;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudentExam {

    private String pen;
    private String courseCode;
    private String courseName;
    private String courseLevel;
    private String sessionDate;
    private String gradReqMet;
    private String courseType;
    private Double completedCourseSchoolPercentage;
    private Double completedCourseExamPercentage;
    private Double completedCourseFinalPercentage;
    private String completedCourseLetterGrade;
    private Double interimPercent;
    private String interimLetterGrade;
    private Integer credits;
    private Integer creditsUsedForGrad;
    private String reqMetLiteracyNumeracy;
    private String wroteFlag;
    private String specialCase;

    @Override
    public String toString() {
        return "StudentExam [pen=" + pen + ", courseCode=" + courseCode + ", courseLevel=" + courseLevel
                + ", sessionDate=" + sessionDate + ", gradReqMet=" + gradReqMet + ", courseType=" + courseType
                + ", completedCourseSchoolPercentage=" + completedCourseSchoolPercentage
                + ", completedCourseExamPercentage=" + completedCourseExamPercentage
                + ", completedCourseFinalPercentage=" + completedCourseFinalPercentage + ", completedCourseLetterGrade="
                + completedCourseLetterGrade + ", interimPercent=" + interimPercent + ", interimLetterGrade="
                + interimLetterGrade + ", credits=" + credits + ", creditsUsedForGrad=" + creditsUsedForGrad
                + ", reqMetLiteracyNumeracy=" + reqMetLiteracyNumeracy + "]";
    }




}
