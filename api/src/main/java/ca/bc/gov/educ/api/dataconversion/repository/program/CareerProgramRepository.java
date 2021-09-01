package ca.bc.gov.educ.api.dataconversion.repository.program;

import ca.bc.gov.educ.api.dataconversion.entity.program.CareerProgramEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CareerProgramRepository extends JpaRepository<CareerProgramEntity, String> {

}
