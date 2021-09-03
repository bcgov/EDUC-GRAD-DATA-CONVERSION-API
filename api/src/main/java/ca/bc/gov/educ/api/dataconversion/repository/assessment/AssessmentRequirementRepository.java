package ca.bc.gov.educ.api.dataconversion.repository.assessment;

import ca.bc.gov.educ.api.dataconversion.entity.assessment.AssessmentRequirementCodeEntity;
import ca.bc.gov.educ.api.dataconversion.entity.assessment.AssessmentRequirementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AssessmentRequirementRepository extends JpaRepository<AssessmentRequirementEntity, UUID> {

	AssessmentRequirementEntity findByAssessmentCodeAndRuleCode(String assessmentCode, AssessmentRequirementCodeEntity ruleCode);

}
