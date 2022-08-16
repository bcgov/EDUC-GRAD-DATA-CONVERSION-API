package ca.bc.gov.educ.api.dataconversion.service.student;

import ca.bc.gov.educ.api.dataconversion.constant.ConversionResultType;
import ca.bc.gov.educ.api.dataconversion.entity.student.*;
import ca.bc.gov.educ.api.dataconversion.model.*;
import ca.bc.gov.educ.api.dataconversion.model.StudentAssessment;
import ca.bc.gov.educ.api.dataconversion.model.StudentCourse;
import ca.bc.gov.educ.api.dataconversion.model.tsw.*;
import ca.bc.gov.educ.api.dataconversion.model.tsw.report.ReportData;
import ca.bc.gov.educ.api.dataconversion.repository.student.*;

import ca.bc.gov.educ.api.dataconversion.service.assessment.AssessmentService;
import ca.bc.gov.educ.api.dataconversion.service.course.CourseService;
import ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiConstants;
import ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiUtils;
import ca.bc.gov.educ.api.dataconversion.util.JsonUtil;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
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
    private static final String DATA_CONVERSION_HISTORY_ACTIVITY_CODE = "DATACONVERT";
    private static final String TRAX_API_ERROR_MSG = "Grad Trax API is failed for ";

    private final GraduationStudentRecordRepository graduationStudentRecordRepository;
    private final StudentOptionalProgramRepository studentOptionalProgramRepository;
    private final StudentCareerProgramRepository studentCareerProgramRepository;
    private final GraduationStudentRecordHistoryRepository graduationStudentRecordHistoryRepository;
    private final StudentOptionalProgramHistoryRepository studentOptionalProgramHistoryRepository;
    private final EducGradDataConversionApiConstants constants;

    private final RestUtils restUtils;
    private final AssessmentService assessmentService;
    private final CourseService courseService;
    private final ReportService reportService;

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
                          ReportService reportService) {
        this.graduationStudentRecordRepository = graduationStudentRecordRepository;
        this.studentOptionalProgramRepository = studentOptionalProgramRepository;
        this.studentCareerProgramRepository = studentCareerProgramRepository;
        this.graduationStudentRecordHistoryRepository = graduationStudentRecordHistoryRepository;
        this.studentOptionalProgramHistoryRepository = studentOptionalProgramHistoryRepository;
        this.constants = constants;
        this.restUtils = restUtils;
        this.assessmentService = assessmentService;
        this.courseService = courseService;
        this.reportService = reportService;
    }

    @Transactional(transactionManager = "studentTransactionManager")
    public ConvGradStudent convertStudent(ConvGradStudent convGradStudent, ConversionStudentSummaryDTO summary) {
        summary.setProcessedCount(summary.getProcessedCount() + 1L);
        try {
            String accessToken = summary.getAccessToken();

            // School validation
            boolean schoolExists = validateSchool(convGradStudent, summary);
            if (convGradStudent.getResult() == ConversionResultType.FAILURE) { // Grad Trax API is failed
                return convGradStudent;
            } else if (!schoolExists) {
                ConversionAlert error = new ConversionAlert();
                error.setItem(convGradStudent.getPen());
                error.setReason("Invalid school of record " + convGradStudent.getSchoolOfRecord());
                summary.getErrors().add(error);
                convGradStudent.setResult(ConversionResultType.FAILURE);
                return convGradStudent;
            }

            // PEN Student
            List<Student> students = getStudentsFromPEN(convGradStudent, summary);
            if (convGradStudent.getResult() == ConversionResultType.FAILURE) { // PEN Student API is failed
                return convGradStudent;
            } else if (students == null || students.isEmpty()) {
                ConversionAlert error = new ConversionAlert();
                error.setItem(convGradStudent.getPen());
                error.setReason("PEN does not exist: PEN Student API returns empty response.");
                summary.getErrors().add(error);
                convGradStudent.setResult(ConversionResultType.FAILURE);
                return convGradStudent;
            }

            // Student conversion process
            processStudents(convGradStudent, students, summary, accessToken);

        } catch (Exception e) {
            ConversionAlert error = new ConversionAlert();
            error.setItem(convGradStudent.getPen());
            error.setReason("Unexpected Exception is occurred: " + e.getLocalizedMessage());
            summary.getErrors().add(error);
            convGradStudent.setResult(ConversionResultType.FAILURE);

        }
        return convGradStudent;
    }

    private Boolean validateSchool(ConvGradStudent convGradStudent, ConversionStudentSummaryDTO summary) {
        // School validation
        Boolean schoolExists = null;
        try {
            schoolExists = restUtils.checkSchoolExists(convGradStudent.getSchoolOfRecord(), summary.getAccessToken());
        } catch (Exception e) {
            ConversionAlert error = new ConversionAlert();
            error.setItem(convGradStudent.getPen());
            error.setReason(TRAX_API_ERROR_MSG + "validating school existence : " + e.getLocalizedMessage());
            summary.getErrors().add(error);
            convGradStudent.setResult(ConversionResultType.FAILURE);
        }
        return schoolExists;
    }

    private List<Student> getStudentsFromPEN(ConvGradStudent convGradStudent, ConversionStudentSummaryDTO summary) {
        List<Student> students = null;
        try {
            // Call PEN Student API
            students = restUtils.getStudentsByPen(convGradStudent.getPen(), summary.getAccessToken());
        } catch (Exception e) {
            ConversionAlert error = new ConversionAlert();
            error.setItem(convGradStudent.getPen());
            error.setReason("PEN Student API is failed: " + e.getLocalizedMessage());
            summary.getErrors().add(error);
            convGradStudent.setResult(ConversionResultType.FAILURE);
        }
        return students;
    }

    private void processStudents(ConvGradStudent convGradStudent, List<Student> students, ConversionStudentSummaryDTO summary, String accessToken) {
        if (convGradStudent.isGraduated()) {
            log.debug("Process Graduated Students for pen# : " + convGradStudent.getPen());
        } else {
            log.debug("Process Non-Graduated Students for pen# : " + convGradStudent.getPen());
        }
        students.forEach(st -> {
            GraduationStudentRecordEntity gradStudentEntity = processStudent(convGradStudent, st, summary);
            if (gradStudentEntity != null) {
                processDedendencies(gradStudentEntity.getStudentID(), convGradStudent, gradStudentEntity, st, summary, accessToken);
            }

            if (convGradStudent.getResult() == null) {
                convGradStudent.setResult(ConversionResultType.SUCCESS);
            }
        });
    }

    private GraduationStudentRecordEntity processStudent(ConvGradStudent convGradStudent, Student penStudent,  ConversionStudentSummaryDTO summary) {
        boolean isStudentPersisted = false;
        UUID studentID = UUID.fromString(penStudent.getStudentID());
        Optional<GraduationStudentRecordEntity> stuOptional = graduationStudentRecordRepository.findById(studentID);
        GraduationStudentRecordEntity gradStudentEntity;
        if (stuOptional.isPresent()) {
            gradStudentEntity = stuOptional.get();
            gradStudentEntity.setPen(penStudent.getPen());
            if (convGradStudent.isGraduated()) {
                convertGraduatedStudentData(convGradStudent, gradStudentEntity, summary);
            } else {
                convertStudentData(convGradStudent, gradStudentEntity, summary);
            }
            gradStudentEntity.setUpdateDate(null);
            gradStudentEntity.setUpdateUser(null);
            gradStudentEntity = graduationStudentRecordRepository.save(gradStudentEntity);
            isStudentPersisted = true;
            summary.setUpdatedCount(summary.getUpdatedCount() + 1L);
        } else {
            gradStudentEntity = new GraduationStudentRecordEntity();
            gradStudentEntity.setPen(penStudent.getPen());
            gradStudentEntity.setStudentID(studentID);
            if (convGradStudent.isGraduated()) {
                convertGraduatedStudentData(convGradStudent, gradStudentEntity, summary);
            } else {
                convertStudentData(convGradStudent, gradStudentEntity, summary);
            }
            // if year "1950" without "AD" or "AN", then program is null
            if (StringUtils.isNotBlank(gradStudentEntity.getProgram())) {
                gradStudentEntity = graduationStudentRecordRepository.save(gradStudentEntity);
                summary.setAddedCount(summary.getAddedCount() + 1L);
                isStudentPersisted = true;
            }
        }
        if (isStudentPersisted) {
            return gradStudentEntity;
        } else {
            return null;
        }
    }

    private void processDedendencies(UUID studentID,
                                     ConvGradStudent convGradStudent,
                                     GraduationStudentRecordEntity gradStudentEntity,
                                     Student penStudent,
                                     ConversionStudentSummaryDTO summary, String accessToken) {

        ConversionResultType result;

        // graduation status history
        createGraduationStudentRecordHistory(gradStudentEntity, DATA_CONVERSION_HISTORY_ACTIVITY_CODE);

        // student guid - pen
        if (constants.isStudentGuidPenXrefEnabled()) {
            saveStudentGuidPenXref(studentID, convGradStudent.getPen());
        }

        // process dependencies
        gradStudentEntity.setPen(convGradStudent.getPen());
        if (convGradStudent.isGraduated()) {
            result = processOptionalProgramsForGraduatedStudent(gradStudentEntity, accessToken, summary);
        } else {
            result = processOptionalPrograms(gradStudentEntity, accessToken, summary);
        }

        if (result != ConversionResultType.FAILURE) {
            result = processProgramCodes(gradStudentEntity, convGradStudent.getProgramCodes(), accessToken, summary);
        }
        if (result != ConversionResultType.FAILURE) {
            result = processSccpFrenchCertificates(gradStudentEntity, accessToken, summary);
        }

        if (convGradStudent.isGraduated()) {
            // Building GraduationData CLOB data
            GraduationData graduationData = buildGraduationData(convGradStudent, gradStudentEntity, penStudent, summary);
            if (graduationData != null) {
                try {
                    gradStudentEntity.setStudentGradData(JsonUtil.getJsonStringFromObject(graduationData));
                } catch (JsonProcessingException jpe) {
                    log.error("Json Parsing Error: " + jpe.getLocalizedMessage());
                }
            }
            createAndStoreStudentTranscript(graduationData,graduationData.getGradStatus(),accessToken);


            // TODO(sks) : report, transcript, certificate generation & saving
        }
        convGradStudent.setResult(result);
    }

    private void createAndStoreStudentTranscript(GraduationData graduationData, GradAlgorithmGraduationStudentRecord gradStatus, String accessToken) {
        ReportData data = reportService.prepareTranscriptData(graduationData, gradStatus,accessToken);
        reportService.saveStudentTranscriptReportJasper(data, accessToken, gradStatus.getStudentID(), graduationData.isGraduated());
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
        studentEntity.setConsumerEducationRequirementMet(student.getConsumerEducationRequirementMet());
    }

    private void convertGraduatedStudentData(ConvGradStudent student, GraduationStudentRecordEntity studentEntity, ConversionStudentSummaryDTO summary) {
        if (determineProgram(student, summary)) {
            studentEntity.setProgram(student.getProgram());
        }

        studentEntity.setGpa(student.getGpa());
        studentEntity.setHonoursStanding(student.getHonoursStanding());
        if (studentEntity.getProgram().equals("SCCP")) {
            studentEntity.setProgramCompletionDate(EducGradDataConversionApiUtils.parseDate(student.getSlpDate(), EducGradDataConversionApiConstants.TRAX_SLP_DATE_FORMAT));
        } else {
            studentEntity.setProgramCompletionDate(student.getProgramCompletionDate());
        }
        studentEntity.setSchoolAtGrad(StringUtils.isNotBlank(student.getSchoolAtGrad())? student.getSchoolAtGrad() : null);
        studentEntity.setSchoolOfRecord(StringUtils.isNotBlank(student.getSchoolOfRecord())? student.getSchoolOfRecord() : null);
        studentEntity.setRecalculateGradStatus(null);
        studentEntity.setRecalculateProjectedGrad(null);
        studentEntity.setStudentGrade(student.getStudentGrade());
        studentEntity.setStudentStatus(getGradStudentStatus(student.getStudentStatus(), student.getArchiveFlag()));

        // Mappings with Student_Master
        studentEntity.setFrenchCert(student.getFrenchCert());
        studentEntity.setEnglishCert(student.getEnglishCert());
        studentEntity.setConsumerEducationRequirementMet(student.getConsumerEducationRequirementMet());
    }

    private GraduationData buildGraduationData(ConvGradStudent student, GraduationStudentRecordEntity studentEntity, Student penStudent, ConversionStudentSummaryDTO summary) {
        GraduationData graduationData = new GraduationData();

        TranscriptStudentDemog transcriptStudentDemog;
        // TSW_TRAN_DEMOG
        try {
            transcriptStudentDemog = restUtils.getTranscriptStudentDemog(student.getPen(), summary.getAccessToken());
        } catch (Exception e) {
            log.error(TRAX_API_ERROR_MSG + "getting Transcript Student Demog data : " + e.getLocalizedMessage());
            return null;
        }

        // gradStatus
        GradAlgorithmGraduationStudentRecord gradStatus = new GradAlgorithmGraduationStudentRecord();
        gradStatus.setStudentID(studentEntity.getStudentID());
        gradStatus.setProgram(studentEntity.getProgram());
        gradStatus.setProgramCompletionDate(EducGradDataConversionApiUtils.formatDate(studentEntity.getProgramCompletionDate()));
        gradStatus.setGpa(studentEntity.getGpa());
        gradStatus.setSchoolOfRecord(studentEntity.getSchoolOfRecord());
        gradStatus.setStudentGrade(studentEntity.getStudentGrade());
        gradStatus.setStudentStatus(studentEntity.getStudentStatus());
        gradStatus.setSchoolAtGrad(studentEntity.getSchoolAtGrad());
        gradStatus.setHonoursStanding(studentEntity.getHonoursStanding());
        gradStatus.setRecalculateGradStatus(studentEntity.getRecalculateGradStatus());
        graduationData.setGradStatus(gradStatus);

        // graduated Student
        GradSearchStudent gradStudent = populateGraduateStudentInfo(studentEntity, penStudent, transcriptStudentDemog);
        graduationData.setGradStudent(gradStudent);

        // TSW_TRAN_CRSE
        List<TranscriptStudentCourse> transcriptStudentCourses = retrieveTswStudentCourses(student.getPen(), summary.getAccessToken());

        // school
        // TODO (sks) : replace this with getting School object from Trax API
        School school = new School();
        school.setMinCode(transcriptStudentDemog.getMincode());
        school.setSchoolName(transcriptStudentDemog.getSchoolName());
        school.setAddress1(transcriptStudentDemog.getAddress1());
        school.setCity(transcriptStudentDemog.getCity());
        school.setProvCode(transcriptStudentDemog.getProvCode());
        school.setPostal(transcriptStudentDemog.getPostal());
        graduationData.setSchool(school);

        // studentCourses
        List<ca.bc.gov.educ.api.dataconversion.model.tsw.StudentCourse> studentCourseList = buildStudentCourses(transcriptStudentCourses);
        StudentCourses studentCourses = new StudentCourses();
        studentCourses.setStudentCourseList(studentCourseList);
        graduationData.setStudentCourses(studentCourses);

        // optionalGradStatus
        List<GradAlgorithmOptionalStudentProgram> optionalGradStatus = buildOptionalGradStatus(studentEntity, studentCourseList, summary);
        graduationData.setOptionalGradStatus(optionalGradStatus);

        // requirements met
        List<GradRequirement> requirementsMet = buildRequirementsMet(studentEntity.getProgram(), transcriptStudentCourses, summary);
        graduationData.setRequirementsMet(requirementsMet);

        // gradMessage
        graduationData.setGradMessage(transcriptStudentDemog.getGradTextMessage());

        // dualDogwood
        graduationData.setDualDogwood(studentEntity.isDualDogwood());

        // gradProgram
        graduationData.setGradProgram(retrieveGradProgram(studentEntity.getProgram(), summary.getAccessToken()));

        // graduated
        graduationData.setGraduated(true);

        return graduationData;
    }

    private GradSearchStudent populateGraduateStudentInfo(GraduationStudentRecordEntity studentEntity, Student penStudent, TranscriptStudentDemog transcriptStudentDemog ) {
        GradSearchStudent gradSearchStudent = GradSearchStudent.builder()
                .studentID(penStudent.getStudentID())
                .pen(penStudent.getPen())
                .legalFirstName(penStudent.getLegalFirstName())
                .legalMiddleNames(penStudent.getLegalMiddleNames())
                .legalLastName(penStudent.getLegalLastName())
                .dob(penStudent.getDob())
                .sexCode(penStudent.getSexCode())
                .genderCode(penStudent.getGenderCode())
                .usualFirstName(penStudent.getUsualFirstName())
                .usualMiddleNames(penStudent.getUsualMiddleNames())
                .usualLastName(penStudent.getUsualLastName())
                .email(penStudent.getEmail())
                .emailVerified(penStudent.getEmailVerified())
                .deceasedDate(penStudent.getDeceasedDate())
                .postalCode(penStudent.getPostalCode())
                .mincode(penStudent.getMincode())
                .localID(penStudent.getLocalID())
                .gradeCode(penStudent.getGradeCode())
                .gradeYear(penStudent.getGradeYear())
                .demogCode(penStudent.getDemogCode())
                .statusCode(penStudent.getStatusCode())
                .memo(penStudent.getMemo())
                .trueStudentID(penStudent.getTrueStudentID())
        .build();

        gradSearchStudent.setProgram(studentEntity.getProgram());
        gradSearchStudent.setStudentGrade(studentEntity.getStudentGrade());
        gradSearchStudent.setStudentStatus(studentEntity.getStudentStatus());
        // TODO (sks) : replace this with getting School object from Trax API
        gradSearchStudent.setSchoolOfRecord(studentEntity.getSchoolOfRecord());
        gradSearchStudent.setSchoolOfRecordName(transcriptStudentDemog.getSchoolName());
        gradSearchStudent.setSchoolOfRecordindependentAffiliation(null);

        return gradSearchStudent;
    }

    private List<TranscriptStudentCourse> retrieveTswStudentCourses(String pen, String accessToken) {
        List<TranscriptStudentCourse> courses = new ArrayList<>();
        try {
            courses = restUtils.getTranscriptStudentCourses(pen, accessToken);
        } catch (Exception e) {
            log.error(TRAX_API_ERROR_MSG + "getting TSW_TRAN_CRSE records : " + e.getLocalizedMessage());
        }
        return courses;
    }

    private List<ca.bc.gov.educ.api.dataconversion.model.tsw.StudentCourse> buildStudentCourses(List<TranscriptStudentCourse> tswStudentCourse) {
        List<ca.bc.gov.educ.api.dataconversion.model.tsw.StudentCourse> studentCourses = new ArrayList<>();
        for (TranscriptStudentCourse tswCourse : tswStudentCourse) {
            ca.bc.gov.educ.api.dataconversion.model.tsw.StudentCourse studentCourse = populateStudentCourse(tswCourse);
            studentCourses.add(studentCourse);
        }

        return studentCourses;
    }

    private ca.bc.gov.educ.api.dataconversion.model.tsw.StudentCourse populateStudentCourse(TranscriptStudentCourse tswCourse) {
        return ca.bc.gov.educ.api.dataconversion.model.tsw.StudentCourse.builder()
                .pen(tswCourse.getStudNo())
                .courseCode(tswCourse.getCourseCode())
                .courseLevel(tswCourse.getCourseLevel())
                .courseName(tswCourse.getCourseName())
                .originalCredits(NumberUtils.isCreatable(tswCourse.getNumberOfCredits())? Integer.parseInt(tswCourse.getNumberOfCredits()) : null)
                .credits(NumberUtils.isCreatable(tswCourse.getNumberOfCredits())? Integer.parseInt(tswCourse.getNumberOfCredits()) : null)
                .creditsUsedForGrad(NumberUtils.isCreatable(tswCourse.getNumberOfCredits())? Integer.parseInt(tswCourse.getNumberOfCredits()) : null)
                .sessionDate(tswCourse.getCourseSession())
                .completedCoursePercentage(NumberUtils.isCreatable(tswCourse.getFinalPercentage())? Double.parseDouble(tswCourse.getFinalPercentage()) : Double.parseDouble("0.0"))
                .completedCourseLetterGrade(tswCourse.getFinalLG())
                .schoolPercent(NumberUtils.isCreatable(tswCourse.getSchoolPercentage())? Double.parseDouble(tswCourse.getSchoolPercentage()) : null)
                .bestSchoolPercent(NumberUtils.isCreatable(tswCourse.getSchoolPercentage())? Double.parseDouble(tswCourse.getSchoolPercentage()) : null)
                .examPercent(NumberUtils.isCreatable(tswCourse.getExamPercentage())? Double.parseDouble(tswCourse.getExamPercentage()) : null)
                .bestExamPercent(NumberUtils.isCreatable(tswCourse.getExamPercentage())? Double.parseDouble(tswCourse.getExamPercentage()) : null)
                .hasRelatedCourse("N")
                .metLitNumRequirement(tswCourse.getMetLitNumReqt())
                .gradReqMet("") // TODO
                .gradReqMetDetail("") // TODO
                .hasRelatedCourse("N") // TODO
                .genericCourseType("") //
                .specialCase("N")
                .provExamCourse("N") // TODO
                .isUsed(false) // TODO
                .isProjected(false)
                .isRestricted(false)
                .isDuplicate(false)
                .isFailed(false)
                .isNotCompleted(false)
                .isLessCreditCourse(false)
                .isValidationCourse(false)
                .isGrade10Course(false)
                .isCareerPrep(false)
                .isLocallyDeveloped(false)
                .isNotEligibleForElective(false)
                .isBoardAuthorityAuthorized(false)
                .isIndependentDirectedStudies(false)
                .isCutOffCourse(false)
            .build();
    }

    private List<GradAlgorithmOptionalStudentProgram> buildOptionalGradStatus(GraduationStudentRecordEntity studentEntity,
              List<ca.bc.gov.educ.api.dataconversion.model.tsw.StudentCourse> studentCourseList, ConversionStudentSummaryDTO summary) {
        List<GradAlgorithmOptionalStudentProgram> results = new ArrayList<>();
        List<StudentOptionalProgramEntity> list = studentOptionalProgramRepository.findByStudentID(studentEntity.getStudentID());
        for (StudentOptionalProgramEntity ent : list) {
            GradAlgorithmOptionalStudentProgram result = populateOptionStudentProgramStatus(ent, summary.getAccessToken());
            if (result != null) {
                StudentCourses studentCourses = new StudentCourses();
                studentCourses.setStudentCourseList(studentCourseList);
                result.setOptionalStudentCourses(studentCourses);

                results.add(result);
            }

        }
        return results;

    }

    private GradAlgorithmOptionalStudentProgram populateOptionStudentProgramStatus(StudentOptionalProgramEntity entity, String accessToken) {
        GradAlgorithmOptionalStudentProgram result = new GradAlgorithmOptionalStudentProgram();
        try {
            OptionalProgram optionalProgram = restUtils.getOptionalProgramByID(entity.getOptionalProgramID(), accessToken);
            if (optionalProgram != null) {
                result.setOptionalProgramID(optionalProgram.getOptionalProgramID());
                result.setOptionalProgramCode(optionalProgram.getOptProgramCode());
                result.setOptionalProgramCompletionDate(EducGradDataConversionApiUtils.formatDate(entity.getSpecialProgramCompletionDate()));
                result.setOptionalRequirementsMet(new ArrayList<>());
                result.setStudentID(entity.getStudentID());
                result.setCpList(buildStudentCareerProgramList(entity, accessToken));
                result.setOptionalGraduated(true);
            }
        } catch (Exception e) {
            log.error("Program API is failed to get OptionalProgram! : " + e.getLocalizedMessage());
            return null;
        }
        return result;
    }

    private List<StudentCareerProgram> buildStudentCareerProgramList(StudentOptionalProgramEntity entity, String accessToken) {
        List<StudentCareerProgram> results = new ArrayList<>();

        List<StudentCareerProgramEntity> list = studentCareerProgramRepository.findByStudentID(entity.getStudentID());
        for (StudentCareerProgramEntity ent : list) {
            StudentCareerProgram studentCareerProgram = populateStudentCareerProgram(ent, accessToken);
            if (studentCareerProgram != null) {
                results.add(studentCareerProgram);
            }
        }

        return results;
    }

    private StudentCareerProgram populateStudentCareerProgram(StudentCareerProgramEntity entity, String accessToken) {
        StudentCareerProgram obj = new StudentCareerProgram();

        try {
            CareerProgram careerProgram = restUtils.getCareerProgram(entity.getCareerProgramCode(), accessToken);
            obj.setCareerProgramName(careerProgram.getDescription());
        } catch (Exception e) {
            log.error("Program API is failed to get CareerProgram! : " + e.getLocalizedMessage());
            return null;
        }
        obj.setCareerProgramCode(entity.getCareerProgramCode());
        obj.setStudentID(entity.getStudentID());
        obj.setId(entity.getId());

        return obj;
    }

    private List<GradRequirement> buildRequirementsMet(String programCode, List<TranscriptStudentCourse> tswStudentCourses, ConversionStudentSummaryDTO summary) {
        List<GradRequirement> requirements = new ArrayList<>();
        for (TranscriptStudentCourse tswCourse : tswStudentCourses) {
            if (StringUtils.isNotBlank(tswCourse.getFoundationReq())) {
                GradRequirement gradRequirement = populateRequirement(programCode, tswCourse.getFoundationReq(), summary);
                if (gradRequirement != null) {
                    requirements.add(gradRequirement);
                }
            }
        }
        return requirements;
    }

    private GradRequirement populateRequirement(String programCode, String traxReqNumber, ConversionStudentSummaryDTO summary) {
        GradRuleDetails rule = null;
        List<GradRuleDetails> rules = retrieveProgramRulesByTraxReqNumber(traxReqNumber, summary.getAccessToken());
        for (GradRuleDetails ruleDetail : rules) {
            if (StringUtils.equals(ruleDetail.getProgramCode(), programCode)) {
                rule = ruleDetail;
                break;
            }
        }
        if (rule != null) {
            GradRequirement result = new GradRequirement();
            result.setRule(rule.getRuleCode());
            result.setDescription(rule.getRequirementName());
            result.setProjected(false);
            return result;
        } else {
            return null;
        }

    }

    private GraduationProgramCode retrieveGradProgram(String programCode, String accessToken) {
        GraduationProgramCode program = null;
        try {
            program = restUtils.getGradProgram(programCode, accessToken);
        } catch (Exception e) {
            log.error("Program API is failed to get Grad Programs! : " + e.getLocalizedMessage());
        }
        return program;
    }

    private List<GradRuleDetails> retrieveProgramRulesByTraxReqNumber(String traxReqNumber, String accessToken) {
        List<GradRuleDetails> rules = new ArrayList<>();
        try {
            rules = restUtils.getGradProgramRulesByTraxReqNumber(traxReqNumber, accessToken);
        } catch (Exception e) {
            log.error("Program API is failed to get Program Rules! : " + e.getLocalizedMessage());
        }
        return rules;
    }

    private ConversionResultType processOptionalPrograms(GraduationStudentRecordEntity student, String accessToken, ConversionStudentSummaryDTO summary) {
        if (StringUtils.isBlank(student.getProgram())) {
            return ConversionResultType.SUCCESS;
        }

        // Dual Dogwood for yyyy-PF
        if (student.getProgram().endsWith("-PF")) {
            return createStudentOptionalProgram("DD", student, accessToken, summary);
        }

        // French Immersion for 2018-EN, 2004-EN
        if (hasAnyFrenchImmersionCourse(student.getProgram(), student.getPen(), student.getFrenchCert(), accessToken)) {
            return createStudentOptionalProgram("FI", student, accessToken, summary);
        }

        return ConversionResultType.SUCCESS;
    }

    private ConversionResultType processOptionalProgramsForGraduatedStudent(GraduationStudentRecordEntity student, String accessToken, ConversionStudentSummaryDTO summary) {
        if (StringUtils.isBlank(student.getProgram())) {
            return ConversionResultType.SUCCESS;
        }

        // Dual Dogwood for yyyy-PF
        if (student.getProgram().endsWith("-PF") && StringUtils.equalsIgnoreCase(student.getEnglishCert(), "E")) {
            student.setDualDogwood(true);
            return createStudentOptionalProgram("DD", student, accessToken, summary);
        }

        // French Immersion for mincode[1:3] <> '093' and french_cert = 'F'
        if (!student.getSchoolOfRecord().startsWith("093") && StringUtils.equalsIgnoreCase(student.getFrenchCert(), "F")) {
            return createStudentOptionalProgram("FI", student, accessToken, summary);
        }

        return ConversionResultType.SUCCESS;
    }

    protected boolean hasAnyFrenchImmersionCourse(String program, String pen, String frenchCert, String accessToken) {
        boolean frenchImmersion = false;
        // French Immersion for 2018-EN, 2004-EN
        if (program.equals("2018-EN") || program.equals("2004-EN")) {
            if (courseService.isFrenchImmersionCourse(pen, "10", accessToken)) {
                frenchImmersion = true;
            }
        } else if (program.equals("1996-EN")) {
            if (courseService.isFrenchImmersionCourse(pen, "11", accessToken)) {
                frenchImmersion = true;
            }
        } else if (program.equals("1986-EN") && isFrenchCertificate(frenchCert, pen, accessToken)) {
            frenchImmersion = true;
        }
        return frenchImmersion;
    }

    private boolean isFrenchCertificate(String frenchCert, String pen, String accessToken) {
        return StringUtils.equalsIgnoreCase("F", frenchCert) || courseService.isFrenchImmersionCourseForEN(pen, "11", accessToken);
    }

    private ConversionResultType processProgramCodes(GraduationStudentRecordEntity student, List<String> programCodes, String accessToken, ConversionStudentSummaryDTO summary) {
        ConversionResultType resultType = ConversionResultType.SUCCESS;
        boolean isCareerProgramCreated = false;
        if (StringUtils.isNotBlank(student.getProgram()) && !programCodes.isEmpty()) {
            for (String programCode : programCodes) {
                if (isOptionalProgramCode(programCode)) {
                    resultType = createStudentOptionalProgram(programCode, student, accessToken, summary);
                } else {
                    resultType = createStudentCareerProgram(programCode, student, summary);
                    if (resultType == ConversionResultType.SUCCESS) {
                        isCareerProgramCreated = true;
                    }
                }
                if (resultType == ConversionResultType.FAILURE) {
                    break;
                }
            }
            if (isCareerProgramCreated) {
                resultType = createStudentOptionalProgram("CP", student, accessToken, summary);
            }
        }
        return resultType;
    }

    private ConversionResultType processSccpFrenchCertificates(GraduationStudentRecordEntity student, String accessToken, ConversionStudentSummaryDTO summary) {
        if (StringUtils.equals(student.getProgram(), "SCCP")
            && ( StringUtils.isNotBlank(student.getSchoolOfRecord())
                 && student.getSchoolOfRecord().startsWith("093") )
        ) {
            return createStudentOptionalProgram("FR", student, accessToken, summary);
        }
        return ConversionResultType.SUCCESS;
    }

    private boolean isOptionalProgramCode(String code) {
       return OPTIONAL_PROGRAM_CODES.contains(code);
    }

    private ConversionResultType createStudentOptionalProgram(String optionalProgramCode, GraduationStudentRecordEntity student, String accessToken, ConversionStudentSummaryDTO summary) {
        StudentOptionalProgramEntity entity = new StudentOptionalProgramEntity();
        entity.setPen(student.getPen());
        entity.setStudentID(student.getStudentID());

        OptionalProgram optionalProgram;
        // Call GRAD Program API
        try {
            optionalProgram = restUtils.getOptionalProgram(student.getProgram(), optionalProgramCode, accessToken);
        } catch (Exception e) {
            ConversionAlert error = new ConversionAlert();
            error.setLevel(ConversionAlert.AlertLevelEnum.WARNING);
            error.setItem(student.getPen());
            error.setReason("Grad Program API is failed to retrieve Optional Program [" + optionalProgramCode + "] - " + e.getLocalizedMessage());
            summary.getErrors().add(error);
            return ConversionResultType.WARNING;
        }
        if (optionalProgram != null && optionalProgram.getOptionalProgramID() != null) {
            entity.setOptionalProgramID(optionalProgram.getOptionalProgramID());
            Optional<StudentOptionalProgramEntity> stdSpecialProgramOptional = studentOptionalProgramRepository.findByStudentIDAndOptionalProgramID(student.getStudentID(), optionalProgram.getOptionalProgramID());
            if (stdSpecialProgramOptional.isPresent()) {
                StudentOptionalProgramEntity currentEntity = stdSpecialProgramOptional.get();
                currentEntity.setUpdateDate(null);
                currentEntity.setUpdateUser(null);
                studentOptionalProgramRepository.save(currentEntity); // touch: update_user & update_date will be updated only.
                createStudentOptionalProgramHistory(currentEntity, DATA_CONVERSION_HISTORY_ACTIVITY_CODE); // student optional program history
            } else {
                entity.setId(UUID.randomUUID());
                studentOptionalProgramRepository.save(entity);
                createStudentOptionalProgramHistory(entity, DATA_CONVERSION_HISTORY_ACTIVITY_CODE); // student optional program history
            }
            summary.incrementOptionalProgram(optionalProgramCode);
        }
        return ConversionResultType.SUCCESS;
    }

    private ConversionResultType createStudentCareerProgram(String careerProgramCode, GraduationStudentRecordEntity student, ConversionStudentSummaryDTO summary) {
        StudentCareerProgramEntity entity = new StudentCareerProgramEntity();
        entity.setStudentID(student.getStudentID());

        CareerProgram careerProgram;
        // Call GRAD Program API
        try {
            careerProgram = restUtils.getCareerProgram(careerProgramCode, summary.getAccessToken());
        } catch (Exception e) {
            ConversionAlert error = new ConversionAlert();
            error.setLevel(ConversionAlert.AlertLevelEnum.WARNING);
            error.setItem(student.getPen());
            error.setReason("Grad Program API is failed to retrieve Career Program [" + careerProgramCode + "] - " + e.getLocalizedMessage());
            summary.getErrors().add(error);
            return ConversionResultType.WARNING;
        }
        if (careerProgram != null) {
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
            return ConversionResultType.SUCCESS;
        } else {
            ConversionAlert error = new ConversionAlert();
            error.setLevel(ConversionAlert.AlertLevelEnum.WARNING);
            error.setItem(student.getPen());
            error.setReason("Career Program Code does not exist: " + careerProgramCode);
            summary.getErrors().add(error);
            return ConversionResultType.WARNING;
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
    public StudentGradDTO loadStudentData(String pen, String accessToken) {
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
        studentData.getProgramCodes().addAll(getOptionalProgramCodes(optionalPrograms, accessToken));

        // career programs
        List<StudentCareerProgramEntity> careerPrograms = studentCareerProgramRepository.findByStudentID(studentID);
        studentData.getProgramCodes().addAll(getCareerProgramCodes(careerPrograms));

        // courses
        List<StudentCourse> courses = courseService.getStudentCourses(pen, accessToken);
        studentData.getCourses().addAll(courses);
        // assessments
        List<StudentAssessment> assessments = assessmentService.getStudentAssessments(pen, accessToken);
        studentData.getAssessments().addAll(assessments);

        return studentData;
    }

    private List<String> getOptionalProgramCodes(List<StudentOptionalProgramEntity> studentOptionalProgramEntities, String accessToken) {
        List<String> codes = new ArrayList<>();
        studentOptionalProgramEntities.forEach(e -> {
            OptionalProgram op = restUtils.getOptionalProgramByID(e.getOptionalProgramID(), accessToken);
            if (op != null) {
                codes.add(op.getOptProgramCode());
            }
        });
        return codes;
    }

    private List<String> getCareerProgramCodes(List<StudentCareerProgramEntity> studentCareerProgramEntities) {
        List<String> codes = new ArrayList<>();
        studentCareerProgramEntities.forEach(
            e -> codes.add(e.getCareerProgramCode())
        );
        return codes;
    }

    @Transactional(transactionManager = "studentTransactionManager")
    public void saveGraduationStudent(StudentGradDTO gradStudent, String accessToken) {
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
            addStudentOptionalProgram("DD", gradStudent, accessToken);
        } else if (gradStudent.isDeleteDualDogwood()) {
            log.info(" => [DD] optional program will be removed if exist.");
            removeStudentOptionalProgram("DD", gradStudent, accessToken);
        }
    }

    @Transactional(transactionManager = "studentTransactionManager")
    public void removeStudentOptionalProgram(String optionalProgramCode, StudentGradDTO gradStudent, String accessToken) {
        // Call GRAD Program API
        OptionalProgram optionalProgram = restUtils.getOptionalProgram(gradStudent.getProgram(), optionalProgramCode, accessToken);
        if (optionalProgram != null) {
            removeStudentOptionalProgram(optionalProgram.getOptionalProgramID(), gradStudent);
        }
    }

    @Transactional(transactionManager = "studentTransactionManager")
    public void removeStudentOptionalProgram(UUID optionalProgramID, StudentGradDTO gradStudent) {
        Optional<StudentOptionalProgramEntity> optional = studentOptionalProgramRepository.findByStudentIDAndOptionalProgramID(gradStudent.getStudentID(), optionalProgramID);
        if (optional.isPresent()) {
            StudentOptionalProgramEntity entity = optional.get();
            createStudentOptionalProgramHistory(entity, "TRAXDELETE");
            studentOptionalProgramRepository.delete(entity);
        }
    }

    @Transactional(transactionManager = "studentTransactionManager")
    public void addStudentOptionalProgram(String optionalProgramCode, StudentGradDTO gradStudent, String accessToken) {
        OptionalProgram optionalProgram = restUtils.getOptionalProgram(gradStudent.getProgram(), optionalProgramCode, accessToken);
        if (optionalProgram != null) {
            addStudentOptionalProgram(optionalProgram.getOptionalProgramID(), gradStudent);
        }
    }

    @Transactional(transactionManager = "studentTransactionManager")
    public void addStudentOptionalProgram(UUID optionalProgramID, StudentGradDTO gradStudent) {
        Optional<StudentOptionalProgramEntity> optional = studentOptionalProgramRepository.findByStudentIDAndOptionalProgramID(gradStudent.getStudentID(), optionalProgramID);
        if (!optional.isPresent()) {
            StudentOptionalProgramEntity entity = new StudentOptionalProgramEntity();
            entity.setId(UUID.randomUUID());
            entity.setStudentID(gradStudent.getStudentID());
            entity.setOptionalProgramID(optionalProgramID);
            studentOptionalProgramRepository.save(entity);
            createStudentOptionalProgramHistory(entity, "TRAXADD");
        }
    }

    @Transactional(transactionManager = "studentTransactionManager")
    public void removeStudentCareerProgram(String careerProgramCode, StudentGradDTO gradStudent) {
        Optional<StudentCareerProgramEntity> optional = studentCareerProgramRepository.findByStudentIDAndCareerProgramCode(gradStudent.getStudentID(), careerProgramCode);
        if (optional.isPresent()) {
            StudentCareerProgramEntity entity = optional.get();
            studentCareerProgramRepository.delete(entity);
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
        return list != null && !list.isEmpty();
    }

    @Transactional(transactionManager = "studentTransactionManager")
    public void triggerGraduationBatchRun(UUID studentID) {
        Optional<GraduationStudentRecordEntity> gradStatusOptional = graduationStudentRecordRepository.findById(studentID);
        if (gradStatusOptional.isPresent()) {
            GraduationStudentRecordEntity graduationStudentRecordEntity = gradStatusOptional.get();
            if (StringUtils.equals(graduationStudentRecordEntity.getStudentStatus(), "MER")
                || StringUtils.equals(graduationStudentRecordEntity.getStudentStatus(), "DEC")) {
                graduationStudentRecordEntity.setRecalculateGradStatus(null);
                graduationStudentRecordEntity.setRecalculateProjectedGrad(null);
            } else {
                graduationStudentRecordEntity.setRecalculateGradStatus("Y");
                graduationStudentRecordEntity.setRecalculateProjectedGrad("Y");
            }
            graduationStudentRecordRepository.save(graduationStudentRecordEntity);
        }
    }

}
