package ca.bc.gov.educ.api.dataconversion.repository.student;

import ca.bc.gov.educ.api.dataconversion.entity.student.StudentOptionalProgramEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentOptionalProgramRepository extends JpaRepository<StudentOptionalProgramEntity, UUID> {
    List<StudentOptionalProgramEntity> findByStudentID(UUID studentID);
    Optional<StudentOptionalProgramEntity> findByStudentIDAndOptionalProgramID(UUID studentID,UUID optionalProgramID);
    void deleteByStudentID(UUID studentID);
}

