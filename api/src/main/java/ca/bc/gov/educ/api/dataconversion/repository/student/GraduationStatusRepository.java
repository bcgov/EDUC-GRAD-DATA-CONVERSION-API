package ca.bc.gov.educ.api.dataconversion.repository.student;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ca.bc.gov.educ.api.dataconversion.entity.student.GraduationStatusEntity;

@Repository
public interface GraduationStatusRepository extends JpaRepository<GraduationStatusEntity, UUID> {

}
