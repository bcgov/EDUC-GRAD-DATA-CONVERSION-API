package ca.bc.gov.educ.api.dataconversion.model;

import ca.bc.gov.educ.api.dataconversion.model.tsw.ExceptionMessage;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.stereotype.Component;

import jakarta.persistence.Transient;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = false)
@Component
public class GraduationStudentRecord extends BaseModel{

    private UUID studentID;
    private String studentGradData;
    private String studentProjectedGradData;
    private String pen;
    private String program;
    private String programName;
    private String programCompletionDate;
    private String gpa;
    private String honoursStanding;
    private String recalculateGradStatus;
    private String recalculateProjectedGrad;
    private UUID schoolOfRecordId;
    private String studentGrade;
    private String studentStatus;
    private Long batchId;
    private UUID schoolAtGradId;
    private String consumerEducationRequirementMet;
    private String studentCitizenship;
    private String adultStartDate;

    // Mappings for Student_Master
    @Transient
    private boolean dualDogwood = false;

	private ExceptionMessage exception;
}
