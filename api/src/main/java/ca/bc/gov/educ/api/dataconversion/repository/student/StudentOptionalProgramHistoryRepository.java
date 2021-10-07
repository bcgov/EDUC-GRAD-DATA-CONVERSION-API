package ca.bc.gov.educ.api.dataconversion.repository.student;

import ca.bc.gov.educ.api.dataconversion.entity.student.StudentOptionalProgramHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StudentOptionalProgramHistoryRepository extends JpaRepository<StudentOptionalProgramHistoryEntity, UUID> {

    List<StudentOptionalProgramHistoryEntity> findAll();
	List<StudentOptionalProgramHistoryEntity> findByStudentID(UUID studentID);
}
