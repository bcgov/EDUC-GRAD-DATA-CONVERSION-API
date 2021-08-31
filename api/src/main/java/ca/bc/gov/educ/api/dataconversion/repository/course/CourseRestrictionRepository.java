package ca.bc.gov.educ.api.dataconversion.repository.course;

import ca.bc.gov.educ.api.dataconversion.entity.course.CourseRestrictionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseRestrictionRepository extends JpaRepository<CourseRestrictionEntity, UUID> {

	List<CourseRestrictionEntity> findByMainCourseAndMainCourseLevel(String courseCode, String courseLevel);

	List<CourseRestrictionEntity> findByMainCourseAndRestrictedCourse(String mainCourseCode, String restrictedCourseCode);

	Optional<CourseRestrictionEntity> findByMainCourseAndMainCourseLevelAndRestrictedCourseAndRestrictedCourseLevel(
			String courseCode, String courseLevel, String restrictedCourseCode, String restrictedCourseCodeLevel);
}
