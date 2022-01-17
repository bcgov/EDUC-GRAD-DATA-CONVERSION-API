package ca.bc.gov.educ.api.dataconversion.repository.assessment;

import ca.bc.gov.educ.api.dataconversion.entity.assessment.StudentAssessmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StudentAssessmentRepository extends JpaRepository<StudentAssessmentEntity, UUID> {

	@Query("select c from StudentAssessmentEntity c where c.assessmentKey.pen=:pen")
    List<StudentAssessmentEntity> findByPen(String pen);

}
