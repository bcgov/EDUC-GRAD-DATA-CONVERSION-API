package ca.bc.gov.educ.api.dataconversion.service.assessment;

import ca.bc.gov.educ.api.dataconversion.entity.assessment.AssessmentRequirementCodeEntity;
import ca.bc.gov.educ.api.dataconversion.entity.assessment.AssessmentRequirementEntity;
import ca.bc.gov.educ.api.dataconversion.repository.assessment.AssessmentRequirementCodeRepository;
import ca.bc.gov.educ.api.dataconversion.repository.assessment.AssessmentRequirementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class AssessmentService {
    private final AssessmentRequirementCodeRepository assessmentRequirementCodeRepository;
    private final AssessmentRequirementRepository assessmentRequirementRepository;

    @Autowired
    public AssessmentService(AssessmentRequirementCodeRepository assessmentRequirementCodeRepository,
                             AssessmentRequirementRepository assessmentRequirementRepository) {
        this.assessmentRequirementCodeRepository = assessmentRequirementCodeRepository;
        this.assessmentRequirementRepository = assessmentRequirementRepository;
    }

    @Transactional(transactionManager = "assessmentTransactionManager")
    public void processAssessmentRequirement() {
        createAssessmentRequirement("NME", "116");
        createAssessmentRequirement("NME10", "116");

        createAssessmentRequirement("NMF", "116");
        createAssessmentRequirement("NMF10", "116");

        createAssessmentRequirement("LTE10", "115");
        createAssessmentRequirement("LTE10", "403");

        createAssessmentRequirement("LTP10", "115");
        createAssessmentRequirement("LTP10", "303");

        createAssessmentRequirement("LTE12", "118");
        createAssessmentRequirement("LTE12", "404");

        createAssessmentRequirement("LTP12", "118");
        createAssessmentRequirement("LTP12", "304");

        createAssessmentRequirement("LTF12", "203");
    }

    private void createAssessmentRequirement(String assessmentCode, String assessmentRequirementCode) {
        AssessmentRequirementEntity assessmentRequirementEntity = populate(assessmentCode, assessmentRequirementCode);

        AssessmentRequirementEntity currentEntity = assessmentRequirementRepository.findByAssessmentCodeAndRuleCode(
                assessmentRequirementEntity.getAssessmentCode(), assessmentRequirementEntity.getRuleCode());
        if (currentEntity != null) {
            // Update
            assessmentRequirementRepository.save(currentEntity);
        } else {
            // Add
            assessmentRequirementRepository.save(assessmentRequirementEntity);
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
