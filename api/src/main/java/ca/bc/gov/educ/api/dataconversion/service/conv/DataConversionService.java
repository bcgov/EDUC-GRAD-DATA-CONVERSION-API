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
            Character archiveFlag = (Character) result[5];
            String graduationRequestYear = (String) result[6];

            Character recalculateGradStatus = (Character) result[7];
            if (studentStatus != null && (studentStatus.charValue() == 'M' || studentStatus.charValue() == 'D')) {
                recalculateGradStatus = null;
            }
            // grad or non-grad
            BigDecimal gradDate = (BigDecimal) result[8];
            boolean isGraduated = gradDate != null && !gradDate.equals(BigDecimal.ZERO);

            List<String> programCodes = new ArrayList<>();
            // optional program
            populateProgramCode((String) result[9], programCodes);
            populateProgramCode((String) result[10], programCodes);
            populateProgramCode((String) result[11], programCodes);
            populateProgramCode((String) result[12], programCodes);
            populateProgramCode((String) result[13], programCodes);

            // slp date
            BigDecimal slpDate = (BigDecimal) result[14];
            String slpDateStr = slpDate != null && !slpDate.equals(BigDecimal.ZERO)? slpDate.toString() : null;

            // french cert
            String frenchCert = (String) result[15];

            try {
                ConvGradStudent student = new ConvGradStudent(
                        pen, null, null, slpDateStr, null, null,
                        recalculateGradStatus != null ? recalculateGradStatus.toString() : null, null,
                        schoolOfRecord, schoolAtGrad, studentGrade,
                        studentStatus != null ? studentStatus.toString() : null,
                        archiveFlag != null ? archiveFlag.toString() : null,
                        StringUtils.isNotBlank(frenchCert)? frenchCert.trim() : null,
                        graduationRequestYear, programCodes, isGraduated);
                students.add(student);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
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

            // check null value for course level and convert it to space
            if (StringUtils.isBlank(mainCourseLevel)) {
                mainCourseLevel = " ";
            }
            if (StringUtils.isBlank(restrictedCourseLevel)) {
                restrictedCourseLevel = " ";
            }
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
