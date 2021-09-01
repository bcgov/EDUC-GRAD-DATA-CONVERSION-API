package ca.bc.gov.educ.api.dataconversion.repository.conv;

import ca.bc.gov.educ.api.dataconversion.entity.conv.ConvGradStudentEntity;
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

	@Query(value="select trim(m.stud_no) as PEN, m.mincode as SCHOOL_OF_RECORD, m.mincode_grad as SCHOOL_AT_GRADUATION, m.stud_grade as STUDENT_GRADE, m.stud_status as STUDENT_STATUS_CODE,\n" +
					"m.grad_reqt_year as GRAD_REQT_YEAR, 'Y' as RECALCULATE_GRAD_STATUS, m.grad_date as GRAD_DATE,\n" +
					"trim(m.prgm_code) as PRGM_CODE1, trim(m.prgm_code2) as PRGM_CODE2, trim(m.prgm_code3) as PRGM_CODE3, trim(m.prgm_code4) as PRGM_CODE4, trim(m.prgm_code5) as PRGM_CODE5\n" +
					"from trax_students_load l, student_master m\n" +
					"where 1 = 1\n" +
					//"and trim(l.stud_no) = '134232461'\n" +
					"and l.stud_no = m.stud_no\n" , nativeQuery=true)
	@Transactional(readOnly = true)
	List<Object[]> loadInitialRawData();

}
