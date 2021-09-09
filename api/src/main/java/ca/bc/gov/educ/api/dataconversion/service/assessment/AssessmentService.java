package ca.bc.gov.educ.api.dataconversion.service.assessment;

import ca.bc.gov.educ.api.dataconversion.entity.assessment.AssessmentRequirementCodeEntity;
import ca.bc.gov.educ.api.dataconversion.entity.assessment.AssessmentRequirementEntity;
import ca.bc.gov.educ.api.dataconversion.model.ConversionCourseSummaryDTO;
import ca.bc.gov.educ.api.dataconversion.repository.assessment.AssessmentRequirementCodeRepository;
import ca.bc.gov.educ.api.dataconversion.repository.assessment.AssessmentRequirementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class AssessmentService {

    private static final Logger logger = LoggerFactory.getLogger(AssessmentService.class);

    private final AssessmentRequirementCodeRepository assessmentRequirementCodeRepository;
    private final AssessmentRequirementRepository assessmentRequirementRepository;

    @Autowired
    public AssessmentService(AssessmentRequirementCodeRepository assessmentRequirementCodeRepository,
                             AssessmentRequirementRepository assessmentRequirementRepository) {
        this.assessmentRequirementCodeRepository = assessmentRequirementCodeRepository;
        this.assessmentRequirementRepository = assessmentRequirementRepository;
    }

    @Transactional(transactionManager = "assessmentTransactionManager")
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
        AssessmentRequirementEntity assessmentRequirementEntity = populate(assessmentCode, assessmentRequirementCode);

        AssessmentRequirementEntity currentEntity = assessmentRequirementRepository.findByAssessmentCodeAndRuleCode(
                assessmentRequirementEntity.getAssessmentCode(), assessmentRequirementEntity.getRuleCode());
        logger.info(" Create AssessmentRequirement: assessment [{}], rule [{}]", assessmentCode, assessmentRequirementCode);
        if (currentEntity != null) {
            // Update
            currentEntity.setUpdateDate(null);
            currentEntity.setUpdateUser(null);
            assessmentRequirementRepository.save(currentEntity);
            summary.setUpdatedCountForAssessmentRequirement(summary.getUpdatedCountForAssessmentRequirement() + 1L);

        } else {
            // Add
            assessmentRequirementRepository.save(assessmentRequirementEntity);
            summary.setAddedCountForAssessmentRequirement(summary.getAddedCountForAssessmentRequirement() + 1L);
        }
    }

    private AssessmentRequirementEntity populate(String assessmentCode, String assessmentRequirementCode) {
        AssessmentRequirementEntity assessmentRequirement = new AssessmentRequirementEntity();
        assessmentRequirement.setAssessmentCode(assessmentCode);

        Optional<AssessmentRequirementCodeEntity> assessmentCodeRequirementCodeOptional = assessmentRequirementCodeRepository.findById(assessmentRequirementCode);
        if (assessmentCodeRequirementCodeOptional.isPresent()) {
            assessmentRequirement.setRuleCode(assessmentCodeRequirementCodeOptional.get());
        }
        assessmentRequirement.setAssessmentRequirementId(UUID.randomUUID());
        return assessmentRequirement;
    }
}
