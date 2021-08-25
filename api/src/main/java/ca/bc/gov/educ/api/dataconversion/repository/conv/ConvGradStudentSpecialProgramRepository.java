package ca.bc.gov.educ.api.dataconversion.repository.conv;

import ca.bc.gov.educ.api.dataconversion.entity.conv.ConvGradStudentSpecialProgramEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConvGradStudentSpecialProgramRepository extends JpaRepository<ConvGradStudentSpecialProgramEntity, UUID> {

    Optional<ConvGradStudentSpecialProgramEntity> findByStudentIDAndOptionalProgramID(UUID studentID, UUID optionalProgramID);

}

