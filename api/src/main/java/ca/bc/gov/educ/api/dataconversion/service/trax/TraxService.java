package ca.bc.gov.educ.api.dataconversion.service.trax;

import ca.bc.gov.educ.api.dataconversion.repository.trax.GraduationCourseRepository;
import ca.bc.gov.educ.api.dataconversion.repository.trax.TraxStudentRepository;
import ca.bc.gov.educ.api.dataconversion.repository.trax.TraxStudentsLoadRepository;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class TraxService {
    private final TraxStudentsLoadRepository traxStudentsLoadRepository;

    @Autowired
    public TraxService(TraxStudentsLoadRepository traxStudentsLoadRepository) {
        this.traxStudentsLoadRepository = traxStudentsLoadRepository;
    }

    @Transactional(readOnly = true, transactionManager = "traxTransactionManager")
    public boolean existsSchool(String minCode) {
        return traxStudentsLoadRepository.countTabSchools(minCode) > 0L;
    }
}
