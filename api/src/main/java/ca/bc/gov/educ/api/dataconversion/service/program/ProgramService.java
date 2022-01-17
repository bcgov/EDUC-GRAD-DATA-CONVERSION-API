package ca.bc.gov.educ.api.dataconversion.service.program;

import ca.bc.gov.educ.api.dataconversion.entity.program.CareerProgramEntity;
import ca.bc.gov.educ.api.dataconversion.entity.program.OptionalProgramEntity;
import ca.bc.gov.educ.api.dataconversion.repository.program.CareerProgramRepository;
import ca.bc.gov.educ.api.dataconversion.repository.program.OptionalProgramRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class ProgramService {
    private final CareerProgramRepository careerProgramRepository;
    private final OptionalProgramRepository optionalProgramRepository;

    public ProgramService(CareerProgramRepository careerProgramRepository,
                          OptionalProgramRepository optionalProgramRepository) {
        this.careerProgramRepository = careerProgramRepository;
        this.optionalProgramRepository = optionalProgramRepository;
    }

    @Transactional(readOnly = true, transactionManager = "programTransactionManager")
    public CareerProgramEntity getCareerProgram(String cpc) {
        Optional<CareerProgramEntity> entity = careerProgramRepository.findById(StringUtils.toRootUpperCase(cpc));
        if (entity.isPresent()) {
            return entity.get();
        } else {
            return null;
        }
    }

    @Transactional(readOnly = true, transactionManager = "programTransactionManager")
    public OptionalProgramEntity getOptionalProgram(String programCode, String optionalProgramCode) {
        Optional<OptionalProgramEntity> entity = optionalProgramRepository.findByGraduationProgramCodeAndOptProgramCode(programCode, optionalProgramCode);
        if (entity.isPresent()) {
            return entity.get();
        } else {
            return null;
        }
    }
<<<<<<< HEAD

    @Transactional(readOnly = true, transactionManager = "programTransactionManager")
    public OptionalProgramEntity findOptionalProgram(UUID optionalProgramID) {
        Optional<OptionalProgramEntity> entity = optionalProgramRepository.findById(optionalProgramID);
        if (entity.isPresent()) {
            return entity.get();
        } else {
            return null;
        }
    }

    @Transactional(readOnly = true, transactionManager = "programTransactionManager")
    public boolean isOptionalProgramCode(String programCode) {
        if (optionalProgramRepository.countOptionalProgram(programCode) > 0L) {
            return true;
        }
        return false;
    }
=======
>>>>>>> 9e1a14b... second commit
}
