package ca.bc.gov.educ.api.dataconversion.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import ca.bc.gov.educ.api.dataconversion.entity.GraduationStatusEntity;

@Repository
public interface GraduationStatusRepository extends JpaRepository<GraduationStatusEntity, String> {

    List<GraduationStatusEntity> findAll();

	List<GraduationStatusEntity> findByRecalculateGradStatus(String recalulateFlag);
	@Query(value="select * from grad_student where student_id is null",nativeQuery=true)
	List<GraduationStatusEntity> findByStudentID();
}
