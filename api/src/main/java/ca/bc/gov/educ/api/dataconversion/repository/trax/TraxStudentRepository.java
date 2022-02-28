package ca.bc.gov.educ.api.dataconversion.repository.trax;

import ca.bc.gov.educ.api.dataconversion.entity.trax.TraxStudentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TraxStudentRepository extends JpaRepository<TraxStudentEntity, String> {

    @QueryHints(@javax.persistence.QueryHint(name="org.hibernate.fetchSize", value="500"))
    List<TraxStudentEntity> findAll();
}
