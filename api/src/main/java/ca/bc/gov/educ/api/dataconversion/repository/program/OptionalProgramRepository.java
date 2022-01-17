package ca.bc.gov.educ.api.dataconversion.repository.program;

import ca.bc.gov.educ.api.dataconversion.entity.program.OptionalProgramEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OptionalProgramRepository extends JpaRepository<OptionalProgramEntity, UUID> {

	Optional<OptionalProgramEntity> findByGraduationProgramCodeAndOptProgramCode(String programCode, String optionalProgramCode);
	List<OptionalProgramEntity> findByGraduationProgramCode(String programCode);

}
