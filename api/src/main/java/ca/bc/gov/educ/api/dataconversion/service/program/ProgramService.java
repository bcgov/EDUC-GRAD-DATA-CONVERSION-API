package ca.bc.gov.educ.api.dataconversion.service.program;

import ca.bc.gov.educ.api.dataconversion.entity.program.CareerProgramEntity;
import ca.bc.gov.educ.api.dataconversion.repository.program.CareerProgramRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ProgramService {
    private final CareerProgramRepository careerProgramRepository;

    public ProgramService(CareerProgramRepository careerProgramRepository) {
        this.careerProgramRepository = careerProgramRepository;
    }

    @Transactional(readOnly = true, transactionManager = "programTransactionManager")
    public CareerProgramEntity getCareerProgramCode(String cpc) {
        Optional<CareerProgramEntity> entity = careerProgramRepository.findById(StringUtils.toRootUpperCase(cpc));
        if (entity.isPresent()) {
            return entity.get();
        } else {
            return null;
        }
    }
}
