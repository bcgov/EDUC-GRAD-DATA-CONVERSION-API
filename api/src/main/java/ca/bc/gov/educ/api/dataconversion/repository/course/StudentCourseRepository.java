package ca.bc.gov.educ.api.dataconversion.repository.course;

import ca.bc.gov.educ.api.dataconversion.entity.course.StudentCourseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StudentCourseRepository extends JpaRepository<StudentCourseEntity, UUID> {

    @Query("select c from StudentCourseEntity c where c.courseKey.pen=:pen")
    List<StudentCourseEntity> findByPen(String pen);

}
