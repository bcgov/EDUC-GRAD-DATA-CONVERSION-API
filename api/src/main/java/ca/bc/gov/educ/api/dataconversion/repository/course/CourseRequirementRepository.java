package ca.bc.gov.educ.api.dataconversion.repository.course;

import ca.bc.gov.educ.api.dataconversion.entity.course.CourseRequirementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CourseRequirementRepository extends JpaRepository<CourseRequirementEntity, UUID> {

	@Query(value="select count(*) from trax_student_courses\n" +
			"where pen = :pen \n" +
			"and crse_code in ('FRAL', 'FRALP') \n" +
			"and crse_level = '10'", nativeQuery=true)
	long countFRALAndFRALPCourses(@Param("pen") String pen);

}