package ca.bc.gov.educ.api.dataconversion.repository.assessment;

import ca.bc.gov.educ.api.dataconversion.entity.assessment.AssessmentRequirementCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssessmentRequirementCodeRepository extends JpaRepository<AssessmentRequirementCodeEntity, String> {

}
