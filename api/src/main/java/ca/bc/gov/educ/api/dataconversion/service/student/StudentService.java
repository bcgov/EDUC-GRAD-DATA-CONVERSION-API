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
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import ca.bc.gov.educ.api.dataconversion.util.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiConstants.DEFAULT_CREATED_BY;
import static ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiConstants.DEFAULT_UPDATED_BY;

@Service
@Slf4j
public class StudentService extends StudentBaseService {

    private static final List<String> OPTIONAL_PROGRAM_CODES = Arrays.asList("AD", "BC", "BD");
    private static final String DATA_CONVERSION_HISTORY_ACTIVITY_CODE = "DATACONVERT";
    private static final String TRAX_API_ERROR_MSG = "Grad Trax API is failed for ";
    private static final String TSW_PF_GRAD_MSG = "Student has successfully completed the Programme Francophone.";

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

    /**
     * The Program Rules map.
     */
    private final Map<String, List<ProgramRequirement>> programRuleMap = new ConcurrentHashMap<>();

    /**
     * The Special Case map.
     */
    private final Map<String, SpecialCase> specialCaseMap = new ConcurrentHashMap<>();

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

    public void clearMaps() {
        programRuleMap.clear();
        specialCaseMap.clear();
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
                handleException(convGradStudent, summary, convGradStudent.getPen(), ConversionResultType.FAILURE, "Invalid school of record " + convGradStudent.getSchoolOfRecord());
                return convGradStudent;
            }

            // PEN Student
            List<Student> students = getStudentsFromPEN(convGradStudent, summary);
            if (convGradStudent.getResult() == ConversionResultType.FAILURE) { // PEN Student API is failed
                return convGradStudent;
            } else if (students == null || students.isEmpty()) {
                handleException(convGradStudent, summary, convGradStudent.getPen(), ConversionResultType.FAILURE, "PEN does not exist: PEN Student API returns empty response.");
                return convGradStudent;
            }

            // Student conversion process
            processStudents(convGradStudent, students, summary, accessToken);

        } catch (Exception e) {
            handleException(convGradStudent, summary, convGradStudent.getPen(), ConversionResultType.FAILURE, "Unexpected Exception is occurred: " + e.getLocalizedMessage());
        }
        return convGradStudent;
    }

    private Boolean validateSchool(ConvGradStudent convGradStudent, ConversionStudentSummaryDTO summary) {
        // School validation
        Boolean schoolExists = null;
        try {
            schoolExists = restUtils.checkSchoolExists(convGradStudent.getSchoolOfRecord(), summary.getAccessToken());
        } catch (Exception e) {
            handleException(convGradStudent, summary, convGradStudent.getPen(), ConversionResultType.FAILURE, TRAX_API_ERROR_MSG + "validating school existence : " + e.getLocalizedMessage());
        }
        return schoolExists;
    }

    private List<Student> getStudentsFromPEN(ConvGradStudent convGradStudent, ConversionStudentSummaryDTO summary) {
        List<Student> students = null;
        try {
            // Call PEN Student API
            students = restUtils.getStudentsByPen(convGradStudent.getPen(), summary.getAccessToken());
        } catch (Exception e) {
            handleException(convGradStudent, summary, convGradStudent.getPen(), ConversionResultType.FAILURE, "PEN Student API is failed: " + e.getLocalizedMessage());
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
                processDependencies(gradStudentEntity.getStudentID(), convGradStudent, gradStudentEntity, st, summary, accessToken);
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

    private void processDependencies(UUID studentID,
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

        if (convGradStudent.isGraduated() && !StringUtils.equalsIgnoreCase(gradStudentEntity.getStudentStatus(), STUDENT_STATUS_MERGED)) {
            // Building GraduationData CLOB data
            GraduationData graduationData = buildGraduationData(convGradStudent, gradStudentEntity, penStudent, summary);
            if (graduationData != null) {
                try {
                    gradStudentEntity.setStudentGradData(JsonUtil.getJsonStringFromObject(graduationData));
                } catch (JsonProcessingException jpe) {
                    log.error("Json Parsing Error: " + jpe.getLocalizedMessage());
                }
            }
            if(graduationData != null) {
                createAndStoreStudentTranscript(graduationData, accessToken);
                createAndStoreStudentCertificates(graduationData, accessToken);
            }
        }
        convGradStudent.setResult(result);
    }

    private void createAndStoreStudentTranscript(GraduationData graduationData,String accessToken) {
        ReportData data = reportService.prepareTranscriptData(graduationData, graduationData.getGradStatus(),accessToken);
        reportService.saveStudentTranscriptReportJasper(data, accessToken, graduationData.getGradStatus().getStudentID(), graduationData.isGraduated());
    }

    private void createAndStoreStudentCertificates(GraduationData graduationData, String accessToken) {
        List<ProgramCertificateTranscript> certificateList = reportService.getCertificateList(graduationData,accessToken);
        for (ProgramCertificateTranscript certType : certificateList) {
            reportService.saveStudentCertificateReportJasper(graduationData,accessToken, certType);
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
        studentEntity.setStudentGrade(student.getStudentGrade());
        studentEntity.setStudentStatus(getGradStudentStatus(student.getStudentStatus(), student.getArchiveFlag()));

        // flags
        if (StringUtils.equalsIgnoreCase(studentEntity.getStudentStatus(), STUDENT_STATUS_MERGED)) {
            studentEntity.setRecalculateGradStatus(null);
            studentEntity.setRecalculateProjectedGrad(null);
        } else {
            studentEntity.setRecalculateGradStatus("Y");
            studentEntity.setRecalculateProjectedGrad("Y");
        }

        // Mappings with Student_Master
        studentEntity.setFrenchCert(student.getFrenchCert());
        studentEntity.setConsumerEducationRequirementMet(student.getConsumerEducationRequirementMet());
        studentEntity.setStudentCitizenship(StringUtils.isBlank(student.getStudentCitizenship())? "U" : student.getStudentCitizenship());
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
        studentEntity.setStudentGrade(student.getStudentGrade());
        studentEntity.setStudentStatus(getGradStudentStatus(student.getStudentStatus(), student.getArchiveFlag()));

        // flags
        studentEntity.setRecalculateGradStatus(null);
        studentEntity.setRecalculateProjectedGrad(null);

        // Mappings with Student_Master
        studentEntity.setFrenchCert(student.getFrenchCert());
        studentEntity.setEnglishCert(student.getEnglishCert());
        studentEntity.setConsumerEducationRequirementMet(student.getConsumerEducationRequirementMet());
        studentEntity.setStudentCitizenship(StringUtils.isBlank(student.getStudentCitizenship())? "U" : student.getStudentCitizenship());
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
        gradStatus.setLastUpdateDate(DateConversionUtils.convertStringToDate(transcriptStudentDemog.getUpdateDate().toString()));
        graduationData.setGradStatus(gradStatus);

        // school
        School school = restUtils.getSchoolGrad(transcriptStudentDemog.getMincode(), summary.getAccessToken());
        graduationData.setSchool(school);

        // graduated Student
        GradSearchStudent gradStudent = populateGraduateStudentInfo(studentEntity, penStudent, school);
        graduationData.setGradStudent(gradStudent);

        // TSW_TRAN_CRSE
        List<TranscriptStudentCourse> transcriptStudentCourses = retrieveTswStudentCourses(student.getPen(), summary.getAccessToken());

        // studentCourses
        List<ca.bc.gov.educ.api.dataconversion.model.tsw.StudentCourse> studentCourseList = buildStudentCourses(transcriptStudentCourses.stream().filter(c -> !c.getReportType().equals("3")).collect(Collectors.toList()), studentEntity.getProgram(), summary.getAccessToken());
        StudentCourses studentCourses = new StudentCourses();
        studentCourses.setStudentCourseList(studentCourseList);
        graduationData.setStudentCourses(studentCourses);

        // studentAssessments
        List<ca.bc.gov.educ.api.dataconversion.model.tsw.StudentAssessment> studentAssessmentList = buildStudentAssessments(transcriptStudentCourses.stream().filter(c -> c.getReportType().equals("3")).collect(Collectors.toList()), studentEntity.getProgram(), summary.getAccessToken());
        StudentAssessments studentAssessments = new StudentAssessments();
        studentAssessments.setStudentAssessmentList(studentAssessmentList);
        graduationData.setStudentAssessments(studentAssessments);

        // optionalGradStatus
        List<GradAlgorithmOptionalStudentProgram> optionalGradStatus = buildOptionalGradStatus(studentEntity, studentCourseList, summary);
        graduationData.setOptionalGradStatus(optionalGradStatus);

        // requirements met
        List<GradRequirement> requirementsMet = buildRequirementsMet(studentEntity.getProgram(), transcriptStudentCourses, summary);
        graduationData.setRequirementsMet(requirementsMet);

        // gradMessage
        String gradMessage = transcriptStudentDemog.getGradMessage();
        if ("1950".equalsIgnoreCase(studentEntity.getProgram())
            && "AD".equalsIgnoreCase(studentEntity.getStudentGrade())
            && StringUtils.isNotBlank(gradMessage)
            && StringUtils.contains(gradMessage, TSW_PF_GRAD_MSG)) {
            gradMessage = StringUtils.remove(gradMessage, TSW_PF_GRAD_MSG).trim();
        }
        graduationData.setGradMessage(gradMessage);

        // dualDogwood
        graduationData.setDualDogwood(studentEntity.isDualDogwood());

        // gradProgram
        graduationData.setGradProgram(retrieveGradProgram(studentEntity.getProgram(), summary.getAccessToken()));

        // graduated
        graduationData.setGraduated(true);

        return graduationData;
    }

    private GradSearchStudent populateGraduateStudentInfo(GraduationStudentRecordEntity studentEntity,
                                                          Student penStudent,
                                                          School school) {
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
        gradSearchStudent.setSchoolOfRecord(school.getMinCode());
        gradSearchStudent.setSchoolOfRecordName(school.getSchoolName());
        gradSearchStudent.setSchoolOfRecordindependentAffiliation(school.getIndependentAffiliation());

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

    private List<ca.bc.gov.educ.api.dataconversion.model.tsw.StudentCourse> buildStudentCourses(List<TranscriptStudentCourse> tswStudentCourse, String graduationProgramCode, String accessToken) {
        List<ca.bc.gov.educ.api.dataconversion.model.tsw.StudentCourse> studentCourses = new ArrayList<>();
        for (TranscriptStudentCourse tswCourse : tswStudentCourse) {
            ca.bc.gov.educ.api.dataconversion.model.tsw.StudentCourse studentCourse = populateStudentCourse(tswCourse, graduationProgramCode, accessToken);
            studentCourses.add(studentCourse);
        }

        return studentCourses;
    }

    private List<ca.bc.gov.educ.api.dataconversion.model.tsw.StudentAssessment> buildStudentAssessments(List<TranscriptStudentCourse> tswStudentCourse, String graduationProgramCode, String accessToken) {
        List<ca.bc.gov.educ.api.dataconversion.model.tsw.StudentAssessment> studentAssessments = new ArrayList<>();
        for (TranscriptStudentCourse tswCourse : tswStudentCourse) {
            ca.bc.gov.educ.api.dataconversion.model.tsw.StudentAssessment studentAssessment = populateStudentAssessment(tswCourse, graduationProgramCode, accessToken);
            studentAssessments.add(studentAssessment);
        }

        return studentAssessments;
    }

    private ca.bc.gov.educ.api.dataconversion.model.tsw.StudentCourse populateStudentCourse(TranscriptStudentCourse tswCourse, String graduationProgramCode, String accessToken) {
        ca.bc.gov.educ.api.dataconversion.model.tsw.StudentCourse result = ca.bc.gov.educ.api.dataconversion.model.tsw.StudentCourse.builder()
                .pen(tswCourse.getStudNo())
                .courseCode(tswCourse.getCourseCode())
                .courseLevel(tswCourse.getCourseLevel())
                .courseName(tswCourse.getCourseName())
                .originalCredits(EducGradDataConversionApiUtils.getNumberOfCredits(tswCourse.getNumberOfCredits()))
                .credits(EducGradDataConversionApiUtils.getNumberOfCredits(tswCourse.getNumberOfCredits()))
                .creditsUsedForGrad(EducGradDataConversionApiUtils.getNumberOfCredits(tswCourse.getNumberOfCredits()))
                .sessionDate(tswCourse.getCourseSession())
                .completedCoursePercentage(EducGradDataConversionApiUtils.getPercentage(tswCourse.getFinalPercentage()))
                .completedCourseLetterGrade(tswCourse.getFinalLG() != null? tswCourse.getFinalLG().trim() : null)
                .schoolPercent(EducGradDataConversionApiUtils.getPercentage(tswCourse.getSchoolPercentage()))
                .bestSchoolPercent(EducGradDataConversionApiUtils.getPercentage(tswCourse.getSchoolPercentage()))
                .examPercent(EducGradDataConversionApiUtils.getPercentage(tswCourse.getExamPercentage()))
                .bestExamPercent(EducGradDataConversionApiUtils.getPercentage(tswCourse.getExamPercentage()))
                .hasRelatedCourse("N")
                .metLitNumRequirement(tswCourse.getMetLitNumReqt())
                .relatedCourse(StringUtils.isNotBlank(tswCourse.getRelatedCourse())? tswCourse.getRelatedCourse().trim() : null)
                .relatedLevel(StringUtils.isNotBlank(tswCourse.getRelatedCourseLevel())? tswCourse.getRelatedCourseLevel().trim() : null)
                .hasRelatedCourse(StringUtils.isNotBlank(tswCourse.getRelatedCourse())? "Y" : "N")
                //.genericCourseType("") // tsw course name is used
                //.relatedCourseName("")// tsw course name is used
                .provExamCourse(StringUtils.equals(tswCourse.getReportType(), "1")? "Y" : "N")
                .isUsed(StringUtils.isNotBlank(tswCourse.getUsedForGrad()))
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

        ProgramRequirement rule = getProgramRequirement(graduationProgramCode, tswCourse.getFoundationReq(), accessToken);
        if (rule != null) {
            // old trax requirement code is used instead of new requirement code, rule.getProgramRequirementCode().getProReqCode()
            result.setGradReqMet(tswCourse.getFoundationReq());
            result.setGradReqMetDetail(rule.getProgramRequirementCode().getLabel());
        }

        // Final Percentage
        SpecialCase sc = handleSpecialCase(tswCourse.getFinalPercentage(), accessToken);
        if (sc != null) {
            result.setSpecialCase(sc.getSpCase());
        }
        return result;
    }

    private ca.bc.gov.educ.api.dataconversion.model.tsw.StudentAssessment populateStudentAssessment(TranscriptStudentCourse tswCourse, String graduationProgramCode, String accessToken) {
        ca.bc.gov.educ.api.dataconversion.model.tsw.StudentAssessment result = ca.bc.gov.educ.api.dataconversion.model.tsw.StudentAssessment.builder()
                .pen(tswCourse.getStudNo())
                .assessmentCode(tswCourse.getCourseCode())
                .assessmentName(tswCourse.getCourseName())
                .sessionDate(tswCourse.getCourseSession())
                .proficiencyScore(EducGradDataConversionApiUtils.getPercentage(tswCourse.getFinalPercentage()))
                .isUsed(StringUtils.isNotBlank(tswCourse.getUsedForGrad())) // usedForGrad has some credits or not
                .isProjected(false)
                .isDuplicate(false)
                .isFailed(false)
                .isNotCompleted(false)
            .build();

        ProgramRequirement rule = getProgramRequirement(graduationProgramCode, tswCourse.getFoundationReq(), accessToken);
        if (rule != null) {
            // old trax requirement code is used instead of new requirement code, rule.getProgramRequirementCode().getProReqCode()
            result.setGradReqMet(tswCourse.getFoundationReq());
            result.setGradReqMetDetail(rule.getProgramRequirementCode().getLabel());
        }

        if (StringUtils.isNotBlank(tswCourse.getSpecialCase())) {
            // SpecialCase
            SpecialCase sc = lookupSpecialCase(tswCourse.getSpecialCase().trim(), accessToken);
            if (sc != null) {
                result.setSpecialCase(sc.getSpCase());
            }
        }

        // Final Percentage
        SpecialCase sc = handleSpecialCase(tswCourse.getFinalPercentage(), accessToken);
        if (sc != null) {
            result.setSpecialCase(sc.getSpCase());
        }
        return result;
    }

    // SpecialCase
    private SpecialCase handleSpecialCase(String percentage, String accessToken) {
        if (StringUtils.isNotBlank(percentage)
                && !NumberUtils.isCreatable(percentage.trim())
                && !StringUtils.equals(percentage.trim(), "---")) {
            return lookupSpecialCase(percentage.trim(), accessToken);
        }
        return null;
    }


    private ProgramRequirement getProgramRequirement(String graduationProgramCode, String foundationReq, String accessToken) {
        if (StringUtils.isBlank(graduationProgramCode) || StringUtils.isBlank(foundationReq)) {
            return null;
        }
        return lookupProgramRule(graduationProgramCode, foundationReq, accessToken);
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
                result.setOptionalProgramName(optionalProgram.getOptionalProgramName());
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
            program = restUtils.getGradProgramCode(programCode, accessToken);
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

        // French Immersion for 2018-EN, 2004-EN, 1996-EN, 1986-EN
        if (hasAnyFrenchImmersionCourse(student.getProgram(), student.getPen(), accessToken)) {
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

    public boolean hasAnyFrenchImmersionCourse(String program, String pen, String accessToken) {
        boolean frenchImmersion = false;
        switch(program) {
            case "2018-EN":
            case "2004-EN":
                if (courseService.isFrenchImmersionCourse(pen, "10", accessToken)) { // FRAL 10 or FRALP 10
                    frenchImmersion = true;
                }
                break;
            case "1996-EN":
                if (courseService.isFrenchImmersionCourse(pen, "11", accessToken)) { // FRAL 11 or FRALP 11
                    frenchImmersion = true;
                }
                break;
            case "1986-EN":
                if (courseService.isFrenchImmersionCourseForEN(pen, "11", accessToken)) { // FRAL 11
                    frenchImmersion = true;
                }
                break;
            default:
                break;
        }
        return frenchImmersion;
    }

    private ConversionResultType processProgramCodes(GraduationStudentRecordEntity student, List<String> programCodes, String accessToken, ConversionStudentSummaryDTO summary) {
        ConversionResultType resultType = ConversionResultType.SUCCESS;
        Boolean isCareerProgramCreated = Boolean.FALSE;
        if (StringUtils.isNotBlank(student.getProgram()) && !programCodes.isEmpty()) {
            for (String programCode : programCodes) {
                Pair<ConversionResultType, Boolean> res = handleProgramCode(programCode, student, accessToken, summary);
                if (Boolean.TRUE.equals(res.getRight())) {
                    isCareerProgramCreated = Boolean.TRUE;
                }
                resultType = res.getLeft();
                if (resultType == ConversionResultType.FAILURE) {
                    break;
                }
            }
            if (Boolean.TRUE.equals(isCareerProgramCreated)) {
                resultType = createStudentOptionalProgram("CP", student, accessToken, summary);
            }
        }
        return resultType;
    }

    private Pair<ConversionResultType, Boolean> handleProgramCode(String programCode, GraduationStudentRecordEntity student, String accessToken, ConversionStudentSummaryDTO summary) {
        ConversionResultType resultType;
        boolean isCareerProgramCreated = false;
        if (isOptionalProgramCode(programCode)) {
            resultType = createStudentOptionalProgram(programCode, student, accessToken, summary);
        } else {
            resultType = createStudentCareerProgram(programCode, student, summary);
            if (resultType == ConversionResultType.SUCCESS) {
                isCareerProgramCreated = true;
            }
        }
        return Pair.of(resultType, isCareerProgramCreated);
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
            handleException(null, summary, student.getPen(), ConversionResultType.WARNING, "Grad Program API is failed to retrieve Optional Program [" + optionalProgramCode + "] - " + e.getLocalizedMessage());
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
            handleException(null, summary, student.getPen(), ConversionResultType.WARNING, "Grad Program API is failed to retrieve Career Program [" + careerProgramCode + "] - " + e.getLocalizedMessage());
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
            handleException(null, summary, student.getPen(), ConversionResultType.WARNING, "Career Program Code does not exist: " + careerProgramCode);
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
        Student penStudent;
        // PEN Student
        try {
            // Call PEN Student API
            List<Student> students = restUtils.getStudentsByPen(pen, accessToken);
            penStudent = students.stream().filter(s -> s.getPen().compareTo(pen) == 0).findAny().orElse(null);
        } catch (Exception e) {
            log.error("PEN Student API is failed for pen[{}] : {} ", pen, e.getLocalizedMessage());
            return null;
        }

        if (penStudent == null) {
            log.error("Pen# [{}] is not found in PEN StudentAPI.", pen);
            return null;
        }

        UUID studentID = UUID.fromString(penStudent.getStudentID());
        StudentGradDTO studentData = new StudentGradDTO();
        studentData.setStudentID(studentID);
        // populate Demographic data
        studentData.setLastName(penStudent.getLegalLastName());
        studentData.setFirstName(penStudent.getLegalFirstName());
        studentData.setMiddleName(penStudent.getLegalMiddleNames());
        studentData.setBirthday(penStudent.getDob());

        Optional<GraduationStudentRecordEntity> gradStatusOptional = graduationStudentRecordRepository.findById(studentID);
        if (gradStatusOptional.isPresent()) {
            GraduationStudentRecordEntity entity = gradStatusOptional.get();
            studentData.setProgram(entity.getProgram());
            studentData.setStudentGrade(entity.getStudentGrade());
            studentData.setStudentStatus(entity.getStudentStatus());
            studentData.setSchoolOfRecord(entity.getSchoolOfRecord());
            studentData.setSchoolAtGrad(entity.getSchoolAtGrad());
            studentData.setCitizenship(entity.getStudentCitizenship());
        } else {
            log.error("GraduationStudentRecord is not found for pen# [{}], studentID [{}]", pen, studentID);
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
            if (StringUtils.isNotBlank(gradStudent.getNewRecalculateGradStatus())) {
                entity.setRecalculateGradStatus(gradStudent.getNewRecalculateGradStatus());
            }
            if (StringUtils.isNotBlank(gradStudent.getNewRecalculateProjectedGrad())) {
                entity.setRecalculateProjectedGrad(gradStudent.getNewRecalculateProjectedGrad());
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
        if (optional.isEmpty()) {
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
        if (optional.isEmpty()) {
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
    public void triggerGraduationBatchRun(UUID studentID, String recalculateGradStatus, String recalcualteProjectedGrad) {
        Optional<GraduationStudentRecordEntity> gradStatusOptional = graduationStudentRecordRepository.findById(studentID);
        if (gradStatusOptional.isPresent()) {
            GraduationStudentRecordEntity graduationStudentRecordEntity = gradStatusOptional.get();
            if (StringUtils.equals(graduationStudentRecordEntity.getStudentStatus(), STUDENT_STATUS_MERGED)) {
                graduationStudentRecordEntity.setRecalculateGradStatus(null);
                graduationStudentRecordEntity.setRecalculateProjectedGrad(null);
            } else {
                graduationStudentRecordEntity.setRecalculateGradStatus("Y");
                graduationStudentRecordEntity.setRecalculateProjectedGrad("Y");
            }
            graduationStudentRecordRepository.save(graduationStudentRecordEntity);
        }
    }

    public ProgramRequirement lookupProgramRule(String graduationProgramCode, String foundationReq, String accessToken) {
        programRuleMap.computeIfAbsent(graduationProgramCode, k -> restUtils.getGradProgramRules(graduationProgramCode, accessToken));

        List<ProgramRequirement> rules = programRuleMap.get(graduationProgramCode);
        return rules.stream()
                .filter(pr -> StringUtils.equals(pr.getProgramRequirementCode().getTraxReqNumber(), foundationReq))
                .findAny()
                .orElse(null);
    }

    public SpecialCase lookupSpecialCase(String specialCaseLabel, String accessToken) {
        if (!specialCaseMap.containsKey(specialCaseLabel)) {
            List<SpecialCase> results = restUtils.getAllSpecialCases(accessToken);
            results.forEach(r -> specialCaseMap.put(r.getLabel(), r));
        }

        return specialCaseMap.get(specialCaseLabel);
    }
}
