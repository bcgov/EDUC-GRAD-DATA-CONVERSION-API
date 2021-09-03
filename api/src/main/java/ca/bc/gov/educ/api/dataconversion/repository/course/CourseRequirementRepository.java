package ca.bc.gov.educ.api.dataconversion.repository.course;

import ca.bc.gov.educ.api.dataconversion.entity.course.CourseRequirementCodeEntity;
import ca.bc.gov.educ.api.dataconversion.entity.course.CourseRequirementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CourseRequirementRepository extends JpaRepository<CourseRequirementEntity, UUID> {
	@Query(value="select count(*) from TRAX_STUDENT_COURSES tsc, COURSE_REQUIREMENT cr \n" +
			"where tsc.pen = :pen \n" +
			"and trim(tsc.crse_code) = cr.course_code \n" +
			"and trim(tsc.crse_level) = cr.course_level \n" +
			"and trim(tsc.crse_code) in ('FRAL', 'FRALP') \n" +
			"and trim(tsc.crse_level) = '10'", nativeQuery=true)
	long countFrenchImmersionCourses(@Param("pen") String pen);

	@Query(value="select count(*) from TAB_CRSE cr \n" +
			"where trim(cr.crse_code) = :courseCode \n" +
			"and trim(cr.crse_level) = :courseLevel \n" +
			"and cr.language = :lang", nativeQuery=true)
	long countTabCourses(@Param("courseCode") String courseCode, @Param("courseLevel") String courseLevel, @Param("lang") String lang);

	List<CourseRequirementEntity> findByCourseCodeAndCourseLevel(String courseCode, String courseLevel);

	CourseRequirementEntity findByCourseCodeAndCourseLevelAndRuleCode(String courseCode, String courseLevel, CourseRequirementCodeEntity ruleCode);
}