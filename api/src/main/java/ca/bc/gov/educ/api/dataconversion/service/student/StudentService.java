package ca.bc.gov.educ.api.dataconversion.service.student;

import ca.bc.gov.educ.api.dataconversion.constant.ConversionResultType;
import ca.bc.gov.educ.api.dataconversion.model.*;
import ca.bc.gov.educ.api.dataconversion.model.StudentAssessment;
import ca.bc.gov.educ.api.dataconversion.model.StudentCourse;
import ca.bc.gov.educ.api.dataconversion.model.tsw.*;
import ca.bc.gov.educ.api.dataconversion.model.tsw.report.ReportData;

import ca.bc.gov.educ.api.dataconversion.service.assessment.AssessmentService;
import ca.bc.gov.educ.api.dataconversion.service.course.CourseService;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import ca.bc.gov.educ.api.dataconversion.util.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiConstants.DEFAULT_CREATED_BY;
import static ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiConstants.DEFAULT_UPDATED_BY;

@Service
@Slf4j
public class StudentService extends StudentBaseService {

    private static final List<String> OPTIONAL_PROGRAM_CODES = Arrays.asList("AD", "BC", "BD");
    private static final String TRAX_API_ERROR_MSG = "Grad Trax API is failed for ";
    private static final String GRAD_STUDENT_API_ERROR_MSG = "Grad Student API is failed for ";
    private static final String TSW_PF_GRAD_MSG = "Student has successfully completed the Programme Francophone.";

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
    public StudentService(EducGradDataConversionApiConstants constants,
                          RestUtils restUtils,
                          AssessmentService assessmentService,
                          CourseService courseService,
                          ReportService reportService) {
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

    public ConvGradStudent convertStudent(ConvGradStudent convGradStudent, ConversionStudentSummaryDTO summary) throws Exception {
        long startTime = System.currentTimeMillis();
        summary.setProcessedCount(summary.getProcessedCount() + 1L);
        String accessToken = summary.getAccessToken();

        // School validation
        validateSchool(convGradStudent, summary);
        if (ConversionResultType.FAILURE == convGradStudent.getResult()) { // Grad Trax API is failed
            return convGradStudent;
        }

        // PEN Student
        List<Student> students = getStudentsFromPEN(convGradStudent, summary);
        if (ConversionResultType.FAILURE == convGradStudent.getResult()) { // PEN Student API is failed
            return convGradStudent;
        } else if (students == null || students.isEmpty()) {
            handleException(convGradStudent, summary, convGradStudent.getPen(), ConversionResultType.FAILURE, "PEN does not exist: PEN Student API returns empty response.");
            return convGradStudent;
        }

        // Student conversion process
        processStudents(convGradStudent, students, summary, accessToken);

        long diff = (System.currentTimeMillis() - startTime) / 1000L;
        log.info("************* TIME Taken for pen [{}]  ************ {} secs", convGradStudent.getPen(), diff);
        return convGradStudent;
    }

    private void validateSchool(ConvGradStudent convGradStudent, ConversionStudentSummaryDTO summary) {
        // School Category Code form School API
        if (convGradStudent.isGraduated() && StringUtils.isBlank(convGradStudent.getTranscriptSchoolCategoryCode())) {
            handleException(convGradStudent, summary, convGradStudent.getPen(), ConversionResultType.FAILURE, "School does not exist in SPM School data : mincode [" + convGradStudent.getSchoolOfRecord() + "]");
            return;
        }
        // TRAX School validation
        if (convGradStudent.getTranscriptSchool() == null) {
            handleException(convGradStudent, summary, convGradStudent.getPen(), ConversionResultType.FAILURE, "Invalid school of record " + convGradStudent.getSchoolOfRecord());
            return;
        }
//        try {
//            schoolExists = restUtils.checkSchoolExists(convGradStudent.getSchoolOfRecord(), summary.getAccessToken());
//        } catch (Exception e) {
//            handleException(convGradStudent, summary, convGradStudent.getPen(), ConversionResultType.FAILURE, TRAX_API_ERROR_MSG + "validating school existence : " + e.getLocalizedMessage());
//        }
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
            GraduationStudentRecord gradStudent = processStudent(convGradStudent, st, summary);
            if (gradStudent != null) {
                processDependencies(convGradStudent, gradStudent, st, summary, accessToken);
            }

            if (convGradStudent.getResult() == null) {
                convGradStudent.setResult(ConversionResultType.SUCCESS);
            }
        });
    }

    private GraduationStudentRecord processStudent(ConvGradStudent convGradStudent, Student penStudent, ConversionStudentSummaryDTO summary) {
        UUID studentID = UUID.fromString(penStudent.getStudentID());
        GraduationStudentRecord gradStudent = null;
        try {
            gradStudent = restUtils.getStudentGradStatus(penStudent.getStudentID(), summary.getAccessToken());
        } catch (Exception e) {
            handleException(convGradStudent, summary, convGradStudent.getPen(), ConversionResultType.FAILURE, GRAD_STUDENT_API_ERROR_MSG + "getting a GraduationStudentRecord : " + e.getLocalizedMessage());
            return null;
        }
        if (gradStudent != null) { // update
            gradStudent.setPen(penStudent.getPen());
            if (convGradStudent.isGraduated()) {
                convertGraduatedStudentData(convGradStudent, penStudent, gradStudent, summary);
            } else {
                convertStudentData(convGradStudent, penStudent, gradStudent, summary);
            }
            if (ConversionResultType.FAILURE != convGradStudent.getResult()) {
                gradStudent.setUpdateDate(null);
                gradStudent.setUpdateUser(null);
                try {
                    gradStudent = restUtils.saveStudentGradStatus(penStudent.getStudentID(), gradStudent, false, summary.getAccessToken());
                } catch (Exception e) {
                    handleException(convGradStudent, summary, convGradStudent.getPen(), ConversionResultType.FAILURE, GRAD_STUDENT_API_ERROR_MSG + "saving a GraduationStudentRecord : " + e.getLocalizedMessage());
                    return null;
                }
                summary.setUpdatedCount(summary.getUpdatedCount() + 1L);
            }
        } else { // create
            gradStudent = new GraduationStudentRecord();
            gradStudent.setPen(penStudent.getPen());
            gradStudent.setStudentID(studentID);
            if (convGradStudent.isGraduated()) {
                convertGraduatedStudentData(convGradStudent, penStudent, gradStudent, summary);
            } else {
                convertStudentData(convGradStudent, penStudent, gradStudent, summary);
            }
            if (ConversionResultType.FAILURE != convGradStudent.getResult()) {
                try {
                    gradStudent = restUtils.saveStudentGradStatus(penStudent.getStudentID(), gradStudent, false, summary.getAccessToken());
                } catch (Exception e) {
                    handleException(convGradStudent, summary, convGradStudent.getPen(), ConversionResultType.FAILURE, GRAD_STUDENT_API_ERROR_MSG + "saving a GraduationStudentRecord : " + e.getLocalizedMessage());
                    return null;
                }
                summary.setAddedCount(summary.getAddedCount() + 1L);
            }
        }
        if (ConversionResultType.FAILURE != convGradStudent.getResult()) {
            return gradStudent;
        } else {
            return null;
        }
    }

    private void processDependencies(ConvGradStudent convGradStudent,
                                     GraduationStudentRecord gradStudent,
                                     Student penStudent,
                                     ConversionStudentSummaryDTO summary, String accessToken) {
        ConversionResultType result;

        // process dependencies
        gradStudent.setPen(convGradStudent.getPen());
        if (convGradStudent.isGraduated()) {
            result = processOptionalProgramsForGraduatedStudent(gradStudent, accessToken, summary);
        } else {
            result = processOptionalPrograms(gradStudent, accessToken, summary);
        }

        if (ConversionResultType.FAILURE != result) {
            result = processProgramCodes(gradStudent, convGradStudent.getProgramCodes(), convGradStudent.isGraduated(), accessToken, summary);
        }
        if (ConversionResultType.FAILURE != result) {
            result = processSccpFrenchCertificates(gradStudent, convGradStudent.isGraduated(), accessToken, summary);
        }

        if (convGradStudent.isGraduated() && !StringUtils.equalsIgnoreCase(gradStudent.getStudentStatus(), STUDENT_STATUS_MERGED)) {
            // Building GraduationData CLOB data
            GraduationData graduationData = buildGraduationData(convGradStudent, gradStudent, penStudent, summary);
            try {
                gradStudent.setStudentGradData(JsonUtil.getJsonStringFromObject(graduationData));
            } catch (JsonProcessingException jpe) {
                log.error("Json Parsing Error for StudentGradData: " + jpe.getLocalizedMessage());
            }
            try {
                restUtils.saveStudentGradStatus(penStudent.getStudentID(), gradStudent, false, summary.getAccessToken());
            } catch (Exception e) {
                handleException(convGradStudent, summary, convGradStudent.getPen(), ConversionResultType.FAILURE, GRAD_STUDENT_API_ERROR_MSG + "updating a GraduationStudentRecord with clob data : " + e.getLocalizedMessage());
            }
            if (convGradStudent.getDistributionDate() == null) {
                Date distributionDate = null;
                if (!"SCCP".equalsIgnoreCase(convGradStudent.getProgram())) {
                    distributionDate = DateConversionUtils.getLastDayOfMonth(convGradStudent.getProgramCompletionDate());
                } else {
                    distributionDate = DateConversionUtils.convertStringToDate(convGradStudent.getSccDate());
                }
                convGradStudent.setDistributionDate(distributionDate);
            }
            createAndStoreStudentTranscript(graduationData, convGradStudent, accessToken);
            createAndStoreStudentCertificates(graduationData, convGradStudent, accessToken);
        }
        convGradStudent.setResult(result);
    }

    private void createAndStoreStudentTranscript(GraduationData graduationData, ConvGradStudent convStudent, String accessToken) {
        ReportData data = reportService.prepareTranscriptData(graduationData, graduationData.getGradStatus(), convStudent, accessToken);
        reportService.saveStudentTranscriptReportJasper(data, convStudent.getDistributionDate(), accessToken, graduationData.getGradStatus().getStudentID(), graduationData.isGraduated());
    }

    private void createAndStoreStudentCertificates(GraduationData graduationData, ConvGradStudent convStudent, String accessToken) {
        List<ProgramCertificateTranscript> certificateList = reportService.getCertificateList(graduationData,
            convStudent.getCertificateSchoolCategoryCode() != null? convStudent.getCertificateSchoolCategoryCode() : convStudent.getTranscriptSchoolCategoryCode(), accessToken);
        for (ProgramCertificateTranscript certType : certificateList) {
            reportService.saveStudentCertificateReportJasper(graduationData, convStudent, accessToken, certType);
        }
    }

    private void convertStudentData(ConvGradStudent student, Student penStudent, GraduationStudentRecord gradStudent, ConversionStudentSummaryDTO summary) {
        if (determineProgram(student, summary)) {
            gradStudent.setProgram(student.getProgram());
        } else {
            return;
        }

        gradStudent.setGpa(null);
        gradStudent.setHonoursStanding(null);
        if ("SCCP".equalsIgnoreCase(gradStudent.getProgram())) {
            if (!validateAndSetSlpDate(student, gradStudent, summary)) {
                return;
            }
        } else {
            gradStudent.setProgramCompletionDate(null);
        }
        gradStudent.setStudentGradData(null);
        gradStudent.setSchoolAtGrad(null);

        gradStudent.setSchoolOfRecord(StringUtils.isNotBlank(student.getSchoolOfRecord())? student.getSchoolOfRecord() : null);
        gradStudent.setStudentGrade(student.getStudentGrade());
        gradStudent.setStudentStatus(getGradStudentStatus(student.getStudentStatus(), student.getArchiveFlag()));

        handleAdult19Rule(student, penStudent, gradStudent);

        // flags
        if (StringUtils.equalsIgnoreCase(gradStudent.getStudentStatus(), STUDENT_STATUS_MERGED)) {
            gradStudent.setRecalculateGradStatus(null);
            gradStudent.setRecalculateProjectedGrad(null);
        } else {
            gradStudent.setRecalculateGradStatus("Y");
            gradStudent.setRecalculateProjectedGrad("Y");
        }

        // Mappings with Student_Master
        gradStudent.setFrenchCert(student.getFrenchCert());
        gradStudent.setConsumerEducationRequirementMet(student.getConsumerEducationRequirementMet());
        gradStudent.setStudentCitizenship(StringUtils.isBlank(student.getStudentCitizenship())? "U" : student.getStudentCitizenship());

        // extra
        gradStudent.setCreateUser(DEFAULT_CREATED_BY);
        gradStudent.setUpdateUser(DEFAULT_UPDATED_BY);
    }

    private void convertGraduatedStudentData(ConvGradStudent student, Student penStudent, GraduationStudentRecord gradStudent, ConversionStudentSummaryDTO summary) {
        if (determineProgram(student, summary)) {
            gradStudent.setProgram(student.getProgram());
        } else {
            return;
        }

        gradStudent.setGpa(student.getGpa());
        gradStudent.setHonoursStanding(student.getHonoursStanding());
        if ("SCCP".equalsIgnoreCase(gradStudent.getProgram())) {
            if (!validateAndSetSlpDate(student, gradStudent, summary)) {
                return;
            }
        } else {
            gradStudent.setProgramCompletionDate(EducGradDataConversionApiUtils.formatDate(student.getProgramCompletionDate()));
        }
        gradStudent.setSchoolAtGrad(StringUtils.isNotBlank(student.getSchoolAtGrad())? student.getSchoolAtGrad() : null);
        gradStudent.setSchoolOfRecord(StringUtils.isNotBlank(student.getSchoolOfRecord())? student.getSchoolOfRecord() : null);
        gradStudent.setStudentGrade(student.getStudentGrade());
        gradStudent.setStudentStatus(getGradStudentStatus(student.getStudentStatus(), student.getArchiveFlag()));

        handleAdult19Rule(student, penStudent, gradStudent);

        // flags
        gradStudent.setRecalculateGradStatus(null);
        gradStudent.setRecalculateProjectedGrad(null);

        // Mappings with Student_Master
        gradStudent.setFrenchCert(student.getFrenchCert());
        gradStudent.setEnglishCert(student.getEnglishCert());
        gradStudent.setConsumerEducationRequirementMet(student.getConsumerEducationRequirementMet());
        gradStudent.setStudentCitizenship(StringUtils.isBlank(student.getStudentCitizenship())? "U" : student.getStudentCitizenship());

        // extra
        gradStudent.setCreateUser(DEFAULT_CREATED_BY);
        gradStudent.setUpdateUser(DEFAULT_UPDATED_BY);
    }

    private GraduationData buildGraduationData(ConvGradStudent student, GraduationStudentRecord gradStudent, Student penStudent, ConversionStudentSummaryDTO summary) {
        GraduationData graduationData = new GraduationData();

        TranscriptStudentDemog transcriptStudentDemog = student.getTranscriptStudentDemog();

        // gradStatus
        GradAlgorithmGraduationStudentRecord gradStatus = new GradAlgorithmGraduationStudentRecord();
        gradStatus.setStudentID(gradStudent.getStudentID());
        gradStatus.setProgram(gradStudent.getProgram());
        gradStatus.setProgramCompletionDate(gradStudent.getProgramCompletionDate());
        gradStatus.setGpa(gradStudent.getGpa());
        gradStatus.setSchoolOfRecord(gradStudent.getSchoolOfRecord());
        gradStatus.setStudentGrade(gradStudent.getStudentGrade());
        gradStatus.setStudentStatus(gradStudent.getStudentStatus());
        gradStatus.setSchoolAtGrad(gradStudent.getSchoolAtGrad());
        gradStatus.setHonoursStanding(gradStudent.getHonoursStanding());
        gradStatus.setRecalculateGradStatus(gradStudent.getRecalculateGradStatus());
        gradStatus.setLastUpdateDate(DateConversionUtils.convertStringToDate(transcriptStudentDemog.getUpdateDate().toString()));
        graduationData.setGradStatus(gradStatus);

        // school
        graduationData.setSchool(student.getTranscriptSchool());

        // graduated Student
        GradSearchStudent gradSearchStudent = populateGraduateStudentInfo(gradStudent, penStudent, student.getTranscriptSchool());
        graduationData.setGradStudent(gradSearchStudent);

        // TSW_TRAN_CRSE
        List<TranscriptStudentCourse> transcriptStudentCourses = student.getTranscriptStudentCourses();

        // studentCourses
        List<ca.bc.gov.educ.api.dataconversion.model.tsw.StudentCourse> studentCourseList =
                buildStudentCourses(transcriptStudentCourses.stream().filter(c -> !c.getReportType().equals("3")).collect(Collectors.toList()),
                gradStudent.getProgram(), student.getStudentGrade(), summary.getAccessToken());
        StudentCourses studentCourses = new StudentCourses();
        studentCourses.setStudentCourseList(studentCourseList);
        graduationData.setStudentCourses(studentCourses);

        // studentAssessments
        List<ca.bc.gov.educ.api.dataconversion.model.tsw.StudentAssessment> studentAssessmentList = buildStudentAssessments(transcriptStudentCourses.stream().filter(c -> c.getReportType().equals("3")).collect(Collectors.toList()), gradStudent.getProgram(), summary.getAccessToken());
        StudentAssessments studentAssessments = new StudentAssessments();
        studentAssessments.setStudentAssessmentList(studentAssessmentList);
        graduationData.setStudentAssessments(studentAssessments);

        // optionalGradStatus
        List<GradAlgorithmOptionalStudentProgram> optionalGradStatus = buildOptionalGradStatus(student, gradStudent, studentCourseList, summary);
        graduationData.setOptionalGradStatus(optionalGradStatus);

        // requirements met
        List<GradRequirement> requirementsMet = buildRequirementsMet(gradSearchStudent.getProgram(), transcriptStudentCourses, summary);
        graduationData.setRequirementsMet(requirementsMet);

        // gradMessage
        String gradMessage = transcriptStudentDemog.getGradMessage();
        if ("1950".equalsIgnoreCase(gradSearchStudent.getProgram())
            && StringUtils.isNotBlank(gradMessage)
            && StringUtils.contains(gradMessage, TSW_PF_GRAD_MSG)) {
            gradMessage = StringUtils.remove(gradMessage, TSW_PF_GRAD_MSG).trim();
        }
        graduationData.setGradMessage(gradMessage);

        // dualDogwood
        graduationData.setDualDogwood(gradStudent.isDualDogwood());

        // gradProgram
        graduationData.setGradProgram(retrieveGradProgram(gradStudent.getProgram(), summary.getAccessToken()));

        // graduated
        graduationData.setGraduated(true);

        return graduationData;
    }

    private GradSearchStudent populateGraduateStudentInfo(GraduationStudentRecord gradStudent,
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

        gradSearchStudent.setProgram(gradStudent.getProgram());
        gradSearchStudent.setStudentGrade(gradStudent.getStudentGrade());
        gradSearchStudent.setStudentStatus(gradStudent.getStudentStatus());
        gradSearchStudent.setSchoolOfRecord(school.getMinCode());
        gradSearchStudent.setSchoolOfRecordName(school.getSchoolName());
        gradSearchStudent.setSchoolOfRecordindependentAffiliation(school.getIndependentAffiliation());

        return gradSearchStudent;
    }

    private List<ca.bc.gov.educ.api.dataconversion.model.tsw.StudentCourse> buildStudentCourses(List<TranscriptStudentCourse> tswStudentCourse, String graduationProgramCode, String studentGrade, String accessToken) {
        List<ca.bc.gov.educ.api.dataconversion.model.tsw.StudentCourse> studentCourses = new ArrayList<>();
        for (TranscriptStudentCourse tswCourse : tswStudentCourse) {
            ca.bc.gov.educ.api.dataconversion.model.tsw.StudentCourse studentCourse = populateStudentCourse(tswCourse, graduationProgramCode, studentGrade, accessToken);
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

    private ca.bc.gov.educ.api.dataconversion.model.tsw.StudentCourse populateStudentCourse(TranscriptStudentCourse tswCourse, String graduationProgramCode, String studentGrade, String accessToken) {
        Integer credits = EducGradDataConversionApiUtils.getNumberOfCredits(tswCourse.getNumberOfCredits());
        Integer creditsUsedForGrad = EducGradDataConversionApiUtils.getNumberOfCredits(tswCourse.getUsedForGrad());
        boolean isAdultOrSccp = isAdultOrSccp(graduationProgramCode, studentGrade);

        ca.bc.gov.educ.api.dataconversion.model.tsw.StudentCourse result = ca.bc.gov.educ.api.dataconversion.model.tsw.StudentCourse.builder()
                .pen(tswCourse.getStudNo())
                .courseCode(tswCourse.getCourseCode())
                .courseLevel(tswCourse.getCourseLevel())
                .courseName(tswCourse.getCourseName())
                .originalCredits(credits)
                .credits(credits)
                .creditsUsedForGrad(isAdultOrSccp? null : creditsUsedForGrad)
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
                .specialCase(StringUtils.isNotBlank(tswCourse.getSpecialCase())? tswCourse.getSpecialCase().trim() : null)
                .provExamCourse(StringUtils.equals(tswCourse.getReportType(), "1")? "Y" : "N")
                .isUsed(isAdultOrSccp? true : creditsUsedForGrad != null && creditsUsedForGrad.intValue() != 0)
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
            result.setGradReqMetDetail(StringUtils.isNotBlank(tswCourse.getFoundationReq())?
                    tswCourse.getFoundationReq().trim() + " - " + rule.getProgramRequirementCode().getLabel() : rule.getProgramRequirementCode().getLabel());
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
                .specialCase(StringUtils.isNotBlank(tswCourse.getSpecialCase())? tswCourse.getSpecialCase().trim() : null)
                .isUsed(StringUtils.isNotBlank(tswCourse.getFoundationReq()))
                .isProjected(false)
                .isDuplicate(false)
                .isFailed(false)
                .isNotCompleted(false)
            .build();

        ProgramRequirement rule = getProgramRequirement(graduationProgramCode, tswCourse.getFoundationReq(), accessToken);
        if (rule != null) {
            // old trax requirement code is used instead of new requirement code, rule.getProgramRequirementCode().getProReqCode()
            result.setGradReqMet(tswCourse.getFoundationReq());
            result.setGradReqMetDetail(StringUtils.isNotBlank(tswCourse.getFoundationReq())?
                tswCourse.getFoundationReq().trim() + " - " + rule.getProgramRequirementCode().getLabel() : rule.getProgramRequirementCode().getLabel());
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

    private List<GradAlgorithmOptionalStudentProgram> buildOptionalGradStatus(ConvGradStudent student, GraduationStudentRecord gradStudent,
              List<ca.bc.gov.educ.api.dataconversion.model.tsw.StudentCourse> studentCourseList, ConversionStudentSummaryDTO summary) {
        List<GradAlgorithmOptionalStudentProgram> results = new ArrayList<>();
        if (student.getProgramCodes() == null || student.getProgramCodes().isEmpty()) {
            return results;
        }

        List<StudentOptionalProgram> list = null;
        try {
            list = restUtils.getStudentOptionalPrograms(gradStudent.getStudentID().toString(), summary.getAccessToken());
        } catch (Exception e) {
            handleException(student, summary, student.getPen(), ConversionResultType.FAILURE, GRAD_STUDENT_API_ERROR_MSG + "getting StudentOptionalPrograms : " + e.getLocalizedMessage());
        }
        if (list != null && !list.isEmpty()) {
            for (StudentOptionalProgram obj : list) {
                GradAlgorithmOptionalStudentProgram result = populateOptionStudentProgramStatus(student, obj, summary);
                StudentCourses studentCourses = new StudentCourses();
                studentCourses.setStudentCourseList(studentCourseList);
                result.setOptionalStudentCourses(studentCourses);
                results.add(result);
            }
        }
        return results;

    }

    private GradAlgorithmOptionalStudentProgram populateOptionStudentProgramStatus(ConvGradStudent student, StudentOptionalProgram object, ConversionStudentSummaryDTO summary) {
        GradAlgorithmOptionalStudentProgram result = new GradAlgorithmOptionalStudentProgram();
        result.setOptionalProgramID(object.getOptionalProgramID());
        result.setOptionalProgramCode(object.getOptionalProgramCode());
        result.setOptionalProgramName(object.getOptionalProgramName());
        result.setOptionalProgramCompletionDate(object.getOptionalProgramCompletionDate());
        result.setOptionalRequirementsMet(new ArrayList<>());
        result.setStudentID(object.getStudentID());
        result.setOptionalGraduated(true);
        try {
            object.setStudentOptionalProgramData(new ObjectMapper().writeValueAsString(result));
        } catch (JsonProcessingException e) {
            log.error("Json Parsing Error for StudentOptionalProgramData: " + e.getLocalizedMessage());
        }
        result.setCpList(buildStudentCareerProgramList(student, object, summary));
        updateStudentOptionalProgram(object, summary.getAccessToken());
        return result;
    }

    private void updateStudentOptionalProgram(StudentOptionalProgram object, String accessToken) {
        StudentOptionalProgramRequestDTO requestDTO = new StudentOptionalProgramRequestDTO();
        BeanUtils.copyProperties(object, requestDTO);
        restUtils.saveStudentOptionalProgram(requestDTO, accessToken);
    }

    private List<StudentCareerProgram> buildStudentCareerProgramList(ConvGradStudent student, StudentOptionalProgram object, ConversionStudentSummaryDTO summary) {
        List<StudentCareerProgram> results = null;
        try {
            results = restUtils.getStudentCareerPrograms(object.getStudentID().toString(), summary.getAccessToken());
        } catch (Exception e) {
            handleException(student, summary, student.getPen(), ConversionResultType.FAILURE, GRAD_STUDENT_API_ERROR_MSG + "getting StudentCareerPrograms : " + e.getLocalizedMessage());
        }
        if (results != null) {
            return results;
        }
        return new ArrayList<>();
    }

    private List<GradRequirement> buildRequirementsMet(String programCode, List<TranscriptStudentCourse> tswStudentCourses, ConversionStudentSummaryDTO summary) {
        Set<GradRequirement> requirements = new HashSet<>();
        for (TranscriptStudentCourse tswCourse : tswStudentCourses) {
            if (StringUtils.isNotBlank(tswCourse.getFoundationReq())) {
                GradRequirement gradRequirement = populateRequirement(programCode, tswCourse.getFoundationReq(), summary);
                if (gradRequirement != null) {
                    requirements.add(gradRequirement);
                }
            }
        }
        return new ArrayList<>(requirements);
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

    private ConversionResultType processOptionalPrograms(GraduationStudentRecord student, String accessToken, ConversionStudentSummaryDTO summary) {
        if (StringUtils.isBlank(student.getProgram())) {
            return ConversionResultType.SUCCESS;
        }

        // Dual Dogwood for yyyy-PF
        if (student.getProgram().endsWith("-PF")) {
            return createStudentOptionalProgram("DD", student, false, accessToken, summary);
        }

        // French Immersion for 2018-EN, 2004-EN, 1996-EN, 1986-EN
        if (hasAnyFrenchImmersionCourse(student.getProgram(), student.getPen(), accessToken)) {
            return createStudentOptionalProgram("FI", student, false, accessToken, summary);
        }

        return ConversionResultType.SUCCESS;
    }

    private ConversionResultType processOptionalProgramsForGraduatedStudent(GraduationStudentRecord student, String accessToken, ConversionStudentSummaryDTO summary) {
        if (StringUtils.isBlank(student.getProgram())) {
            return ConversionResultType.SUCCESS;
        }

        // Dual Dogwood for yyyy-PF
        if (student.getProgram().endsWith("-PF") && StringUtils.equalsIgnoreCase(student.getEnglishCert(), "E")) {
            student.setDualDogwood(true);
            return createStudentOptionalProgram("DD", student, true, accessToken, summary);
        }

        // French Immersion for mincode[1:3] <> '093' and french_cert = 'F'
        if (student.getProgram().endsWith("-EN") && !student.getSchoolOfRecord().startsWith("093") && StringUtils.equalsIgnoreCase(student.getFrenchCert(), "F")) {
            return createStudentOptionalProgram("FI", student, true, accessToken, summary);
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

    private ConversionResultType processProgramCodes(GraduationStudentRecord student, List<String> programCodes, boolean isGraduated, String accessToken, ConversionStudentSummaryDTO summary) {
        ConversionResultType resultType = ConversionResultType.SUCCESS;
        Boolean isCareerProgramCreated = Boolean.FALSE;
        if (StringUtils.isNotBlank(student.getProgram()) && !programCodes.isEmpty()) {
            for (String programCode : programCodes) {
                Pair<ConversionResultType, Boolean> res = handleProgramCode(programCode, student, isGraduated, accessToken, summary);
                if (Boolean.TRUE.equals(res.getRight())) {
                    isCareerProgramCreated = Boolean.TRUE;
                }
                resultType = res.getLeft();
                if (ConversionResultType.FAILURE == resultType) {
                    break;
                }
            }
            if (Boolean.TRUE.equals(isCareerProgramCreated)) {
                resultType = createStudentOptionalProgram("CP", student, isGraduated, accessToken, summary);
            }
        }
        return resultType;
    }

    private Pair<ConversionResultType, Boolean> handleProgramCode(String programCode, GraduationStudentRecord student, boolean isGraduated, String accessToken, ConversionStudentSummaryDTO summary) {
        ConversionResultType resultType;
        boolean isCareerProgramCreated = false;
        if (isOptionalProgramCode(programCode)) {
            resultType = createStudentOptionalProgram(programCode, student, isGraduated, accessToken, summary);
        } else {
            resultType = createStudentCareerProgram(programCode, student, summary);
            if (ConversionResultType.SUCCESS == resultType) {
                isCareerProgramCreated = true;
            }
        }
        return Pair.of(resultType, isCareerProgramCreated);
    }

    private ConversionResultType processSccpFrenchCertificates(GraduationStudentRecord student, boolean isGraduated, String accessToken, ConversionStudentSummaryDTO summary) {
        if (StringUtils.equals(student.getProgram(), "SCCP")
            && ( StringUtils.isNotBlank(student.getSchoolOfRecord())
                 && student.getSchoolOfRecord().startsWith("093") )
        ) {
            return createStudentOptionalProgram("FR", student, isGraduated, accessToken, summary);
        }
        return ConversionResultType.SUCCESS;
    }

    private boolean isOptionalProgramCode(String code) {
       return OPTIONAL_PROGRAM_CODES.contains(code);
    }

    private ConversionResultType createStudentOptionalProgram(String optionalProgramCode, GraduationStudentRecord student, boolean isGraduated, String accessToken, ConversionStudentSummaryDTO summary) {
        StudentOptionalProgramRequestDTO object = new StudentOptionalProgramRequestDTO();
        object.setPen(student.getPen());
        object.setStudentID(student.getStudentID());
        object.setMainProgramCode(student.getProgram());
        object.setOptionalProgramCode(optionalProgramCode);
        object.setOptionalProgramCompletionDate(isGraduated? student.getProgramCompletionDate() : null);

        try {
            restUtils.saveStudentOptionalProgram(object, accessToken);
        } catch (Exception e) {
            handleException(null, summary, student.getPen(), ConversionResultType.FAILURE, GRAD_STUDENT_API_ERROR_MSG + "saving a StudentOptionalProgram : " + e.getLocalizedMessage());
            return ConversionResultType.FAILURE;
        }
        summary.incrementOptionalProgram(optionalProgramCode);
        return ConversionResultType.SUCCESS;
    }

    private ConversionResultType createStudentCareerProgram(String careerProgramCode, GraduationStudentRecord student, ConversionStudentSummaryDTO summary) {
        StudentCareerProgram object = new StudentCareerProgram();
        object.setStudentID(student.getStudentID());
        object.setCareerProgramCode(careerProgramCode);


        try {
            restUtils.saveStudentCareerProgram(object, summary.getAccessToken());
        } catch (Exception e) {
            handleException(null, summary, student.getPen(), ConversionResultType.FAILURE, GRAD_STUDENT_API_ERROR_MSG + "saving a StudentCareerProgram : " + e.getLocalizedMessage());
            return ConversionResultType.FAILURE;
        }
        summary.incrementCareerProgram(careerProgramCode);
        return ConversionResultType.SUCCESS;
    }

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

        GraduationStudentRecord gradStudent = null;
        try {
            gradStudent = restUtils.getStudentGradStatus(studentID.toString(), accessToken);
        } catch (Exception e) {
            log.error(GRAD_STUDENT_API_ERROR_MSG + "getting a GraduationStudentRecord : " + e.getLocalizedMessage());
            return null;
        }
        if (gradStudent != null) {
            studentData.setProgram(gradStudent.getProgram());
            studentData.setStudentGrade(gradStudent.getStudentGrade());
            studentData.setStudentStatus(gradStudent.getStudentStatus());
            studentData.setSchoolOfRecord(gradStudent.getSchoolOfRecord());
            studentData.setSchoolAtGrad(gradStudent.getSchoolAtGrad());
            studentData.setCitizenship(gradStudent.getStudentCitizenship());
            studentData.setAdultStartDate(gradStudent.getAdultStartDate());
        } else {
            log.error("GraduationStudentRecord is not found for pen# [{}], studentID [{}]", pen, studentID);
            return null;
        }

        // optional programs
        List<StudentOptionalProgram> optionalPrograms = new ArrayList<>();
        try {
            optionalPrograms = restUtils.getStudentOptionalPrograms(studentID.toString(), accessToken);
        } catch (Exception e) {
            log.error(GRAD_STUDENT_API_ERROR_MSG + "getting StudentOptionalPrograms : " + e.getLocalizedMessage());
        }
        studentData.getProgramCodes().addAll(getOptionalProgramCodes(optionalPrograms));

        // career programs
        List<StudentCareerProgram> careerPrograms = new ArrayList<>();
        try {
            careerPrograms = restUtils.getStudentCareerPrograms(studentID.toString(), accessToken);
        } catch (Exception e) {
            log.error(GRAD_STUDENT_API_ERROR_MSG + "getting StudentCareerPrograms : " + e.getLocalizedMessage());
        }
        studentData.getProgramCodes().addAll(getCareerProgramCodes(careerPrograms));

        // courses
        List<StudentCourse> courses = courseService.getStudentCourses(pen, accessToken);
        studentData.getCourses().addAll(courses);
        // assessments
        List<StudentAssessment> assessments = assessmentService.getStudentAssessments(pen, accessToken);
        studentData.getAssessments().addAll(assessments);

        return studentData;
    }

    private List<String> getOptionalProgramCodes(List<StudentOptionalProgram> studentOptionalPrograms) {
        List<String> codes = new ArrayList<>();
        if (studentOptionalPrograms != null && !studentOptionalPrograms.isEmpty()) {
            studentOptionalPrograms.forEach(e -> codes.add(e.getOptionalProgramCode()));
        }
        return codes;
    }

    private List<String> getCareerProgramCodes(List<StudentCareerProgram> studentCareerPrograms) {
        List<String> codes = new ArrayList<>();
        if (studentCareerPrograms != null && !studentCareerPrograms.isEmpty()) {
            studentCareerPrograms.forEach(e -> codes.add(e.getCareerProgramCode()));
        }
        return codes;
    }

    public void saveGraduationStudent(StudentGradDTO gradStudent, String accessToken) {
        GraduationStudentRecord object = restUtils.getStudentGradStatus(gradStudent.getStudentID().toString(), accessToken);
        if (object != null) {
            if (StringUtils.isNotBlank(gradStudent.getNewProgram())) {
                object.setProgram(gradStudent.getNewProgram());
            }
            if (StringUtils.isNotBlank(gradStudent.getNewStudentGrade())) {
                object.setStudentGrade(gradStudent.getNewStudentGrade());
            }
            if (StringUtils.isNotBlank(gradStudent.getNewStudentStatus())) {
                object.setStudentStatus(gradStudent.getNewStudentStatus());
            }
            if (StringUtils.isNotBlank(gradStudent.getNewSchoolOfRecord())) {
                object.setSchoolOfRecord(gradStudent.getNewSchoolOfRecord());
            }
            if (StringUtils.isNotBlank(gradStudent.getNewSchoolAtGrad())) {
                object.setSchoolAtGrad(gradStudent.getNewSchoolAtGrad());
            }
            if (StringUtils.isNotBlank(gradStudent.getNewRecalculateGradStatus())) {
                object.setRecalculateGradStatus(gradStudent.getNewRecalculateGradStatus());
            }
            if (StringUtils.isNotBlank(gradStudent.getNewRecalculateProjectedGrad())) {
                object.setRecalculateProjectedGrad(gradStudent.getNewRecalculateProjectedGrad());
            }

            restUtils.saveStudentGradStatus(gradStudent.getStudentID().toString(), object, true, accessToken);
        }

        if (gradStudent.isAddDualDogwood()) {
            log.info(" => [DD] optional program will be added if not exist.");
            addStudentOptionalProgram("DD", gradStudent, accessToken);
        } else if (gradStudent.isDeleteDualDogwood()) {
            log.info(" => [DD] optional program will be removed if exist.");
            removeStudentOptionalProgram("DD", gradStudent, accessToken);
        }
    }

    public void removeStudentOptionalProgram(String optionalProgramCode, StudentGradDTO gradStudent, String accessToken) {
        OptionalProgram optionalProgram = restUtils.getOptionalProgram(gradStudent.getProgram(), optionalProgramCode, accessToken);
        if (optionalProgram != null) {
            log.info(" => removed optional program code : {}", optionalProgramCode);
            removeStudentOptionalProgram(optionalProgram.getOptionalProgramID(), gradStudent, accessToken);
        }
    }

    public void removeStudentOptionalProgram(UUID optionalProgramID, StudentGradDTO gradStudent, String accessToken) {
        restUtils.removeStudentOptionalProgram(optionalProgramID, gradStudent.getStudentID(), accessToken);
    }

    public void addStudentOptionalProgram(String optionalProgramCode, StudentGradDTO gradStudent, String accessToken) {
        StudentOptionalProgramRequestDTO object = new StudentOptionalProgramRequestDTO();
        object.setStudentID(gradStudent.getStudentID());
        object.setMainProgramCode(gradStudent.getProgram());
        object.setOptionalProgramCode(optionalProgramCode);
        restUtils.saveStudentOptionalProgram(object, accessToken);
    }

    public void addStudentCareerProgram(String careerProgramCode, UUID studentID, String accessToken) {
        StudentCareerProgram object = new StudentCareerProgram();
        object.setStudentID(studentID);
        object.setCareerProgramCode(careerProgramCode);
        restUtils.saveStudentCareerProgram(object, accessToken);
    }

    public void removeStudentCareerProgram(String careerProgramCode, StudentGradDTO gradStudent, String accessToken) {
        restUtils.removeStudentCareerProgram(careerProgramCode, gradStudent.getStudentID(), accessToken);
    }

    public boolean existsCareerProgram(UUID studentID, String accessToken) {
        List<StudentCareerProgram> list = restUtils.getStudentCareerPrograms(studentID.toString(), accessToken);
        return list != null && !list.isEmpty();
    }

    public void triggerGraduationBatchRun(UUID studentID, String recalculateGradStatus, String recalculateProjectedGrad, String accessToken) {
        GraduationStudentRecord object = restUtils.getStudentGradStatus(studentID.toString(), accessToken);
        if (object != null) {
            if (StringUtils.equals(object.getStudentStatus(), STUDENT_STATUS_MERGED)) {
                object.setRecalculateGradStatus(null);
                object.setRecalculateProjectedGrad(null);
            } else {
                object.setRecalculateGradStatus(recalculateGradStatus == null? "Y" : recalculateGradStatus);
                object.setRecalculateProjectedGrad(recalculateProjectedGrad == null? "Y" : recalculateProjectedGrad);
            }
            restUtils.saveStudentGradStatus(studentID.toString(), object, false, accessToken);
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

    private boolean isAdultOrSccp(String graduationProgram, String grade) {
        return "SCCP".equalsIgnoreCase(graduationProgram) || ("1950".equalsIgnoreCase(graduationProgram) && "AD".equalsIgnoreCase(grade));
    }

    private void handleAdult19Rule(ConvGradStudent student, Student penStudent, GraduationStudentRecord gradStudent) {
        if ("1950".equalsIgnoreCase(gradStudent.getProgram()) && "AD".equalsIgnoreCase(gradStudent.getStudentGrade())) {
            Date dob = EducGradDataConversionApiUtils.parseDate(penStudent.getDob());
            Date adultStartDate = DateUtils.addYears(dob, student.isAdult19Rule()? 19 : 18);
            gradStudent.setAdultStartDate(EducGradDataConversionApiUtils.formatDate(adultStartDate)); // yyyy-MM-dd
        }
    }

    private boolean validateAndSetSlpDate(ConvGradStudent student, GraduationStudentRecord gradStudent, ConversionStudentSummaryDTO summary) {
        if (StringUtils.isNotBlank(student.getSlpDate()) && StringUtils.length(student.getSlpDate().trim()) != 8) {
            handleException(student, summary, student.getPen(), ConversionResultType.FAILURE, "Bad data : slp_date format is not yyyyMMdd");
            return false;
        } else {
            Date pcd = EducGradDataConversionApiUtils.parseDate(student.getSlpDate(), EducGradDataConversionApiConstants.TRAX_SLP_DATE_FORMAT);
            gradStudent.setProgramCompletionDate(EducGradDataConversionApiUtils.formatDate(pcd));
            return true;
        }
    }
}
