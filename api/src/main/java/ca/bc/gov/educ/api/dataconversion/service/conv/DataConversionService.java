package ca.bc.gov.educ.api.dataconversion.service.conv;

import ca.bc.gov.educ.api.dataconversion.entity.trax.GraduationCourseEntity;
import ca.bc.gov.educ.api.dataconversion.model.ConvGradStudent;
import ca.bc.gov.educ.api.dataconversion.model.GradCourseRestriction;
import ca.bc.gov.educ.api.dataconversion.repository.trax.GraduationCourseRepository;
import ca.bc.gov.educ.api.dataconversion.repository.trax.TraxStudentsLoadRepository;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@Slf4j
public class DataConversionService {
    private final TraxStudentsLoadRepository traxStudentsLoadRepository;
    private final GraduationCourseRepository graduationCourseRepository;
    private final RestUtils restUtils;

    @Autowired
    public DataConversionService(TraxStudentsLoadRepository traxStudentsLoadRepository,
                                 GraduationCourseRepository graduationCourseRepository,
                                 RestUtils restUtils) {
        this.traxStudentsLoadRepository = traxStudentsLoadRepository;
        this.graduationCourseRepository = graduationCourseRepository;
        this.restUtils = restUtils;
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
    public List<ConvGradStudent> loadAllTraxStudentData() {
        List<ConvGradStudent> students = new ArrayList<>();
        List<Object[]> results = traxStudentsLoadRepository.loadAlTraxStudents();
        results.forEach(result -> {
            ConvGradStudent student = new ConvGradStudent();
            String pen = (String) result[0];
            student.setPen(pen);
            students.add(student);
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
        String slpDateStr = slpDate != null && !slpDate.equals(BigDecimal.ZERO) ? slpDate.toString() : null;

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
                    StringUtils.isNotBlank(frenchCert) ? frenchCert.trim() : null,
                    graduationRequestYear, programCodes, isGraduated);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return student;
    }

    @Transactional(readOnly = true, transactionManager = "traxTransactionManager")
    public List<Student> getStudentDemographicsDataFromTrax(String pen) {
        List<Student> students = new ArrayList<>();
        List<Object[]> results = traxStudentsLoadRepository.loadStudentDemographicsData(pen);
        results.forEach(result -> {
            String legalFirstName = (String) result[1];
            legalFirstName = StringUtils.isNotBlank(legalFirstName)? legalFirstName.trim() : null;
            String legalLastName = (String) result[2];
            legalLastName = StringUtils.isNotBlank(legalLastName)? legalLastName.trim() : null;
            String legalMiddleName = (String) result[3];
            legalMiddleName = StringUtils.isNotBlank(legalMiddleName)? legalMiddleName.trim() : null;

            Character studStatus = (Character) result[4];
            String studentStatusCode = studStatus != null? studStatus.toString() : null;
            if (StringUtils.equals(studentStatusCode, "T")) {
                studentStatusCode = "A";
            }
            log.debug(" TRAX - PEN mapping : stud_status [{}] => status code [{}]", studStatus,  studentStatusCode);

            String schoolOfRecord = (String) result[6];
            String studGrade = (String) result[7];
            String studentGrade;
            if (!NumberUtils.isCreatable(studGrade)) {
                studentGrade = "11";
            } else {
                studentGrade = studGrade;
            }
            log.debug(" TRAX - PEN mapping : stud_grade [{}] => grade code [{}]", studGrade,  studentGrade);
            String postal = (String) result[8];
            Character sexCode = (Character) result[9];
            String birthDate = (String) result[10];
            String formattedBirthDate = birthDate.substring(0, 4) + "-" + birthDate.substring(4, 6) + "-" + birthDate.substring(6, 8);

            String truePen = (String) result[12];
            truePen = StringUtils.isNotBlank(truePen)? truePen.trim() : null;

            String localID = (String) result[13];

            Student student = Student.builder()
                    .pen(pen)
                    .legalFirstName(legalFirstName)
                    .legalLastName(legalLastName)
                    .legalMiddleNames(legalMiddleName)
                    .usualFirstName(legalFirstName)
                    .usualLastName(legalLastName)
                    .usualMiddleNames(legalMiddleName)
                    .statusCode(studentStatusCode)
                    .genderCode(sexCode.toString())
                    .sexCode(sexCode.toString())
                    .mincode(schoolOfRecord)
                    .postalCode(postal)
                    .dob(formattedBirthDate)
                    .gradeCode(studentGrade)
                    .emailVerified("Y")
                    .truePen(truePen)
                    .localID(localID)
                    .build();
            students.add(student);
        });
        return students;
    }

    @Transactional(transactionManager = "traxTransactionManager")
    public ConvGradStudent readTraxStudentAndAddNewPen(ConvGradStudent convGradStudent, ConversionStudentSummaryDTO summary) {
        summary.setProcessedCount(summary.getProcessedCount() + 1L);
        try {
            String accessToken = summary.getAccessToken();
            Student penStudent = getPenStudent(convGradStudent.getPen(), accessToken, summary);
            if (penStudent == null) {
                Student traxStudent = readTraxStudent(convGradStudent.getPen());
                if (traxStudent != null) {
                    if (StringUtils.equals(traxStudent.getStatusCode(), "M") && StringUtils.isNotBlank(traxStudent.getTruePen())) {
                        log.info("Merged student is skipped: pen# {}", traxStudent.getPen());
                        return convGradStudent;
//                        // MergedToStudent
//                        Student penMergedToStudent = getPenStudent(traxStudent.getTruePen(), accessToken, summary);
//                        if (penMergedToStudent == null) {
//                            // Create MergedToStudent
//                            penMergedToStudent = readTraxStudent(traxStudent.getTruePen());
//                            if (penMergedToStudent != null) {
//                                penMergedToStudent.setDemogCode("A");
//                                penMergedToStudent = createNewPen(penMergedToStudent, accessToken, summary);
//                            }
//                        }
//                        // TrueStudentID
//                        traxStudent.setTrueStudentID(penMergedToStudent != null? penMergedToStudent.getStudentID() : null);
//                        traxStudent.setDemogCode("A");
                    }
                    // MergedFromStudent
                    createNewPen(traxStudent, accessToken, summary);
                }
            } else {
                log.info("Student already exists : pen# {} => studentID {}", convGradStudent.getPen(), penStudent.getStudentID());
            }
            return convGradStudent;
        } catch (Exception e) {
            ConversionAlert error = new ConversionAlert();
            error.setItem(convGradStudent.getPen());
            error.setReason("Unexpected Exception is occurred: " + e.getLocalizedMessage());
            summary.getErrors().add(error);
            return null;
        }
    }

    private Student readTraxStudent(String pen) {
        List<Student> students = getStudentDemographicsDataFromTrax(pen);
        if (students != null && !students.isEmpty()) {
            return students.get(0);
        }
        return null;
    }

    private Student getPenStudent(String pen, String accessToken, ConversionStudentSummaryDTO summary) {
        Student student = null;
        try {
            // Call PEN Student API
            List<Student> students = restUtils.getStudentsByPen(pen, accessToken);
            if (students != null && !students.isEmpty()) {
                student = students.get(0);
            }
        } catch (Exception e) {
            ConversionAlert error = new ConversionAlert();
            error.setItem(pen);
            error.setReason("PEN Student API is failed: " + e.getLocalizedMessage());
            summary.getErrors().add(error);
        }
        return student;
    }

    private Student createNewPen(Student student, String accessToken, ConversionStudentSummaryDTO summary) {
        if (StringUtils.isBlank(student.getHistoryActivityCode())) {
            student.setHistoryActivityCode("REQNEW");
        }
        if (StringUtils.isBlank(student.getDemogCode())) {
            student.setDemogCode("A");
        }
        Student newStudent = restUtils.addNewPen(student, accessToken);
        if (newStudent != null) {
            log.info("Add missing student: pen# {} => studentID {}", student.getPen(), newStudent.getStudentID());
            summary.setAddedCount(summary.getAddedCount() + 1L);
        }
        return newStudent;
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
