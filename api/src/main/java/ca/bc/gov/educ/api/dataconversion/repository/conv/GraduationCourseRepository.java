package ca.bc.gov.educ.api.dataconversion.repository.conv;

import ca.bc.gov.educ.api.dataconversion.entity.conv.GraduationCourseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface GraduationCourseRepository extends JpaRepository<GraduationCourseEntity, UUID> {
}
