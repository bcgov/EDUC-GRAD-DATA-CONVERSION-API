package ca.bc.gov.educ.api.dataconversion.service.student;

import ca.bc.gov.educ.api.dataconversion.entity.assessment.StudentAssessmentEntity;
import ca.bc.gov.educ.api.dataconversion.entity.course.StudentCourseEntity;
import ca.bc.gov.educ.api.dataconversion.entity.program.CareerProgramEntity;
import ca.bc.gov.educ.api.dataconversion.entity.program.OptionalProgramEntity;
import ca.bc.gov.educ.api.dataconversion.entity.student.*;
import ca.bc.gov.educ.api.dataconversion.model.*;
import ca.bc.gov.educ.api.dataconversion.repository.student.*;

import ca.bc.gov.educ.api.dataconversion.service.assessment.AssessmentService;
import ca.bc.gov.educ.api.dataconversion.service.course.CourseService;
import ca.bc.gov.educ.api.dataconversion.service.program.ProgramService;
import ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiConstants;
import ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiUtils;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.*;

import static ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiConstants.DEFAULT_CREATED_BY;
import static ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiConstants.DEFAULT_UPDATED_BY;

@Service
@Slf4j
public class StudentService extends StudentBaseService {

    private static final List<String> OPTIONAL_PROGRAM_CODES = Arrays.asList("AD", "BC", "BD");

    private final GraduationStudentRecordRepository graduationStudentRecordRepository;
    private final StudentOptionalProgramRepository studentOptionalProgramRepository;
    private final StudentCareerProgramRepository studentCareerProgramRepository;
    private final GraduationStudentRecordHistoryRepository graduationStudentRecordHistoryRepository;
    private final StudentOptionalProgramHistoryRepository studentOptionalProgramHistoryRepository;
    private final EducGradDataConversionApiConstants constants;

    private final RestUtils restUtils;
    private final AssessmentService assessmentService;
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
                          AssessmentService assessmentService,
                          CourseService courseService,
                          ProgramService programService) {
        this.graduationStudentRecordRepository = graduationStudentRecordRepository;
        this.studentOptionalProgramRepository = studentOptionalProgramRepository;
        this.studentCareerProgramRepository = studentCareerProgramRepository;
        this.graduationStudentRecordHistoryRepository = graduationStudentRecordHistoryRepository;
        this.studentOptionalProgramHistoryRepository = studentOptionalProgramHistoryRepository;
        this.constants = constants;
        this.restUtils = restUtils;
        this.assessmentService = assessmentService;
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
                    createGraduationStudentRecordHistory(gradStudentEntity, "DATACONVERT");

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
        studentEntity.setStudentStatus(getGradStudentStatus(student.getStudentStatus(), student.getArchiveFlag()));

        // Mappings with Student_Master
        studentEntity.setFrenchCert(student.getFrenchCert());
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
        if (hasAnyFrenchImmersionCourse(student.getProgram(), student.getPen(), student.getFrenchCert())) {
            createStudentOptionalProgram("FI", student, accessToken, summary);
        }
//        if (student.getProgram().equals("2018-EN") || student.getProgram().equals("2004-EN")) {
//            if (courseService.isFrenchImmersionCourse(student.getPen(), "10")) {
//                createStudentOptionalProgram("FI", student, accessToken, summary);
//            }
//        } else if (student.getProgram().equals("1996-EN")) {
//            if (courseService.isFrenchImmersionCourse(student.getPen(), "11")) {
//                createStudentOptionalProgram("FI", student, accessToken, summary);
//            }
//        } else if (student.getProgram().equals("1986-EN")) {
//            if (StringUtils.equalsIgnoreCase("F", student.getFrenchCert())) {
//                createStudentOptionalProgram("FI", student, accessToken, summary);
//            } else if (courseService.isFrenchImmersionCourseForEN(student.getPen(), "11")) {
//                createStudentOptionalProgram("FI", student, accessToken, summary);
//            }
//        }
    }

    protected boolean hasAnyFrenchImmersionCourse(String program, String pen, String frenchCert) {
        boolean frenchImmersion = false;
        // French Immersion for 2018-EN, 2004-EN
        if (program.equals("2018-EN") || program.equals("2004-EN")) {
            if (courseService.isFrenchImmersionCourse(pen, "10")) {
                frenchImmersion = true;
            }
        } else if (program.equals("1996-EN")) {
            if (courseService.isFrenchImmersionCourse(pen, "11")) {
                frenchImmersion = true;
            }
        } else if (program.equals("1986-EN")) {
            if (StringUtils.equalsIgnoreCase("F", frenchCert)) {
                frenchImmersion = true;
            } else if (courseService.isFrenchImmersionCourseForEN(pen, "11")) {
                frenchImmersion = true;
            }
        }
        return frenchImmersion;
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
                createStudentOptionalProgramHistory(currentEntity, "DATACONVERT"); // student optional program history
            } else {
                entity.setId(UUID.randomUUID());
                studentOptionalProgramRepository.save(entity);
                createStudentOptionalProgramHistory(entity, "DATACONVERT"); // student optional program history
            }
            summary.incrementOptionalProgram(optionalProgramCode);
        }
        return true;
    }

    private boolean createStudentCareerProgram(String careerProgramCode, GraduationStudentRecordEntity student, ConversionStudentSummaryDTO summary) {
        StudentCareerProgramEntity entity = new StudentCareerProgramEntity();
        entity.setStudentID(student.getStudentID());

        CareerProgramEntity cpEntity = programService.getCareerProgram(careerProgramCode);
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

    private void createGraduationStudentRecordHistory(GraduationStudentRecordEntity grauationStudentRecord, String activityCode) {
        final GraduationStudentRecordHistoryEntity graduationStudentRecordHistoryEntity = new GraduationStudentRecordHistoryEntity();
        BeanUtils.copyProperties(grauationStudentRecord, graduationStudentRecordHistoryEntity);
        graduationStudentRecordHistoryEntity.setActivityCode(activityCode);
        graduationStudentRecordHistoryRepository.save(graduationStudentRecordHistoryEntity);
    }

    private void createStudentOptionalProgramHistory(StudentOptionalProgramEntity studentOptionalProgramEntity, String activityCode) {
        final StudentOptionalProgramHistoryEntity studentOptionalProgramHistoryEntity = new StudentOptionalProgramHistoryEntity();
        BeanUtils.copyProperties(studentOptionalProgramEntity, studentOptionalProgramHistoryEntity);
        studentOptionalProgramHistoryEntity.setStudentOptionalProgramID(studentOptionalProgramEntity.getId());
        studentOptionalProgramHistoryEntity.setActivityCode(activityCode);
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

    @Transactional(transactionManager = "studentTransactionManager", readOnly = true)
    public StudentGradDTO loadStudentData(String pen) {
        byte[] rawGUID = graduationStudentRecordRepository.findStudentID(pen);
        if (rawGUID == null) {
            return null;
        }
        ByteBuffer bb = ByteBuffer.wrap(rawGUID);
        UUID studentID = new UUID(bb.getLong(), bb.getLong());
        StudentGradDTO studentData = new StudentGradDTO();
        studentData.setStudentID(studentID);
        Optional<GraduationStudentRecordEntity> gradStatusOptional = graduationStudentRecordRepository.findById(studentID);
        if (gradStatusOptional.isPresent()) {
            GraduationStudentRecordEntity entity = gradStatusOptional.get();
            studentData.setProgram(entity.getProgram());
            studentData.setStudentGrade(entity.getStudentGrade());
            studentData.setStudentStatus(entity.getStudentStatus());
            studentData.setSchoolOfRecord(entity.getSchoolOfRecord());
            studentData.setSchoolAtGrad(entity.getSchoolAtGrad());
        } else {
            return null;
        }

        // optional programs
        List<StudentOptionalProgramEntity> optionalPrograms = studentOptionalProgramRepository.findByStudentID(studentID);
        studentData.getProgramCodes().addAll(getOptionalProgramCodes(optionalPrograms));

        // career programs
        List<StudentCareerProgramEntity> careerPrograms = studentCareerProgramRepository.findByStudentID(studentID);
        studentData.getProgramCodes().addAll(getCareerProgramCodes(careerPrograms));

        // courses
        List<StudentCourseEntity> courses = courseService.getStudentCourses(pen);
        studentData.getCourses().addAll(courses);
        // assessments
        List<StudentAssessmentEntity> assessments = assessmentService.getStudentAssessments(pen);
        studentData.getAssessments().addAll(assessments);

        return studentData;
    }

    private List<String> getOptionalProgramCodes(List<StudentOptionalProgramEntity> studentOptionalProgramEntities) {
        List<String> codes = new ArrayList<>();
        studentOptionalProgramEntities.forEach(e -> {
            OptionalProgramEntity ope = programService.findOptionalProgram(e.getOptionalProgramID());
            if (ope != null) {
                codes.add(ope.getOptProgramCode());
            }
        });
        return codes;
    }

    private List<String> getCareerProgramCodes(List<StudentCareerProgramEntity> studentCareerProgramEntities) {
        List<String> codes = new ArrayList<>();
        studentCareerProgramEntities.forEach(e -> {
            codes.add(e.getCareerProgramCode());
        });
        return codes;
    }

    @Transactional(transactionManager = "studentTransactionManager")
    public void saveGraduationStudent(StudentGradDTO gradStudent) {
        Optional<GraduationStudentRecordEntity> gradStatusOptional = graduationStudentRecordRepository.findById(gradStudent.getStudentID());
        if (gradStatusOptional.isPresent()) {
            GraduationStudentRecordEntity entity = gradStatusOptional.get();
            if (StringUtils.isNotBlank(gradStudent.getNewProgram())) {
                entity.setProgram(gradStudent.getNewProgram());
            }
            if (StringUtils.isNotBlank(gradStudent.getNewStudentGrade())) {
                entity.setStudentGrade(gradStudent.getNewStudentGrade());
            }
            if (StringUtils.isNotBlank(gradStudent.getNewStudentStatus())) {
                entity.setStudentStatus(gradStudent.getNewStudentStatus());
            }
            if (StringUtils.isNotBlank(gradStudent.getNewSchoolOfRecord())) {
                entity.setSchoolOfRecord(gradStudent.getNewSchoolOfRecord());
            }
            if (StringUtils.isNotBlank(gradStudent.getNewSchoolAtGrad())) {
                entity.setSchoolAtGrad(gradStudent.getNewSchoolAtGrad());
            }

            graduationStudentRecordRepository.save(entity);
            // graduation student record history
            createGraduationStudentRecordHistory(entity, "TRAXUPDATE");
        }

        if (gradStudent.isAddDualDogwood()) {
            log.info(" => [DD] optional program will be added if not exist.");
            addStudentOptionalProgram("DD", gradStudent);
        } else if (gradStudent.isDeleteDualDogwood()) {
            log.info(" => [DD] optional program will be removed if exist.");
            removeStudentOptionalProgram("DD", gradStudent);
        }
    }

    @Transactional(transactionManager = "studentTransactionManager")
    public void removeStudentOptionalProgram(String optionalProgramCode, StudentGradDTO gradStudent) {
        OptionalProgramEntity optionalProgramEntity = programService.getOptionalProgram(gradStudent.getProgram(), optionalProgramCode);
        if (optionalProgramEntity != null) {
            Optional<StudentOptionalProgramEntity> optional = studentOptionalProgramRepository.findByStudentIDAndOptionalProgramID(gradStudent.getStudentID(), optionalProgramEntity.getOptionalProgramID());
            if (optional.isPresent()) {
                StudentOptionalProgramEntity entity = optional.get();
                createStudentOptionalProgramHistory(entity, "TRAXDELETE");
                studentOptionalProgramRepository.delete(entity);
            }
        }
    }

    @Transactional(transactionManager = "studentTransactionManager")
    public void addStudentOptionalProgram(String optionalProgramCode, StudentGradDTO gradStudent) {
        OptionalProgramEntity optionalProgramEntity = programService.getOptionalProgram(gradStudent.getProgram(), optionalProgramCode);
        if (optionalProgramEntity != null) {
            Optional<StudentOptionalProgramEntity> optional = studentOptionalProgramRepository.findByStudentIDAndOptionalProgramID(gradStudent.getStudentID(), optionalProgramEntity.getOptionalProgramID());
            if (!optional.isPresent()) {
                StudentOptionalProgramEntity entity = new StudentOptionalProgramEntity();
                entity.setId(UUID.randomUUID());
                entity.setStudentID(gradStudent.getStudentID());
                entity.setOptionalProgramID(optionalProgramEntity.getOptionalProgramID());
                studentOptionalProgramRepository.save(entity);
                createStudentOptionalProgramHistory(entity, "TRAXADD");
            }
        }
    }

    @Transactional(transactionManager = "studentTransactionManager")
    public void removeStudentCareerProgram(String careerProgramCode, StudentGradDTO gradStudent) {
        CareerProgramEntity careerProgramEntity = programService.getCareerProgram(careerProgramCode);
        if (careerProgramEntity != null) {
            Optional<StudentCareerProgramEntity> optional = studentCareerProgramRepository.findByStudentIDAndCareerProgramCode(gradStudent.getStudentID(), careerProgramEntity.getCode());
            if (optional.isPresent()) {
                StudentCareerProgramEntity entity = optional.get();
                studentCareerProgramRepository.delete(entity);
            }
        }
    }

    @Transactional(transactionManager = "studentTransactionManager")
    public void addStudentCareerProgram(String careerProgramCode, StudentGradDTO gradStudent) {
        Optional<StudentCareerProgramEntity> optional = studentCareerProgramRepository.findByStudentIDAndCareerProgramCode(gradStudent.getStudentID(), careerProgramCode);
        if (!optional.isPresent()) {
            StudentCareerProgramEntity entity = new StudentCareerProgramEntity();
            entity.setId(UUID.randomUUID());
            entity.setStudentID(gradStudent.getStudentID());
            entity.setCareerProgramCode(careerProgramCode);
            studentCareerProgramRepository.save(entity);
        }
    }

    @Transactional(transactionManager = "studentTransactionManager", readOnly = true)
    public boolean existsCareerProgram(UUID studentID) {
        List<StudentCareerProgramEntity> list = studentCareerProgramRepository.findByStudentID(studentID);
        if (list != null && !list.isEmpty()) {
            return true;
        }
        return false;
    }

}
