package ca.bc.gov.educ.api.dataconversion.repository;

import ca.bc.gov.educ.api.dataconversion.entity.GradCourseRestrictionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

	@Query(value="select trim(c1.crse_code) as CRSE_MAIN, trim(c1.crse_level) as CRSE_MAIN_LVL,\n" +
					" trim(c2.crse_code) as CRSE_RESTRICTED, trim(c2.crse_level) as CRSE_RESTRICTED_LVL,\n" +
					" trim(c1.start_restrict_session) as RESTRICTION_START_DT, trim(c1.end_restrict_session) as RESTRICTION_END_DT\n" +
					"from tab_crse c1\n" +
					"join tab_crse c2\n" +
					"on c1.restriction_code = c2.restriction_code\n" +
					"and (c1.crse_code  <> c2.crse_code or c1.crse_level <> c2.crse_level)\n" +
					"and c1.restriction_code <> ' '", nativeQuery=true)
	@Transactional(readOnly = true)
	List<Object[]> loadInitialRawData();
}
