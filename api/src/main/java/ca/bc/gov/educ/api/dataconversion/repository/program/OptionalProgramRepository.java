package ca.bc.gov.educ.api.dataconversion.repository.program;

import ca.bc.gov.educ.api.dataconversion.entity.program.OptionalProgramEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OptionalProgramRepository extends JpaRepository<OptionalProgramEntity, UUID> {

	Optional<OptionalProgramEntity> findByGraduationProgramCodeAndOptProgramCode(String programCode, String optionalProgramCode);
	List<OptionalProgramEntity> findByGraduationProgramCode(String programCode);

	@Query(value="select count(*) from OPTIONAL_PROGRAM_CODE opc \n" +
			"where opc.OPTIONAL_PROGRAM_CODE = :programCode \n", nativeQuery=true)
	long countOptionalProgram(@Param("programCode") String programCode);

}
