package ca.bc.gov.educ.api.dataconversion.service.assessment;

import ca.bc.gov.educ.api.dataconversion.model.AssessmentRequirement;
import ca.bc.gov.educ.api.dataconversion.model.AssessmentRequirementCode;
import ca.bc.gov.educ.api.dataconversion.model.ConversionCourseSummaryDTO;
import ca.bc.gov.educ.api.dataconversion.model.StudentAssessment;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AssessmentService {

    private static final Logger logger = LoggerFactory.getLogger(AssessmentService.class);
    private final RestUtils restUtils;

    @Autowired
    public AssessmentService(RestUtils restUtils) {
        this.restUtils = restUtils;
    }

    public void createAssessmentRequirements(ConversionCourseSummaryDTO summary) {
        createAssessmentRequirement("NME", "116", summary);
        createAssessmentRequirement("NME10", "116", summary);
        createAssessmentRequirement("NMF", "116", summary);
        createAssessmentRequirement("NMF10", "116", summary);

        createAssessmentRequirement("LTE10", "115", summary);
        createAssessmentRequirement("LTE10", "403", summary);

        createAssessmentRequirement("LTP10", "115", summary);
        createAssessmentRequirement("LTP10", "303", summary);

        createAssessmentRequirement("LTE12", "118", summary);
        createAssessmentRequirement("LTE12", "404", summary);

        createAssessmentRequirement("LTP12", "118", summary);
        createAssessmentRequirement("LTP12", "304", summary);

        createAssessmentRequirement("LTF12", "203", summary);
    }

    private void createAssessmentRequirement(String assessmentCode, String assessmentRequirementCode, ConversionCourseSummaryDTO summary) {
        logger.info("GRAD ASSESSMENT API - Create AssessmentRequirement: assessment [{}], rule [{}]", assessmentCode, assessmentRequirementCode);
        AssessmentRequirement requestAssessmentRequirement = populate(assessmentCode, assessmentRequirementCode);
        AssessmentRequirement responseAssessmentRequirement = restUtils.addAssessmentRequirement(requestAssessmentRequirement, summary.getAccessToken());
        if (responseAssessmentRequirement != null) {
            summary.setAddedCountForAssessmentRequirement(summary.getAddedCountForAssessmentRequirement() + 1L);
        }
    }

    private AssessmentRequirement populate(String assessmentCode, String assessmentRequirementCode) {
        AssessmentRequirement assessmentRequirement = new AssessmentRequirement();
        assessmentRequirement.setAssessmentCode(assessmentCode);

        AssessmentRequirementCode ruleCode = new AssessmentRequirementCode();
        ruleCode.setAssmtRequirementCode(assessmentRequirementCode);

        assessmentRequirement.setRuleCode(ruleCode);
        return assessmentRequirement;
    }

    public List<StudentAssessment> getStudentAssessments(String pen, String accessToken) {
       return restUtils.getStudentAssessmentsByPen(pen, accessToken);
    }
}
