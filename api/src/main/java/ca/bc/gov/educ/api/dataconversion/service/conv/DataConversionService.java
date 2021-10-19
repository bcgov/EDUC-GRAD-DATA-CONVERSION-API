package ca.bc.gov.educ.api.dataconversion.service.conv;

import ca.bc.gov.educ.api.dataconversion.entity.trax.GraduationCourseEntity;
import ca.bc.gov.educ.api.dataconversion.model.*;
import ca.bc.gov.educ.api.dataconversion.repository.trax.GraduationCourseRepository;
import ca.bc.gov.educ.api.dataconversion.repository.trax.TraxStudentsLoadRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class DataConversionService {
    private final TraxStudentsLoadRepository traxStudentsLoadRepository;
    private final GraduationCourseRepository graduationCourseRepository;

    @Autowired
    public DataConversionService(TraxStudentsLoadRepository traxStudentsLoadRepository,
                                 GraduationCourseRepository graduationCourseRepository) {
        this.traxStudentsLoadRepository = traxStudentsLoadRepository;
        this.graduationCourseRepository = graduationCourseRepository;
    }

    @Transactional(readOnly = true, transactionManager = "traxTransactionManager")
    public List<ConvGradStudent> loadInitialRawGradStudentData() {
        List<ConvGradStudent> students = new ArrayList<>();
        List<Object[]> results = traxStudentsLoadRepository.loadInitialStudentRawData();
        results.forEach(result -> {
            String pen = (String) result[0];
            String schoolOfRecord = (String) result[1];
            String schoolAtGrad = (String) result[2];
            String studentGrade = (String) result[3];
            Character studentStatus = (Character) result[4];
            String graduationRequestYear = (String) result[5];

            Character recalculateGradStatus = (Character) result[6];
            if (studentStatus != null && (studentStatus.charValue() == 'M' || studentStatus.charValue() == 'D')) {
                recalculateGradStatus = null;
            }
            // grad or non-grad
            BigDecimal gradDate = (BigDecimal) result[7];

            List<String> programCodes = new ArrayList<>();
            // optional program
            populateProgramCode((String) result[8], programCodes);
            populateProgramCode((String) result[9], programCodes);
            populateProgramCode((String) result[10], programCodes);
            populateProgramCode((String) result[11], programCodes);
            populateProgramCode((String) result[12], programCodes);

            ConvGradStudent student = new ConvGradStudent(
                    pen, null, null, null, null,
                    recalculateGradStatus != null? recalculateGradStatus.toString() : null, null,
                    schoolOfRecord, schoolAtGrad, studentGrade,
                    studentStatus != null? studentStatus.toString() : null, graduationRequestYear,
                    programCodes, !gradDate.equals(BigDecimal.ZERO));
            students.add(student);
        });

        return students;
    }

    private void populateProgramCode(String code, List<String> optionalProgramCodes) {
        if (StringUtils.isNotBlank(code)) {
            if (code.length() > 2) {
                optionalProgramCodes.add(StringUtils.substring(code,2));
            } else {
                optionalProgramCodes.add(code);
            }
        }
    }

    @Transactional(readOnly = true, transactionManager = "traxTransactionManager")
    public List<GradCourseRestriction> loadInitialRawGradCourseRestrictionsData() {
        List<GradCourseRestriction> courseRestrictions = new ArrayList<>();
        List<Object[]> results = traxStudentsLoadRepository.loadInitialCourseRestrictionRawData();
        results.forEach(result -> {
            String mainCourse = (String) result[0];
            String mainCourseLevel = (String) result[1];
            String restrictedCourse = (String) result[2];
            String restrictedCourseLevel = (String) result[3];
            String startDate = (String) result[4];
            String endDate = (String) result[5];
            GradCourseRestriction courseRestriction = new GradCourseRestriction(
                    mainCourse, mainCourseLevel, restrictedCourse, restrictedCourseLevel, startDate, endDate);
            courseRestrictions.add(courseRestriction);
        });
        return courseRestrictions;
    }

    @Transactional(readOnly = true, transactionManager = "traxTransactionManager")
    public List<GraduationCourseEntity> loadInitialGradCourseRequirementsData() {
        return graduationCourseRepository.findAll();  // .subList(0,1)
    }
}
