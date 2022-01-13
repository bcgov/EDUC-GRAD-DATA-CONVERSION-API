package ca.bc.gov.educ.api.dataconversion.service.student;

import ca.bc.gov.educ.api.dataconversion.entity.program.CareerProgramEntity;
import ca.bc.gov.educ.api.dataconversion.entity.student.*;
import ca.bc.gov.educ.api.dataconversion.model.*;
import ca.bc.gov.educ.api.dataconversion.repository.student.*;

import ca.bc.gov.educ.api.dataconversion.service.course.CourseService;
import ca.bc.gov.educ.api.dataconversion.service.program.ProgramService;
import ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiConstants;
import ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiUtils;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

import static ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiConstants.DEFAULT_CREATED_BY;
import static ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiConstants.DEFAULT_UPDATED_BY;

@Service
public class StudentService {

    private static final List<String> OPTIONAL_PROGRAM_CODES = Arrays.asList("AD", "BC", "BD");

    private final GraduationStudentRecordRepository graduationStudentRecordRepository;
    private final StudentOptionalProgramRepository studentOptionalProgramRepository;
    private final StudentCareerProgramRepository studentCareerProgramRepository;
    private final GraduationStudentRecordHistoryRepository graduationStudentRecordHistoryRepository;
    private final StudentOptionalProgramHistoryRepository studentOptionalProgramHistoryRepository;
    private final EducGradDataConversionApiConstants constants;

    private final RestUtils restUtils;
    private final CourseService courseService;
    private final ProgramService programService;

    @Autowired
    public StudentService(GraduationStudentRecordRepository graduationStudentRecordRepository,
                          StudentOptionalProgramRepository studentOptionalProgramRepository,
                          StudentCareerProgramRepository studentCareerProgramRepository,
                          GraduationStudentRecordHistoryRepository graduationStudentRecordHistoryRepository,
                          StudentOptionalProgramHistoryRepository studentOptionalProgramHistoryRepository,
                          EducGradDataConversionApiConstants constants,
                          RestUtils restUtils,
                          CourseService courseService,
                          ProgramService programService) {
        this.graduationStudentRecordRepository = graduationStudentRecordRepository;
        this.studentOptionalProgramRepository = studentOptionalProgramRepository;
        this.studentCareerProgramRepository = studentCareerProgramRepository;
        this.graduationStudentRecordHistoryRepository = graduationStudentRecordHistoryRepository;
        this.studentOptionalProgramHistoryRepository = studentOptionalProgramHistoryRepository;
        this.constants = constants;
        this.restUtils = restUtils;
        this.courseService = courseService;
        this.programService = programService;
    }

    @Transactional(transactionManager = "studentTransactionManager")
    public ConvGradStudent convertStudent(ConvGradStudent convGradStudent, ConversionStudentSummaryDTO summary) {
        summary.setProcessedCount(summary.getProcessedCount() + 1L);
        try {
            String accessToken = summary.getAccessToken();

            List<Student> students;
            try {
                // Call PEN Student API
                students = restUtils.getStudentsByPen(convGradStudent.getPen(), accessToken);
            } catch (Exception e) {
                ConversionAlert error = new ConversionAlert();
                error.setItem(convGradStudent.getPen());
                error.setReason("PEN Student API is failed: " + e.getLocalizedMessage());
                summary.getErrors().add(error);
                return null;
            }
            if (students == null || students.isEmpty()) {
                ConversionAlert error = new ConversionAlert();
                error.setItem(convGradStudent.getPen());
                error.setReason("PEN does not exist: PEN Student API returns empty response.");
                summary.getErrors().add(error);
                return null;
            }

            students.forEach(st -> {
                boolean isStudentPersisted = false;
                UUID studentID = UUID.fromString(st.getStudentID());
                Optional<GraduationStudentRecordEntity> stuOptional = graduationStudentRecordRepository.findById(studentID);
                GraduationStudentRecordEntity gradStudentEntity;
                if (stuOptional.isPresent()) {
                    gradStudentEntity = stuOptional.get();
                    gradStudentEntity.setPen(st.getPen());
                    convertStudentData(convGradStudent, gradStudentEntity, summary);
                    gradStudentEntity.setUpdateDate(null);
                    gradStudentEntity.setUpdateUser(null);
                    gradStudentEntity = graduationStudentRecordRepository.save(gradStudentEntity);
                    isStudentPersisted = true;
                    summary.setUpdatedCount(summary.getUpdatedCount() + 1L);
                } else {
                    gradStudentEntity = new GraduationStudentRecordEntity();
                    gradStudentEntity.setPen(st.getPen());
                    gradStudentEntity.setStudentID(studentID);
                    convertStudentData(convGradStudent, gradStudentEntity, summary);
                    // if year "1950" without "AD" or "AN", then program is null
                    if (StringUtils.isNotBlank(gradStudentEntity.getProgram())) {
                        gradStudentEntity = graduationStudentRecordRepository.save(gradStudentEntity);
                        summary.setAddedCount(summary.getAddedCount() + 1L);
                        isStudentPersisted = true;
                    }
                }

                if (isStudentPersisted) {
                    // graduation status history
                    createGraduationStudentRecordHistory(gradStudentEntity);

                    // student guid - pen
                    if (constants.isStudentGuidPenXrefEnabled()) {
                        saveStudentGuidPenXref(studentID, convGradStudent.getPen());
                    }

                    // process dependencies
                    gradStudentEntity.setPen(convGradStudent.getPen());
                    processSpecialPrograms(gradStudentEntity, accessToken, summary);
                    processProgramCodes(gradStudentEntity, convGradStudent.getProgramCodes(), accessToken, summary);
                    processSccpFrenchCertificates(gradStudentEntity, accessToken, summary);
                }
            });
            return convGradStudent;
        } catch (Exception e) {
            ConversionAlert error = new ConversionAlert();
            error.setItem(convGradStudent.getPen());
            error.setReason("Unexpected Exception is occurred: " + e.getLocalizedMessage());
            summary.getErrors().add(error);
            return null;
        }
    }

    public GraduationStudentRecordEntity populateGraduationStudentRecord(ConvGradStudent student) {
        GraduationStudentRecordEntity studentEntity = new GraduationStudentRecordEntity();

        // Grad Program
        if (determineProgram(student, null)) {
            studentEntity.setProgram(student.getProgram());
        }

        // Mincode Grad
        studentEntity.setSchoolAtGrad(StringUtils.isNotBlank(student.getSchoolAtGrad())? student.getSchoolAtGrad() : null);
        // Mincode
        studentEntity.setSchoolOfRecord(StringUtils.isNotBlank(student.getSchoolOfRecord())? student.getSchoolOfRecord() : null);
        // Student Grade
        studentEntity.setStudentGrade(student.getStudentGrade());
        // Student Status
        studentEntity.setStudentStatus(determineGradStudentStatus(student.getStudentStatus(), student.getArchiveFlag()));

        studentEntity.setRecalculateGradStatus(student.getRecalculateGradStatus());
        studentEntity.setRecalculateProjectedGrad(student.getRecalculateGradStatus());

        // Populate courses & assessments

        // Populate optional programs ( prgm_codeX )

        return studentEntity;
    }

    private void convertStudentData(ConvGradStudent student, GraduationStudentRecordEntity studentEntity, ConversionStudentSummaryDTO summary) {
        if (determineProgram(student, summary)) {
            studentEntity.setProgram(student.getProgram());
        }

        // for testing purpose
        // always set to NULL, even for graduated students.  => GRAD grad algorithm will be run at some point
        studentEntity.setGpa(null);
        studentEntity.setHonoursStanding(null);
        if (studentEntity.getProgram().equals("SCCP")) {
            studentEntity.setProgramCompletionDate(EducGradDataConversionApiUtils.parseDate(student.getSlpDate(), EducGradDataConversionApiConstants.TRAX_SLP_DATE_FORMAT));
        } else {
            studentEntity.setProgramCompletionDate(null);
        }
        studentEntity.setStudentGradData(null);
        studentEntity.setSchoolAtGrad(null);

        studentEntity.setSchoolOfRecord(StringUtils.isNotBlank(student.getSchoolOfRecord())? student.getSchoolOfRecord() : null);
        studentEntity.setRecalculateGradStatus(student.getRecalculateGradStatus());
        studentEntity.setRecalculateProjectedGrad(student.getRecalculateGradStatus());
        studentEntity.setStudentGrade(student.getStudentGrade());
        studentEntity.setStudentStatus(determineGradStudentStatus(student.getStudentStatus(), student.getArchiveFlag()));

        // Mappings with Student_Master
        studentEntity.setFrenchCert(student.getFrenchCert());
    }

    private String determineGradStudentStatus(String traxStudentStatus, String traxArchiveFlag) {
        if (StringUtils.equalsIgnoreCase(traxStudentStatus, "A") && StringUtils.equalsIgnoreCase(traxArchiveFlag, "A")) {
            return "CUR";
        } else if (StringUtils.equalsIgnoreCase(traxStudentStatus, "A") && StringUtils.equalsIgnoreCase(traxArchiveFlag, "I")) {
            return "ARC";
        } else if (StringUtils.equalsIgnoreCase(traxStudentStatus, "D")) {
            return "DEC";
        } else if (StringUtils.equalsIgnoreCase(traxStudentStatus, "M")) {
            return "MER";
        } else if (StringUtils.equalsIgnoreCase(traxStudentStatus, "T") &&
                (StringUtils.equalsIgnoreCase(traxArchiveFlag, "A") || StringUtils.equalsIgnoreCase(traxArchiveFlag, "I")) ) {
            return "TER";
        }
        return null;
    }

    private void processSpecialPrograms(GraduationStudentRecordEntity student, String accessToken, ConversionStudentSummaryDTO summary) {
        if (StringUtils.isBlank(student.getProgram())) {
            return;
        }

        // Dual Dogwood for yyyy-PF
        if (student.getProgram().endsWith("-PF")) {
            createStudentOptionalProgram("DD", student, accessToken, summary);
        }

        // French Immersion for 2018-EN, 2004-EN
        if (student.getProgram().equals("2018-EN") || student.getProgram().equals("2004-EN")) {
            if (courseService.isFrenchImmersionCourse(student.getPen(), "10")) {
                createStudentOptionalProgram("FI", student, accessToken, summary);
            }
        } else if (student.getProgram().equals("1996-EN")) {
            if (courseService.isFrenchImmersionCourse(student.getPen(), "11")) {
                createStudentOptionalProgram("FI", student, accessToken, summary);
            }
        } else if (student.getProgram().equals("1986-EN")) {
            if (StringUtils.equalsIgnoreCase("F", student.getFrenchCert())) {
                createStudentOptionalProgram("FI", student, accessToken, summary);
            } else if (courseService.isFrenchImmersionCourseForEN(student.getPen(), "11")) {
                createStudentOptionalProgram("FI", student, accessToken, summary);
            }
        }
    }

    private void processProgramCodes(GraduationStudentRecordEntity student, List<String> programCodes, String accessToken, ConversionStudentSummaryDTO summary) {
        if (StringUtils.isNotBlank(student.getProgram()) && !programCodes.isEmpty()) {
            for (String programCode : programCodes) {
                if (isOptionalProgramCode(programCode)) {
                    createStudentOptionalProgram(programCode, student, accessToken, summary);
                } else if (createStudentCareerProgram(programCode, student, summary)) {
                    createStudentOptionalProgram("CP", student, accessToken, summary);
                }
            }
        }
    }

    private void processSccpFrenchCertificates(GraduationStudentRecordEntity student, String accessToken, ConversionStudentSummaryDTO summary) {
        if (StringUtils.equals(student.getProgram(), "SCCP")
            && ( StringUtils.isNotBlank(student.getSchoolOfRecord())
                 && student.getSchoolOfRecord().startsWith("093") )
        ) {
            createStudentOptionalProgram("FR", student, accessToken, summary);
        }
    }

    private boolean isOptionalProgramCode(String code) {
       return OPTIONAL_PROGRAM_CODES.contains(code);
    }

    private boolean createStudentOptionalProgram(String optionalProgramCode, GraduationStudentRecordEntity student, String accessToken, ConversionStudentSummaryDTO summary) {
        StudentOptionalProgramEntity entity = new StudentOptionalProgramEntity();
        entity.setPen(student.getPen());
        entity.setStudentID(student.getStudentID());

        GradSpecialProgram gradSpecialProgram;
        // Call Grad Program API
        try {
            gradSpecialProgram = restUtils.getGradSpecialProgram(student.getProgram(), optionalProgramCode, accessToken);
        } catch (Exception e) {
            ConversionAlert error = new ConversionAlert();
            error.setLevel(ConversionAlert.AlertLevelEnum.WARNING);
            error.setItem(student.getPen());
            error.setReason("Grad Program API is failed: " + e.getLocalizedMessage());
            summary.getErrors().add(error);
            return false;
        }
        if (gradSpecialProgram != null && gradSpecialProgram.getOptionalProgramID() != null) {
            entity.setOptionalProgramID(gradSpecialProgram.getOptionalProgramID());
            Optional<StudentOptionalProgramEntity> stdSpecialProgramOptional = studentOptionalProgramRepository.findByStudentIDAndOptionalProgramID(student.getStudentID(), gradSpecialProgram.getOptionalProgramID());
            if (stdSpecialProgramOptional.isPresent()) {
                StudentOptionalProgramEntity currentEntity = stdSpecialProgramOptional.get();
                currentEntity.setUpdateDate(null);
                currentEntity.setUpdateUser(null);
                studentOptionalProgramRepository.save(currentEntity); // touch: update_user & update_date will be updated only.
                createStudentOptionalProgramHistory(currentEntity); // student optional program history
            } else {
                entity.setId(UUID.randomUUID());
                studentOptionalProgramRepository.save(entity);
                createStudentOptionalProgramHistory(entity); // student optional program history
            }
            summary.incrementOptionalProgram(optionalProgramCode);
        }
        return true;
    }

    private boolean createStudentCareerProgram(String careerProgramCode, GraduationStudentRecordEntity student, ConversionStudentSummaryDTO summary) {
        StudentCareerProgramEntity entity = new StudentCareerProgramEntity();
        entity.setStudentID(student.getStudentID());

        CareerProgramEntity cpEntity = programService.getCareerProgramCode(careerProgramCode);
        if (cpEntity != null) {
            entity.setCareerProgramCode(careerProgramCode);
            Optional<StudentCareerProgramEntity> stdCareerProgramOptional = studentCareerProgramRepository.findByStudentIDAndCareerProgramCode(student.getStudentID(), careerProgramCode);
            if (stdCareerProgramOptional.isPresent()) {
                StudentCareerProgramEntity currentEntity = stdCareerProgramOptional.get();
                currentEntity.setUpdateDate(null);
                currentEntity.setUpdateUser(null);
                studentCareerProgramRepository.save(currentEntity);  // touch: update_user will be updated only.
            } else {
                entity.setId(UUID.randomUUID());
                studentCareerProgramRepository.save(entity);
            }
            summary.incrementCareerProgram(careerProgramCode);
            return true;
        } else {
            ConversionAlert error = new ConversionAlert();
            error.setLevel(ConversionAlert.AlertLevelEnum.WARNING);
            error.setItem(student.getPen());
            error.setReason("Career Program Code does not exist: " + careerProgramCode);
            summary.getErrors().add(error);
            return false;
        }
    }

    private boolean determineProgram(ConvGradStudent student, ConversionStudentSummaryDTO summary) {
        switch(student.getGraduationRequestYear()) {
            case "2018":
                if (student.getSchoolOfRecord().startsWith("093")) {
                    student.setProgram("2018-PF");
                    updateProgramCountsInSummary(summary, "2018-PF", student.isGraduated());
                } else {
                    student.setProgram("2018-EN");
                    updateProgramCountsInSummary(summary, "2018-EN", student.isGraduated());
                }
                break;
            case "2004":
                if (student.getSchoolOfRecord().startsWith("093")) {
                    student.setProgram("2004-PF");
                    updateProgramCountsInSummary(summary, "2004-PF", student.isGraduated());
                } else {
                    student.setProgram("2004-EN");
                    updateProgramCountsInSummary(summary, "2004-EN", student.isGraduated());
                }
                break;
            case "1996":
                if (student.getSchoolOfRecord().startsWith("093")) {
                    student.setProgram("1996-PF");
                    updateProgramCountsInSummary(summary, "1996-PF", student.isGraduated());
                } else {
                    student.setProgram("1996-EN");
                    updateProgramCountsInSummary(summary, "1996-EN", student.isGraduated());
                }
                break;
            case "1986":
                student.setProgram("1986-EN");
                updateProgramCountsInSummary(summary, "1986-EN", student.isGraduated());
                break;
            case "1950":
                student.setProgram("1950");
                summary.increment("1950", student.isGraduated());
                break;
            case "SCCP":
                student.setProgram("SCCP");
                updateProgramCountsInSummary(summary, "SCCP", false);
                break;
            default:
                // error
                ConversionAlert error = new ConversionAlert();
                error.setItem(student.getPen());
                error.setReason("Program is not found for year " + student.getGraduationRequestYear() + " / grade " + student.getStudentGrade());
                summary.getErrors().add(error);
                return false;
        }
        return true;
    }

    private void updateProgramCountsInSummary(ConversionStudentSummaryDTO summary, String programCode, boolean isGraduated) {
        if (summary != null) {
            summary.increment(programCode, isGraduated);
        }
    }

    private void createGraduationStudentRecordHistory(GraduationStudentRecordEntity grauationStudentRecord) {
        final GraduationStudentRecordHistoryEntity graduationStudentRecordHistoryEntity = new GraduationStudentRecordHistoryEntity();
        BeanUtils.copyProperties(grauationStudentRecord, graduationStudentRecordHistoryEntity);
        graduationStudentRecordHistoryEntity.setActivityCode("DATACONVERT");
        graduationStudentRecordHistoryRepository.save(graduationStudentRecordHistoryEntity);
    }

    private void createStudentOptionalProgramHistory(StudentOptionalProgramEntity studentOptionalProgramEntity) {
        final StudentOptionalProgramHistoryEntity studentOptionalProgramHistoryEntity = new StudentOptionalProgramHistoryEntity();
        BeanUtils.copyProperties(studentOptionalProgramEntity, studentOptionalProgramHistoryEntity);
        studentOptionalProgramHistoryEntity.setStudentOptionalProgramID(studentOptionalProgramEntity.getId());
        studentOptionalProgramHistoryEntity.setActivityCode("DATACONVERT");
        studentOptionalProgramHistoryRepository.save(studentOptionalProgramHistoryEntity);
    }

    @Transactional(transactionManager = "studentTransactionManager")
    public void saveStudentGuidPenXref(UUID studentId, String pen) {
        if (graduationStudentRecordRepository.countStudentGuidPenXrefRecord(studentId) > 0) {
            graduationStudentRecordRepository.updateStudentGuidPenXrefRecord(studentId, pen, DEFAULT_UPDATED_BY, LocalDateTime.now());
        } else {
            graduationStudentRecordRepository.createStudentGuidPenXrefRecord(studentId, pen, DEFAULT_CREATED_BY, LocalDateTime.now());
        }
    }

}
