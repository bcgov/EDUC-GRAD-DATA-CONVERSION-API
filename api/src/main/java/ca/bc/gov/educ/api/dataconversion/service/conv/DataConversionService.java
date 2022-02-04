package ca.bc.gov.educ.api.dataconversion.service.conv;

import ca.bc.gov.educ.api.dataconversion.entity.trax.GraduationCourseEntity;
import ca.bc.gov.educ.api.dataconversion.model.ConvGradStudent;
import ca.bc.gov.educ.api.dataconversion.model.GradCourseRestriction;
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
    public List<ConvGradStudent> loadGradStudentsDataFromTrax() {
        List<ConvGradStudent> students = new ArrayList<>();
        List<Object[]> results = traxStudentsLoadRepository.loadAllTraxStudents();
        results.forEach(result -> {
            ConvGradStudent student = populateConvGradStudent(result);
            if (student != null) {
                students.add(student);
            }
        });

        return students;
    }

    @Transactional(readOnly = true, transactionManager = "traxTransactionManager")
    public List<ConvGradStudent> loadGradStudentDataFromTrax(String pen) {
        List<ConvGradStudent> students = new ArrayList<>();
        List<Object[]> results = traxStudentsLoadRepository.loadTraxStudent(pen);
        results.forEach(result -> {
            ConvGradStudent student = populateConvGradStudent(result);
            if (student != null) {
                students.add(student);
            }
        });

        return students;
    }

    private ConvGradStudent populateConvGradStudent(Object[] fields) {
        String pen = (String) fields[0];
        String schoolOfRecord = (String) fields[1];
        String schoolAtGrad = (String) fields[2];
        String studentGrade = (String) fields[3];
        Character studentStatus = (Character) fields[4];
        Character archiveFlag = (Character) fields[5];
        String graduationRequestYear = (String) fields[6];

        Character recalculateGradStatus = (Character) fields[7];
        if (studentStatus != null && (studentStatus.charValue() == 'M' || studentStatus.charValue() == 'D')) {
            recalculateGradStatus = null;
        }
        // grad or non-grad
        BigDecimal gradDate = (BigDecimal) fields[8];
        boolean isGraduated = gradDate != null && !gradDate.equals(BigDecimal.ZERO);

        List<String> programCodes = new ArrayList<>();
        // student optional programs
        populateProgramCode((String) fields[9], programCodes);
        populateProgramCode((String) fields[10], programCodes);
        populateProgramCode((String) fields[11], programCodes);
        populateProgramCode((String) fields[12], programCodes);
        populateProgramCode((String) fields[13], programCodes);

        // slp date
        BigDecimal slpDate = (BigDecimal) fields[14];
        String slpDateStr = slpDate != null && !slpDate.equals(BigDecimal.ZERO)? slpDate.toString() : null;

        // french cert
        String frenchCert = (String) fields[15];

        ConvGradStudent student = null;
        try {
            student = new ConvGradStudent(
                    pen, null, null, slpDateStr, null, null,
                    recalculateGradStatus != null ? recalculateGradStatus.toString() : null, null,
                    schoolOfRecord, schoolAtGrad, studentGrade,
                    studentStatus != null ? studentStatus.toString() : null,
                    archiveFlag != null ? archiveFlag.toString() : null,
                    StringUtils.isNotBlank(frenchCert)? frenchCert.trim() : null,
                    graduationRequestYear, programCodes, isGraduated);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return student;
    }

    // student optional program
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
    public List<GradCourseRestriction> loadGradCourseRestrictionsDataFromTrax() {
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
    public List<GraduationCourseEntity> loadGradCourseRequirementsDataFromTrax() {
        return graduationCourseRepository.findAll();  // .subList(0,1)
    }
}
