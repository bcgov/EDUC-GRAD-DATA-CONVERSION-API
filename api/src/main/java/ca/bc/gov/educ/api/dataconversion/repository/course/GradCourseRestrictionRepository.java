package ca.bc.gov.educ.api.dataconversion.repository.course;

import ca.bc.gov.educ.api.dataconversion.entity.course.GradCourseRestrictionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GradCourseRestrictionRepository extends JpaRepository<GradCourseRestrictionEntity, UUID> {

	List<GradCourseRestrictionEntity> findByMainCourseAndMainCourseLevel(String courseCode, String courseLevel);

	List<GradCourseRestrictionEntity> findByMainCourseAndRestrictedCourse(String mainCourseCode, String restrictedCourseCode);

	Optional<GradCourseRestrictionEntity> findByMainCourseAndMainCourseLevelAndRestrictedCourseAndRestrictedCourseLevel(
			String courseCode, String courseLevel, String restrictedCourseCode, String restrictedCourseCodeLevel);

	@Query(value="select count(*) from STUD_XCRSE sx, COURSE_REQUIREMENT cr\n" +
			"where sx.stud_no = :pen \n" +
			"and trim(sx.crse_code) = cr.course_code\n" +
			"and trim(sx.crse_level) = cr.course_level\n" +
			"and cr.course_requirement_code = 202\n", nativeQuery=true)
	long countFrenchImmersionCourses(@Param("pen") String pen);
}
