package ca.bc.gov.educ.api.dataconversion.model.tsw;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;

@Data
@Component
public class GradAlgorithmGraduationStudentRecord {

    private String studentGradData;
    private String pen;
    private String program;
    private String programName;
    private String programCompletionDate;
    private String gpa;
    private String honoursStanding;
    private String recalculateGradStatus;
    private String schoolOfRecord;
    private String studentGrade;
    private String studentStatus;
    private UUID studentID;
    private String schoolAtGrad;
    private Date lastUpdateDate;
}

