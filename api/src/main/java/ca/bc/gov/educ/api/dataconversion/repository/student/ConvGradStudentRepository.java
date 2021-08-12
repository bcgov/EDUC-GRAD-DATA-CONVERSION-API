package ca.bc.gov.educ.api.dataconversion.repository.student;

import ca.bc.gov.educ.api.dataconversion.entity.student.ConvGradStudentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConvGradStudentRepository extends JpaRepository<ConvGradStudentEntity, UUID> {

	Optional<ConvGradStudentEntity> findByPen(String pen);

	@Query(value="select trim(m.stud_no) as PEN, m.mincode as SCHOOL_OF_RECORD, m.mincode_grad as SCHOOL_AT_GRAD, m.stud_grade as STUD_GRADE, m.stud_status as STUD_STATUS,\n" +
					"m.grad_reqt_year as GRAD_REQT_YEAR, 'Y' as RECALCULATE_GRAD_STATUS\n" +
					"from trax_students_load l, student_master m\n" +
					"where 1 = 1\n" +
					"and l.stud_no = m.stud_no\n" +
					"and m.grad_date = 0\n" +
					"and m.archive_flag = 'A'\n" , nativeQuery=true)
	@Transactional(readOnly = true)
	List<Object[]> loadInitialRawData();

	@Query(value="select count(*) from STUD_XCRSE sx, GRAD_COURSE_REQUIREMENT gcr\n" +
			"where sx.stud_no = :pen \n" +
			"and trim(sx.crse_code) = gcr.crse_code\n" +
			"and trim(sx.crse_level) = gcr.crse_lvl\n" +
			"and gcr.rule_code = 202\n", nativeQuery=true)
	long countFrenchImmersionCourses(@Param("pen") String pen);

}
