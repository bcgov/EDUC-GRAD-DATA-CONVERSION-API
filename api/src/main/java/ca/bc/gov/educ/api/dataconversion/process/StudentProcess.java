package ca.bc.gov.educ.api.dataconversion.process;

import ca.bc.gov.educ.api.dataconversion.constant.ConversionResultType;
import ca.bc.gov.educ.api.dataconversion.constant.StudentLoadType;
import ca.bc.gov.educ.api.dataconversion.model.*;
import ca.bc.gov.educ.api.dataconversion.model.StudentAssessment;
import ca.bc.gov.educ.api.dataconversion.model.StudentCourse;
import ca.bc.gov.educ.api.dataconversion.model.tsw.*;
import ca.bc.gov.educ.api.dataconversion.model.tsw.report.ReportData;

import ca.bc.gov.educ.api.dataconversion.service.student.StudentBaseService;
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
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiConstants.DEFAULT_CREATED_BY;
import static ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiConstants.DEFAULT_UPDATED_BY;

@Component
@Slf4j
public class StudentProcess extends StudentBaseService {

    private static final String GRAD_STUDENT_API_ERROR_MSG = "Grad Student API is failed for ";
    private static final String EXCEPTION_MSG = "Exception occurred: ";

    private final RestUtils restUtils;
    private final AssessmentProcess assessmentProcess;
    private final CourseProcess courseProcess;
    private final ReportProcess reportProcess;

    /**
     * The Program Rules map.
     */
    private final Map<String, List<ProgramRequirement>> programRuleMap = new ConcurrentHashMap<>();

    /**
     * The Special Case map.
     */
    private final Map<String, SpecialCase> specialCaseMap = new ConcurrentHashMap<>();

    @Autowired
    public StudentProcess(RestUtils restUtils,
                          AssessmentProcess assessmentProcess,
                          CourseProcess courseProcess,
                          ReportProcess reportProcess) {
        this.restUtils = restUtils;
        this.assessmentProcess = assessmentProcess;
        this.courseProcess = courseProcess;
        this.reportProcess = reportProcess;
    }

    public void clearMaps() {
        programRuleMap.clear();
        specialCaseMap.clear();
    }

    public ConvGradStudent convertStudent(ConvGradStudent convGradStudent, ConversionStudentSummaryDTO summary, boolean reload) throws Exception {
        long startTime = System.currentTimeMillis();
        summary.setProcessedCount(summary.getProcessedCount() + 1L);

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

        // Program Completion for graduated student
        if (!validateProgramCompletionDate(convGradStudent, summary)) {
            return convGradStudent;
        }

        // Student conversion process
        process(convGradStudent, students, summary, reload);

        long diff = (System.currentTimeMillis() - startTime) / 1000L;
        log.info("** PEN: {} - {} secs", convGradStudent.getPen(), diff);
        return convGradStudent;
    }

    private void validateSchool(ConvGradStudent convGradStudent, ConversionStudentSummaryDTO summary) {
        // School Category Code form School API
        if (convGradStudent.getStudentLoadType() != StudentLoadType.UNGRAD && StringUtils.isBlank(convGradStudent.getTranscriptSchoolCategoryCode())) {
            handleException(convGradStudent, summary, convGradStudent.getPen(), ConversionResultType.FAILURE, "School does not exist in SPM School data : mincode [" + convGradStudent.getSchoolOfRecord() + "]");
            return;
        }
        // TRAX School validation
        if (convGradStudent.getTranscriptSchool() == null) {
            handleException(convGradStudent, summary, convGradStudent.getPen(), ConversionResultType.FAILURE, "Invalid school of record " + convGradStudent.getSchoolOfRecord());
        }
    }

    private List<Student> getStudentsFromPEN(ConvGradStudent convGradStudent, ConversionStudentSummaryDTO summary) {
        List<Student> students = null;
        try {
            // Call PEN Student API
            students = restUtils.getStudentsByPen(convGradStudent.getPen(), summary.getAccessToken());
        } catch (Exception e) {
            log.error(EXCEPTION_MSG, e);
            handleException(convGradStudent, summary, convGradStudent.getPen(), ConversionResultType.FAILURE, "PEN Student API is failed: " + e.getLocalizedMessage());
        }
        return students;
    }

    /**
     *
     * @return true             Valid
     *         false            Bad data (programCompletionDate is null)
     */
    private boolean validateProgramCompletionDate(ConvGradStudent convGradStudent, ConversionStudentSummaryDTO summary) {
        switch(convGradStudent.getStudentLoadType()) {
            case GRAD_ONE -> {
                if (!"SCCP".equalsIgnoreCase(convGradStudent.getGraduationRequirementYear()) &&
                        convGradStudent.getProgramCompletionDate() == null) {
                    handleException(convGradStudent, summary, convGradStudent.getPen(), ConversionResultType.FAILURE, "Bad data: grad_date is null for " + convGradStudent.getGraduationRequirementYear());
                    return false;
                }
                if ("SCCP".equalsIgnoreCase(convGradStudent.getGraduationRequirementYear()) &&
                        StringUtils.isBlank(convGradStudent.getSlpDate())) {
                    handleException(convGradStudent, summary, convGradStudent.getPen(), ConversionResultType.FAILURE, "Bad data: slp_date is null for SCCP");
                    return false;
                }
            }
            case GRAD_TWO -> {
                if (!"SCCP".equalsIgnoreCase(convGradStudent.getGraduationRequirementYear()) &&
                        StringUtils.isBlank(convGradStudent.getSccDate())) {
                    handleException(convGradStudent, summary, convGradStudent.getPen(), ConversionResultType.FAILURE, "Bad data for graduated - 2 programs: scc_date is null for " + convGradStudent.getGraduationRequirementYear());
                    return false;
                }
            }
            default -> {
                return true;
            }
        }
        return true;
    }

    private void process(ConvGradStudent convGradStudent, List<Student> students, ConversionStudentSummaryDTO summary, boolean reload) {
        convGradStudent.setOriginalStudentLoadType(convGradStudent.getStudentLoadType());
        switch (convGradStudent.getStudentLoadType()) {
            case UNGRAD -> {
                log.debug("Process Non-Graduated Student for pen# : " + convGradStudent.getPen());
                processConversion(convGradStudent, students, summary, reload);
            }
            case GRAD_ONE -> {
                log.debug("Process Graduated Student - 1 Program for pen# : " + convGradStudent.getPen());
                processConversion(convGradStudent, students, summary, reload);
            }
            case GRAD_TWO -> {
                log.debug("Process Graduated Student - 2 Programs for pen# : " + convGradStudent.getPen());
                // phase 1
                String graduationRequirementYear = convGradStudent.getGraduationRequirementYear();
                convGradStudent.setGraduationRequirementYear("SCCP");
                convGradStudent.setStudentLoadType(StudentLoadType.GRAD_ONE);
                processConversion(convGradStudent, students, summary, reload);
                // phase 2
                convGradStudent.setGraduationRequirementYear(graduationRequirementYear);
                convGradStudent.setSccDate(null);
                convGradStudent.setSlpDate(null);
                if (convGradStudent.getProgramCompletionDate() != null) {
                    convGradStudent.setStudentLoadType(StudentLoadType.GRAD_ONE);
                } else {
                    convGradStudent.setStudentLoadType(StudentLoadType.UNGRAD);
                }
                processConversion(convGradStudent, students, summary, false);
            }
            default -> log.debug("skip process");
        }
    }

    private void processConversion(ConvGradStudent convGradStudent, List<Student> students, ConversionStudentSummaryDTO summary, boolean reload) {
        students.forEach(st -> {
            if (reload) {
                restUtils.removeAllStudentRelatedData(UUID.fromString(st.getStudentID()), summary.getAccessToken());
            }
            GraduationStudentRecord gradStudent = processStudent(convGradStudent, st, summary);
            if (gradStudent != null) {
                processDependencies(convGradStudent, gradStudent, st, summary, reload);
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
            log.error(EXCEPTION_MSG, e);
            handleException(convGradStudent, summary, convGradStudent.getPen(), ConversionResultType.FAILURE, GRAD_STUDENT_API_ERROR_MSG + "getting a GraduationStudentRecord : " + e.getLocalizedMessage());
            return null;
        }
        if (gradStudent != null) { // update
            gradStudent.setPen(penStudent.getPen());
            if (convGradStudent.getStudentLoadType() == StudentLoadType.UNGRAD) {
                convertStudentData(convGradStudent, penStudent, gradStudent, summary);
            } else {
                convertGraduatedStudentData(convGradStudent, penStudent, gradStudent, summary);
            }
            if (ConversionResultType.FAILURE != convGradStudent.getResult()) {
                gradStudent.setUpdateDate(null);
                gradStudent.setUpdateUser(null);
                try {
                    gradStudent = restUtils.saveStudentGradStatus(penStudent.getStudentID(), gradStudent, false, summary.getAccessToken());
                } catch (Exception e) {
                    log.error(EXCEPTION_MSG, e);
                    handleException(convGradStudent, summary, convGradStudent.getPen(), ConversionResultType.FAILURE, GRAD_STUDENT_API_ERROR_MSG + "saving a GraduationStudentRecord : " + e.getLocalizedMessage());
                    return null;
                }
                summary.setUpdatedCount(summary.getUpdatedCount() + 1L);
            }
        } else { // create
            gradStudent = new GraduationStudentRecord();
            gradStudent.setPen(penStudent.getPen());
            gradStudent.setStudentID(studentID);
            if (convGradStudent.getStudentLoadType() == StudentLoadType.UNGRAD) {
                convertStudentData(convGradStudent, penStudent, gradStudent, summary);
            } else {
                convertGraduatedStudentData(convGradStudent, penStudent, gradStudent, summary);
            }
            if (ConversionResultType.FAILURE != convGradStudent.getResult()) {
                try {
                    gradStudent = restUtils.saveStudentGradStatus(penStudent.getStudentID(), gradStudent, false, summary.getAccessToken());
                } catch (Exception e) {
                    log.error(EXCEPTION_MSG, e);
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
                                     ConversionStudentSummaryDTO summary,
                                     boolean reload) {
        ConversionResultType result;

        // process dependencies
        gradStudent.setPen(convGradStudent.getPen());
        if (convGradStudent.getStudentLoadType() == StudentLoadType.UNGRAD) {
            result = processOptionalPrograms(gradStudent, summary);
        } else {
            result = processOptionalProgramsForGraduatedStudent(convGradStudent, gradStudent, summary);
        }

        if (ConversionResultType.FAILURE != result) {
            result = processProgramCodes(gradStudent, convGradStudent.getProgramCodes(), convGradStudent.getStudentLoadType(), convGradStudent.getOriginalStudentLoadType(), summary);
        }
        if (ConversionResultType.FAILURE != result && convGradStudent.getStudentLoadType() == StudentLoadType.UNGRAD) {
            result = processSccpFrenchCertificates(gradStudent, summary);
        }

        if (convGradStudent.getStudentLoadType() != StudentLoadType.UNGRAD && !StringUtils.equalsIgnoreCase(gradStudent.getStudentStatus(), STUDENT_STATUS_MERGED)) {
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
            processReports(graduationData, convGradStudent, summary, reload);
        }
        convGradStudent.setResult(result);
    }

    private void processReports(GraduationData graduationData, ConvGradStudent convGradStudent, ConversionStudentSummaryDTO summary, boolean reload) {
        if (convGradStudent.getTranscriptSchool() != null && "Y".equalsIgnoreCase(convGradStudent.getTranscriptSchool().getTranscriptEligibility())) {
            fetchAccessToken(summary);
            createAndStoreStudentTranscript(graduationData, convGradStudent, summary.getAccessToken(), reload);
        }
        if (convGradStudent.getCertificateSchool() != null && "Y".equalsIgnoreCase(convGradStudent.getCertificateSchool().getCertificateEligibility())) {
            fetchAccessToken(summary);
            createAndStoreStudentCertificates(graduationData, convGradStudent, summary.getAccessToken(), reload);
        }
    }

    private void createAndStoreStudentTranscript(GraduationData graduationData, ConvGradStudent convStudent, String accessToken, boolean reload) {
        ReportData data = reportProcess.prepareTranscriptData(graduationData, graduationData.getGradStatus(), convStudent, accessToken);
        reportProcess.saveStudentTranscriptReportJasper(data, convStudent.getDistributionDate(), accessToken, graduationData.getGradStatus().getStudentID(), graduationData.isGraduated(), reload);
    }

    private void createAndStoreStudentCertificates(GraduationData graduationData, ConvGradStudent convStudent, String accessToken, boolean reload) {
        List<ProgramCertificateTranscript> certificateList = reportProcess.getCertificateList(graduationData,
            convStudent.getCertificateSchoolCategoryCode() != null? convStudent.getCertificateSchoolCategoryCode() : convStudent.getTranscriptSchoolCategoryCode(), accessToken);
        int i = 0;
        for (ProgramCertificateTranscript certType : certificateList) {
            reportProcess.saveStudentCertificateReportJasper(graduationData, convStudent, accessToken, certType, i == 0 && reload);
            i++;
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

        handleAdultStartRule(student, penStudent, gradStudent);

        // flags
        if (StringUtils.equalsIgnoreCase(gradStudent.getStudentStatus(), STUDENT_STATUS_MERGED)) {
            gradStudent.setRecalculateGradStatus(null);
            gradStudent.setRecalculateProjectedGrad(null);
        } else if (StringUtils.equalsIgnoreCase(gradStudent.getStudentStatus(), STUDENT_STATUS_ARCHIVED)) {
            gradStudent.setRecalculateGradStatus("Y");
            gradStudent.setRecalculateProjectedGrad(null);
        } else {
            gradStudent.setRecalculateGradStatus("Y");
            gradStudent.setRecalculateProjectedGrad("Y");
        }

        // Mappings with Student_Master
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

        handleAdultStartRule(student, penStudent, gradStudent);

        // flags
        gradStudent.setRecalculateGradStatus(null);
        gradStudent.setRecalculateProjectedGrad(null);

        // Mappings with Student_Master
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
        gradStatus.setPen(student.getPen());
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
        List<GradAlgorithmOptionalStudentProgram> optionalGradStatus = buildOptionalGradStatus(student, gradStudent, studentCourseList, studentAssessmentList, summary);
        graduationData.setOptionalGradStatus(optionalGradStatus);

        // requirements met
        List<GradRequirement> requirementsMet = buildRequirementsMet(gradSearchStudent.getProgram(), transcriptStudentCourses, summary);
        graduationData.setRequirementsMet(requirementsMet);

        // gradMessage
        String gradMessage = transcriptStudentDemog.getGradMessage();
        if ("1950".equalsIgnoreCase(gradSearchStudent.getProgram())
            && isProgramFrancophone(gradMessage)) {
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
        Double proficiencyScore;
        if ("LTE10".equalsIgnoreCase(tswCourse.getCourseCode()) || "LTP10".equalsIgnoreCase(tswCourse.getCourseCode())) {
            proficiencyScore = getProficiencyScoreFromTrax(tswCourse.getStudNo(), tswCourse.getCourseCode(), tswCourse.getCourseSession(), accessToken);
        } else {
            proficiencyScore = EducGradDataConversionApiUtils.getPercentage(tswCourse.getFinalPercentage());
        }
        ca.bc.gov.educ.api.dataconversion.model.tsw.StudentAssessment result = ca.bc.gov.educ.api.dataconversion.model.tsw.StudentAssessment.builder()
                .pen(tswCourse.getStudNo())
                .assessmentCode(tswCourse.getCourseCode())
                .assessmentName(tswCourse.getCourseName())
                .sessionDate(tswCourse.getCourseSession())
                .proficiencyScore(proficiencyScore)
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

    private Double getProficiencyScoreFromTrax(String pen, String assessmentCode, String sessionDate, String accessToken) {
        Double proficiencyScore = null;
        List<StudentAssessment> studentAssessments = restUtils.getStudentAssessmentsByPenAndAssessmentCode(pen, assessmentCode, accessToken);
        for (StudentAssessment sA : studentAssessments) {
            if (StringUtils.equalsIgnoreCase(sA.getSessionDate(), sessionDate)) {
                proficiencyScore = sA.getProficiencyScore();
                break;
            }
        }
        return proficiencyScore;
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
                                                                              List<ca.bc.gov.educ.api.dataconversion.model.tsw.StudentCourse> studentCourseList,
                                                                              List<ca.bc.gov.educ.api.dataconversion.model.tsw.StudentAssessment> studentAssessmentList,
                                                                              ConversionStudentSummaryDTO summary) {
        List<GradAlgorithmOptionalStudentProgram> results = new ArrayList<>();
        List<StudentOptionalProgram> list = null;
        try {
            list = restUtils.getStudentOptionalPrograms(gradStudent.getStudentID().toString(), summary.getAccessToken());
        } catch (Exception e) {
            handleException(student, summary, student.getPen(), ConversionResultType.FAILURE, GRAD_STUDENT_API_ERROR_MSG + "getting StudentOptionalPrograms : " + e.getLocalizedMessage());
        }
        if (list != null && !list.isEmpty()) {
            List<StudentCareerProgram> careerProgramList = buildStudentCareerProgramList(student, gradStudent.getStudentID(), summary);
            for (StudentOptionalProgram obj : list) {
                GradAlgorithmOptionalStudentProgram result = populateOptionStudentProgramStatus(obj, studentCourseList, studentAssessmentList, careerProgramList, summary);
                results.add(result);
            }
        }
        return results;
    }

    private GradAlgorithmOptionalStudentProgram populateOptionStudentProgramStatus(StudentOptionalProgram object,
                                                                                   List<ca.bc.gov.educ.api.dataconversion.model.tsw.StudentCourse> studentCourseList,
                                                                                   List<ca.bc.gov.educ.api.dataconversion.model.tsw.StudentAssessment> studentAssessmentList,
                                                                                   List<StudentCareerProgram> careerProgramList, ConversionStudentSummaryDTO summary) {
        GradAlgorithmOptionalStudentProgram result = new GradAlgorithmOptionalStudentProgram();
        result.setOptionalProgramID(object.getOptionalProgramID());
        result.setOptionalProgramCode(object.getOptionalProgramCode());
        result.setOptionalProgramName(object.getOptionalProgramName());
        result.setOptionalProgramCompletionDate(object.getOptionalProgramCompletionDate());
        result.setStudentID(object.getStudentID());
        result.setOptionalGraduated(true);

        // Optional Program Requirements Met
        GradRequirement gradRequirement = createOptionalProgramRequirementMet(object.getOptionalProgramCode());
        result.setOptionalRequirementsMet(gradRequirement != null? Arrays.asList(gradRequirement) : new ArrayList<>());
        result.setOptionalNonGradReasons(new ArrayList<>());

        // Career Programs
        result.setCpList(careerProgramList);

        // Student Courses
        StudentCourses studentCourses = new StudentCourses();
        studentCourses.setStudentCourseList(studentCourseList);
        result.setOptionalStudentCourses(studentCourses);

        // Student Assessments
        StudentAssessments studentAssessments = new StudentAssessments();
        studentAssessments.setStudentAssessmentList(studentAssessmentList);
        result.setOptionalStudentAssessments(studentAssessments);

        // Clob
        try {
            object.setStudentOptionalProgramData(new ObjectMapper().writeValueAsString(result));
        } catch (JsonProcessingException e) {
            log.error("Json Parsing Error for StudentOptionalProgramData: " + e.getLocalizedMessage());
        }

        updateStudentOptionalProgram(object, summary.getAccessToken());
        return result;
    }

    private GradRequirement createOptionalProgramRequirementMet(String optionalProgramCode) {
        if (StringUtils.isBlank(optionalProgramCode)) {
            return null;
        }
        GradRequirement gradRequirement = new GradRequirement();
        gradRequirement.setProjected(false);
        switch (optionalProgramCode) {
            case "AD" -> {  // 951
                gradRequirement.setRule("951");
                gradRequirement.setDescription("A school must report the student as participating in the Advanced Placement program");
            }
            case "BC" -> { //
                gradRequirement.setRule("952");
                gradRequirement.setDescription("The school must report the student as participating in the International Baccalaureate program as a Certificate candidate");
            }
            case "BD" -> {
                gradRequirement.setRule("953");
                gradRequirement.setDescription("The school must report the student as participating in the International Baccalaureate program as a Diploma candidate");
            }
            case "CP" -> {
                gradRequirement.setRule("954");
                gradRequirement.setDescription("The school must report the student as participating in a Career Program");
            }
            default -> gradRequirement = null;
        }
        return gradRequirement;
    }

    private void updateStudentOptionalProgram(StudentOptionalProgram object, String accessToken) {
        StudentOptionalProgramRequestDTO requestDTO = new StudentOptionalProgramRequestDTO();
        BeanUtils.copyProperties(object, requestDTO);
        restUtils.saveStudentOptionalProgram(requestDTO, accessToken);
    }

    private List<StudentCareerProgram> buildStudentCareerProgramList(ConvGradStudent student, UUID studentID, ConversionStudentSummaryDTO summary) {
        List<StudentCareerProgram> results = null;
        try {
            results = restUtils.getStudentCareerPrograms(studentID.toString(), summary.getAccessToken());
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

    private ConversionResultType processOptionalPrograms(GraduationStudentRecord student, ConversionStudentSummaryDTO summary) {
        if (StringUtils.isBlank(student.getProgram())) {
            return ConversionResultType.SUCCESS;
        }

        // Dual Dogwood for yyyy-PF
        if (student.getProgram().endsWith("-PF")) {
            return createStudentOptionalProgram("DD", student, StudentLoadType.UNGRAD, summary);
        }

        // French Immersion for 2018-EN, 2004-EN, 1996-EN, 1986-EN
        if (hasAnyFrenchImmersionCourse(student.getProgram(), student.getPen(), summary.getAccessToken())) {
            return createStudentOptionalProgram("FI", student, StudentLoadType.UNGRAD, summary);
        }

        return ConversionResultType.SUCCESS;
    }

    private ConversionResultType processOptionalProgramsForGraduatedStudent(ConvGradStudent convGradStudent, GraduationStudentRecord student, ConversionStudentSummaryDTO summary) {
        if (StringUtils.isBlank(student.getProgram())) {
            return ConversionResultType.SUCCESS;
        }

        // Dual Dogwood for yyyy-PF
        if (student.getProgram().endsWith("-PF") && StringUtils.equalsIgnoreCase(convGradStudent.getEnglishCert(), "E")) {
            student.setDualDogwood(true);
            return createStudentOptionalProgram("DD", student, StudentLoadType.GRAD_ONE, summary);
        }

        // French Immersion for yyyy-EN to check if their Graduation Message contains "Student has successfully completed the French Immersion program"
        if (student.getProgram().endsWith("-EN") && isFrenchImmersion(convGradStudent.getTranscriptStudentDemog().getGradMessage())) {
            return createStudentOptionalProgram("FI", student, StudentLoadType.GRAD_ONE, summary);
        }

        return ConversionResultType.SUCCESS;
    }

    public boolean hasAnyFrenchImmersionCourse(String program, String pen, String accessToken) {
        boolean frenchImmersion = false;
        switch (program) {
            case "2018-EN", "2004-EN" -> {
                if (courseProcess.isFrenchImmersionCourse(pen, "10", accessToken)) { // FRAL 10 or FRALP 10
                    frenchImmersion = true;
                }
            }
            case "1996-EN" -> {
                if (courseProcess.isFrenchImmersionCourse(pen, "11", accessToken)) { // FRAL 11 or FRALP 11
                    frenchImmersion = true;
                }
            }
            case "1986-EN" -> {
                if (courseProcess.isFrenchImmersionCourseForEN(pen, "11", accessToken)) { // FRAL 11
                    frenchImmersion = true;
                }
            }
        }
        return frenchImmersion;
    }

    private ConversionResultType processProgramCodes(GraduationStudentRecord student, List<String> programCodes, StudentLoadType studentLoadType, StudentLoadType originalStudentLoadType, ConversionStudentSummaryDTO summary) {
        ConversionResultType resultType = ConversionResultType.SUCCESS;
        Boolean isCareerProgramCreated = Boolean.FALSE;
        if (StringUtils.isNotBlank(student.getProgram()) && !programCodes.isEmpty()) {
            for (String programCode : programCodes) {
                Pair<ConversionResultType, Boolean> res = handleProgramCode(programCode, student, studentLoadType, originalStudentLoadType, summary);
                if (Boolean.TRUE.equals(res.getRight())) {
                    isCareerProgramCreated = Boolean.TRUE;
                }
                resultType = res.getLeft();
                if (ConversionResultType.FAILURE == resultType) {
                    break;
                }
            }
            if (Boolean.TRUE.equals(isCareerProgramCreated)) {
                resultType = createStudentOptionalProgram("CP", student, studentLoadType, summary);
            }
        }
        return resultType;
    }

    private Pair<ConversionResultType, Boolean> handleProgramCode(String programCode, GraduationStudentRecord student, StudentLoadType studentLoadType, StudentLoadType originalStudentLoadType, ConversionStudentSummaryDTO summary) {
        ConversionResultType resultType;
        boolean isCareerProgramCreated = false;
        if (isOptionalProgramCode(programCode)) {
            resultType = isStudentSCCPForTwoPrograms(student.getProgram(), originalStudentLoadType)? ConversionResultType.SUCCESS :
                createStudentOptionalProgram(programCode, student, studentLoadType, summary);
        } else {
            resultType = createStudentCareerProgram(programCode, student, summary);
            if (ConversionResultType.SUCCESS == resultType) {
                isCareerProgramCreated = true;
            }
        }
        return Pair.of(resultType, isCareerProgramCreated);
    }

    // GRAD2-2013: skip optional programs for SCCP during 1st pass
    private boolean isStudentSCCPForTwoPrograms(String graduationProgramCode, StudentLoadType originalStudentLoadType) {
        return "SCCP".equalsIgnoreCase(graduationProgramCode) && originalStudentLoadType == StudentLoadType.GRAD_TWO;
    }

    private ConversionResultType processSccpFrenchCertificates(GraduationStudentRecord student, ConversionStudentSummaryDTO summary) {
        if (StringUtils.equals(student.getProgram(), "SCCP")
            && ( StringUtils.isNotBlank(student.getSchoolOfRecord())
                 && student.getSchoolOfRecord().startsWith("093") )
        ) {
            return createStudentOptionalProgram("FR", student, StudentLoadType.UNGRAD, summary);
        }
        return ConversionResultType.SUCCESS;
    }

    private ConversionResultType createStudentOptionalProgram(String optionalProgramCode, GraduationStudentRecord student, StudentLoadType studentLoadType, ConversionStudentSummaryDTO summary) {
        StudentOptionalProgramRequestDTO object = new StudentOptionalProgramRequestDTO();
        object.setPen(student.getPen());
        object.setStudentID(student.getStudentID());
        object.setMainProgramCode(student.getProgram());
        object.setOptionalProgramCode(optionalProgramCode);
        object.setOptionalProgramCompletionDate(studentLoadType == StudentLoadType.UNGRAD? null : student.getProgramCompletionDate());

        try {
            restUtils.saveStudentOptionalProgram(object, summary.getAccessToken());
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
        List<StudentCourse> courses = courseProcess.getStudentCourses(pen, accessToken);
        if (courses != null && !courses.isEmpty()) {
            studentData.getCourses().addAll(courses);
        }
        // assessments
        List<StudentAssessment> assessments = assessmentProcess.getStudentAssessments(pen, accessToken);
        if (assessments != null && !assessments.isEmpty()) {
            studentData.getAssessments().addAll(assessments);
        }

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
            if (StringUtils.isNotBlank(gradStudent.getNewAdultStartDate())) {
                object.setAdultStartDate(gradStudent.getNewAdultStartDate());
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
        } else if (gradStudent.isDeleteFrenchImmersion()) {
            log.info(" => [FI] optional program will be removed if exist.");
            removeStudentOptionalProgram("FI", gradStudent, accessToken);
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

    private void handleAdultStartRule(ConvGradStudent student, Student penStudent, GraduationStudentRecord gradStudent) {
        if ("1950".equalsIgnoreCase(gradStudent.getProgram())) {
            Date dob = EducGradDataConversionApiUtils.parseDate(penStudent.getDob());
            Date adultStartDate;
            if ("AD".equalsIgnoreCase(gradStudent.getStudentGrade())) {
                adultStartDate = DateUtils.addYears(dob, student.isAdult19Rule() ? 19 : 18);
            } else {
                adultStartDate = DateUtils.addYears(dob, 18);
            }
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

    private void fetchAccessToken(ConversionStudentSummaryDTO summaryDTO) {
        ResponseObj res = restUtils.getTokenResponseObject();
        if (res != null) {
            summaryDTO.setAccessToken(res.getAccess_token());
            log.debug("Setting the new access token in summaryDTO.");
        }
    }
}
