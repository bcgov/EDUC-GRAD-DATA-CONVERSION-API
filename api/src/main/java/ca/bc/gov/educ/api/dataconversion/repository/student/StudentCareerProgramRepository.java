package ca.bc.gov.educ.api.dataconversion.repository.student;

import ca.bc.gov.educ.api.dataconversion.entity.student.StudentCareerProgramEntity;
import ca.bc.gov.educ.api.dataconversion.entity.student.StudentOptionalProgramEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentCareerProgramRepository extends JpaRepository<StudentCareerProgramEntity, UUID> {

	Optional<StudentCareerProgramEntity> findByStudentIDAndCareerProgramCode(UUID studentID, String cpCode);

	List<StudentCareerProgramEntity> findByStudentID(UUID studentId);

	@Query("select c from StudentCareerProgramEntity c where c.careerProgramCode=:cpCode")
	List<StudentCareerProgramEntity> existsByCareerProgramCode(String cpCode);
}

