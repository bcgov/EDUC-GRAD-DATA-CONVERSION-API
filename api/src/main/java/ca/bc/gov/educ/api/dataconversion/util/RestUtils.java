package ca.bc.gov.educ.api.dataconversion.util;

import ca.bc.gov.educ.api.dataconversion.model.*;
import ca.bc.gov.educ.api.dataconversion.model.StudentAssessment;
import ca.bc.gov.educ.api.dataconversion.model.StudentCareerProgram;
import ca.bc.gov.educ.api.dataconversion.model.StudentCourse;
import ca.bc.gov.educ.api.dataconversion.model.tsw.*;
import ca.bc.gov.educ.api.dataconversion.model.tsw.report.ReportRequest;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class RestUtils {

    private final EducGradDataConversionApiConstants constants;

    private ResponseObjCache responseObjCache;

    private final WebClient webClient;

    @Autowired
    public RestUtils(final EducGradDataConversionApiConstants constants, final WebClient webClient, ResponseObjCache objCache) {
        this.constants = constants;
        this.webClient = webClient;
        this.responseObjCache = objCache;
    }

    public ResponseObj getTokenResponseObject() {
        if(responseObjCache.isExpired()){
            responseObjCache.setResponseObj(getTokenResponseObj());
        }
        return responseObjCache.getResponseObj();
    }

    @Retry(name = "rt-getToken", fallbackMethod = "rtGetTokenFallback")
    private ResponseObj getTokenResponseObj() {
        log.info("Fetching the access token from KeyCloak API");
        HttpHeaders httpHeadersKC = EducGradDataConversionApiUtils.getHeaders(
                constants.getUserName(), constants.getPassword());
        MultiValueMap<String, String> map= new LinkedMultiValueMap<>();
        map.add("grant_type", "client_credentials");
        return this.webClient.post().uri(constants.getTokenUrl())
                .headers(h -> h.addAll(httpHeadersKC))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(map))
                .retrieve()
                .bodyToMono(ResponseObj.class).block();
    }

    public ResponseObj rtGetTokenFallBack(HttpServerErrorException exception){
        log.error("{} NOT REACHABLE after many attempts: {}", constants.getTokenUrl(), exception);
        return null;
    }

    public List<Student> getStudentsByPen(String pen, String accessToken) {
        final ParameterizedTypeReference<List<Student>> responseType = new ParameterizedTypeReference<>() {
        };
        return this.webClient.get()
                .uri(String.format(constants.getPenStudentApiByPenUrl(), pen))
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradDataConversionApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
                })
                .retrieve().bodyToMono(responseType).block();
    }

    public OptionalProgram getOptionalProgram(String programCode, String specialProgramCode, String accessToken) {
        return this.webClient.get()
                .uri(constants.getGradOptionalProgramUrl(), uri -> uri.path("/{programCode}/{specialProgramCode}").build(programCode, specialProgramCode))
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradDataConversionApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
                })
                .retrieve().bodyToMono(OptionalProgram.class).block();
    }

    public OptionalProgram getOptionalProgramByID(UUID optionalProgramID, String accessToken) {
        return this.webClient.get()
                .uri(constants.getGradOptionalProgramByIDUrl(), uri -> uri.path("/{optionalProgramID}").build(optionalProgramID))
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradDataConversionApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
                })
                .retrieve().bodyToMono(OptionalProgram.class).block();
    }


    public CareerProgram getCareerProgram(String careerProgramCode, String accessToken) {
        return this.webClient.get()
                .uri(constants.getGradCareerProgramUrl(), uri -> uri.path("/{careerProgramCode}").build(careerProgramCode))
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradDataConversionApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
                })
                .retrieve().bodyToMono(CareerProgram.class).block();
    }

    public Student addNewPen(Student student, String accessToken) {
        return webClient.post()
                .uri(constants.getAddNewPenFromGradStudentApiUrl())
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradDataConversionApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
                })
                .body(BodyInserters.fromValue(student))
                .retrieve().bodyToMono(Student.class).block();
    }

    public AssessmentRequirement addAssessmentRequirement(AssessmentRequirement assessmentRequirement, String accessToken) {
        return webClient.post()
                .uri(constants.getAddAssessmentRequirementApiUrl())
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradDataConversionApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
                })
                .body(BodyInserters.fromValue(assessmentRequirement))
                .retrieve().bodyToMono(AssessmentRequirement.class).block();
    }

    public List<StudentAssessment> getStudentAssessmentsByPenAndAssessmentCode(String pen, String assessmentCode, String accessToken) {
        final ParameterizedTypeReference<List<StudentAssessment>> responseType = new ParameterizedTypeReference<>() {
        };
        return this.webClient.get()
                .uri(String.format(constants.getStudentAssessmentsByPenAndAssessmentCodeApiUrl(), assessmentCode, pen))
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradDataConversionApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
                })
                .retrieve().bodyToMono(responseType).block();
    }

    public List<StudentAssessment> getStudentAssessmentsByPen(String pen, String accessToken) {
        final ParameterizedTypeReference<List<StudentAssessment>> responseType = new ParameterizedTypeReference<>() {
        };
        return this.webClient.get()
                .uri(String.format(constants.getStudentAssessmentsByPenApiUrl(), pen))
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradDataConversionApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
                })
                .retrieve().bodyToMono(responseType).block();
    }

    public List<StudentCourse> getStudentCoursesByPen(String pen, String accessToken) {
        final ParameterizedTypeReference<List<StudentCourse>> responseType = new ParameterizedTypeReference<>() {
        };
        return this.webClient.get()
                .uri(constants.getStudentCoursesByPenApiUrl(), uri -> uri.path("/{pen}").build(pen))
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradDataConversionApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
                })
                .retrieve().bodyToMono(responseType).block();
    }

    public CourseRestriction getCourseRestriction(
            String courseCode, String courseLevel,
            String restrictedCourseCode, String restrictedCourseLevel,
            String accessToken) {
        log.debug("get request to retrieve Course Restriction: {} / {}, {} / {}", courseCode, courseLevel, restrictedCourseCode, restrictedCourseLevel);
        return this.webClient.get()
                .uri(constants.getGradCourseRestrictionApiUrl(),
                    uri -> uri.queryParam("courseCode", courseCode)
                            .queryParam("courseLevel", courseLevel)
                            .queryParam("restrictedCourseCode", restrictedCourseCode)
                            .queryParam("restrictedCourseLevel", restrictedCourseLevel)
                            .build())
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradDataConversionApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
                })
                .retrieve().bodyToMono(CourseRestriction.class).block();
    }

    public CourseRestriction saveCourseRestriction(CourseRestriction courseRestriction, String accessToken) {
        return webClient.post()
                .uri(constants.getSaveCourseRestrictionApiUrl())
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradDataConversionApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
                })
                .body(BodyInserters.fromValue(courseRestriction))
                .retrieve().bodyToMono(CourseRestriction.class).block();
    }

    public CourseRequirement saveCourseRequirement(CourseRequirement courseRequirement, String accessToken) {
        return webClient.post()
                .uri(constants.getSaveCourseRequirementApiUrl())
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradDataConversionApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
                })
                .body(BodyInserters.fromValue(courseRequirement))
                .retrieve().bodyToMono(CourseRequirement.class).block();
    }

    public Boolean checkCourseRequirementExists (
            String courseCode, String courseLevel, String ruleCode,
            String accessToken) {
        log.debug("get request to check Course Requirement exists: {} / {} [{}]", courseCode, courseLevel, ruleCode);
        return this.webClient.get()
                .uri(constants.getCheckCourseRequirementApiUrl(),
                        uri -> uri.queryParam("courseCode", courseCode)
                                .queryParam("courseLevel", courseLevel)
                                .queryParam("ruleCode", ruleCode)
                                .build())
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradDataConversionApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
                })
                .retrieve().bodyToMono(Boolean.class).block();
    }

    public Boolean checkFrenchImmersionCourse(
            String pen, String courseLevel,
            String accessToken) {
        return this.webClient.get()
                .uri(constants.getCheckFrenchImmersionCourse(),
                        uri -> uri.queryParam("pen", pen)
                                .queryParam("courseLevel", courseLevel)
                                .build())
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradDataConversionApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
                })
                .retrieve().bodyToMono(Boolean.class).block();
    }

    public Boolean checkFrenchImmersionCourseForEN(
            String pen, String courseLevel,
            String accessToken) {
        return this.webClient.get()
                .uri(constants.getCheckFrenchImmersionCourseForEN(),
                        uri -> uri.queryParam("pen", pen)
                                .queryParam("courseLevel", courseLevel)
                                .build())
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradDataConversionApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
                })
                .retrieve().bodyToMono(Boolean.class).block();
    }

    public Boolean checkBlankLanguageCourse(
            String courseCode, String courseLevel,
            String accessToken) {
        return this.webClient.get()
                .uri(constants.getCheckBlankLanguageCourse(),
                        uri -> uri.queryParam("courseCode", courseCode)
                                .queryParam("courseLevel", courseLevel)
                                .build())
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradDataConversionApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
                })
                .retrieve().bodyToMono(Boolean.class).block();
    }

    public Boolean checkFrenchLanguageCourse(
            String courseCode, String courseLevel,
            String accessToken) {
        return this.webClient.get()
                .uri(constants.getCheckFrenchLanguageCourse(),
                        uri -> uri.queryParam("courseCode", courseCode)
                                .queryParam("courseLevel", courseLevel)
                                .build())
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradDataConversionApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
                })
                .retrieve().bodyToMono(Boolean.class).block();
    }

    @Retry(name = "rt-getTraxStudent")
    public List<ConvGradStudent> getTraxStudentMasterDataByPen(String pen, String accessToken) {
        final ParameterizedTypeReference<List<ConvGradStudent>> responseType = new ParameterizedTypeReference<>() {
        };
        return this.webClient.get()
                .uri(constants.getTraxStudentMasterDataByPenUrl(), uri -> uri.path("/{pen}").build(pen))
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradDataConversionApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
                })
                .retrieve().bodyToMono(responseType).block();
    }

    public List<Student> getTraxStudentDemographicsDataByPen(String pen, String accessToken) {
        final ParameterizedTypeReference<List<Student>> responseType = new ParameterizedTypeReference<>() {
        };
        return this.webClient.get()
                .uri(constants.getTraxStudentDemogDataByPenUrl(), uri -> uri.path("/{pen}").build(pen))
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradDataConversionApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
                })
                .retrieve().bodyToMono(responseType).block();
    }

    public List<TraxStudentNo> getTraxStudentNoListByPage(int pageNumber, int pageSize, String accessToken) {
        final ParameterizedTypeReference<List<TraxStudentNo>> responseType = new ParameterizedTypeReference<>() {
        };
        return this.webClient.get()
                .uri(constants.getTraxStudentNoListByPageUrl(),
                        uri -> uri.queryParam("pageNumber", pageNumber)
                                .queryParam("pageSize", pageSize)
                                .build())
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradDataConversionApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
                })
                .retrieve().bodyToMono(responseType).block();
    }

    public Integer getTotalNumberOfTraxStudentNoList(String accessToken) {
        return webClient.get()
                .uri(constants.getTotalNumberOfTraxStudentNoListUrl())
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradDataConversionApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
                })
                .retrieve().bodyToMono(Integer.class).block();
    }

    public List<CourseRestriction> getTraxCourseRestrictions(String accessToken) {
        final ParameterizedTypeReference<List<CourseRestriction>> responseType = new ParameterizedTypeReference<>() {
        };
        return this.webClient.get()
                .uri(constants.getTraxCourseRestrictionsUrl())
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradDataConversionApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
                })
                .retrieve().bodyToMono(responseType).block();
    }

    public List<GradCourse> getTraxCourseRequirements(String accessToken) {
        final ParameterizedTypeReference<List<GradCourse>> responseType = new ParameterizedTypeReference<>() {
        };
        return this.webClient.get()
                .uri(constants.getTraxCourseRequirementsUrl())
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradDataConversionApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
                })
                .retrieve().bodyToMono(responseType).block();
    }

    @Retry(name = "rt-conversionStatus")
    public TraxStudentNo saveTraxStudentNo(TraxStudentNo traxStudentNo, String accessToken) {
        return webClient.post()
                .uri(constants.getSaveTraxStudentNoUrl())
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradDataConversionApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
                })
                .body(BodyInserters.fromValue(traxStudentNo))
                .retrieve().bodyToMono(TraxStudentNo.class).block();
    }

    public GraduationProgramCode getGradProgramCode(String programCode, String accessToken) {
        return this.webClient.get()
                .uri(constants.getGradProgramUrl(), uri -> uri.path("/{programCode}").build(programCode))
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradDataConversionApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
                })
                .retrieve().bodyToMono(GraduationProgramCode.class).block();
    }

    public List<GradRuleDetails> getGradProgramRulesByTraxReqNumber(String traxReqNumber, String accessToken) {
        final ParameterizedTypeReference<List<GradRuleDetails>> responseType = new ParameterizedTypeReference<>() {
        };
        return this.webClient.get()
                .uri(constants.getGradProgramRulesByTraxReqNumberUrl(), uri -> uri.path("/{traxReqNumber}").build(traxReqNumber))
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradDataConversionApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
                })
                .retrieve().bodyToMono(responseType).block();
    }

    public List<ProgramRequirement> getGradProgramRules(String gradProgramCode, String accessToken) {
        final ParameterizedTypeReference<List<ProgramRequirement>> responseType = new ParameterizedTypeReference<>() {
        };
        return this.webClient.get()
                .uri(constants.getGradProgramRulesUrl(),
                        uri -> uri.queryParam("programCode", gradProgramCode)
                                .build())
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradDataConversionApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
                })
                .retrieve().bodyToMono(responseType).block();
    }

    public List<SpecialCase> getAllSpecialCases(String accessToken) {
        final ParameterizedTypeReference<List<SpecialCase>> responseType = new ParameterizedTypeReference<>() {
        };
        return this.webClient.get()
                .uri(constants.getSpecialCase())
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradDataConversionApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
                })
                .retrieve().bodyToMono(responseType).block();
    }

    public SpecialCase getSpecialCase(String specialCode, String accessToken) {
        return this.webClient.get()
                .uri(constants.getSpecialCase(), uri -> uri.path("/{specialCode}").build(specialCode))
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradDataConversionApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
                }).retrieve().bodyToMono(SpecialCase.class).block();
    }

    public ProgramCertificateTranscript getTranscript(String gradProgram, String schoolCategoryCode, String accessToken) {
        ProgramCertificateReq req = new ProgramCertificateReq();
        req.setProgramCode(gradProgram);
        req.setSchoolCategoryCode(schoolCategoryCode);
        return webClient.post().uri(constants.getTranscript())
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradDataConversionApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
                }).body(BodyInserters.fromValue(req)).retrieve().bodyToMono(ProgramCertificateTranscript.class).block();
    }

    public String getSchoolCategoryCode(String mincode, String accessToken) {
        CommonSchool commonSchoolObj = webClient.get()
                .uri(constants.getSchoolCategoryCode(), uri -> uri.path("/{mincode}").build(mincode))
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradDataConversionApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
                }).retrieve().bodyToMono(CommonSchool.class).block();
        if (commonSchoolObj != null) {
            return commonSchoolObj.getSchoolCategoryCode();
        }
        return null;
    }

    public GradProgram getGradProgram(String gradProgram, String accessToken) {
        return webClient.get().uri(String.format(constants.getProgramNameEndpoint(), gradProgram))
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradDataConversionApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
                }).retrieve().bodyToMono(GradProgram.class).block();
    }

    @Retry(name = "rt-transcript")
    public void saveGradStudentTranscript(GradStudentTranscripts requestObj, boolean isGraduated, String accessToken) {
        webClient.post().uri(String.format(constants.getUpdateGradStudentTranscript(), isGraduated))
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradDataConversionApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
                }).body(BodyInserters.fromValue(requestObj)).retrieve().bodyToMono(GradStudentTranscripts.class).block();
    }

    public byte[] getTranscriptReport(ReportRequest reportParams, String accessToken) {
        return webClient.post().uri(constants.getTranscriptReport())
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradDataConversionApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
                }).body(BodyInserters.fromValue(reportParams)).retrieve().bodyToMono(byte[].class).block();
    }

    public List<ProgramCertificateTranscript> getProgramCertificateTranscriptList(ProgramCertificateReq req, String accessToken) {
        return webClient.post().uri(constants.getCertList())
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradDataConversionApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
                }).body(BodyInserters.fromValue(req)).retrieve().bodyToMono(new ParameterizedTypeReference<List<ProgramCertificateTranscript>>() {
                }).block();
    }

    @Retry(name = "rt-certificate")
    public void saveGradStudentCertificate(GradStudentCertificates requestObj, String accessToken) {
        webClient.post().uri(constants.getUpdateGradStudentCertificate())
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradDataConversionApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
                }).body(BodyInserters.fromValue(requestObj)).retrieve().bodyToMono(GradStudentCertificates.class).block();
    }

    public byte[] getCertificateReport(ReportRequest reportParams, String accessToken) {
        return webClient.post().uri(constants.getCertificateReport())
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradDataConversionApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
                }).body(BodyInserters.fromValue(reportParams)).retrieve().bodyToMono(byte[].class).block();
    }

    // Read GraduationStudentRecord  - GET /student/studentid/{id}/algorithm
    @Retry(name = "rt-getStudentGradStatus", fallbackMethod = "rtGetStudentGradStatusFallback")
    public GraduationStudentRecord getStudentGradStatus(String studentID, String accessToken) {
        return webClient.get().uri(String.format(constants.getReadGraduationStudentRecord(),studentID))
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradDataConversionApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
                }).retrieve().bodyToMono(GraduationStudentRecord.class).block();
    }

    // Save GraduationStudentRecord  - POST /student/conv/studentid/{id}
    @Retry(name = "rt-saveStudentGradStatus", fallbackMethod = "rtSaveStudentGradStatusFallback")
    public GraduationStudentRecord saveStudentGradStatus(String studentID, GraduationStudentRecord toBeSaved, boolean ongoingUpdate, String accessToken) {
        return webClient.post()
                .uri(String.format(constants.getSaveGraduationStudentRecord(),studentID),
                        uri -> uri.queryParam("ongoingUpdate", ongoingUpdate)
                                .build())
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradDataConversionApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
                }).body(BodyInserters.fromValue(toBeSaved)).retrieve().bodyToMono(GraduationStudentRecord.class).block();
    }

    // Remove All Student Related Data - DELETE /student/conv/studentid/{id}
    public void removeAllStudentRelatedData(UUID studentID, String accessToken) {
        webClient.delete().uri(String.format(constants.getSaveGraduationStudentRecord(),studentID))
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradDataConversionApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
                }).retrieve().onStatus(p -> p.value() == 404, error -> Mono.error(new Exception("Student Data Not Found"))).bodyToMono(Void.class).block();
    }

    // READ StudentOptionalProgram - GET /student/optionalprogram/studentid/{id}
    public List<StudentOptionalProgram> getStudentOptionalPrograms(String studentID, String accessToken) {
        final ParameterizedTypeReference<List<StudentOptionalProgram>> responseType = new ParameterizedTypeReference<>() {
        };
        return this.webClient.get().uri(String.format(constants.getReadStudentOptionalPrograms(), studentID))
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradDataConversionApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
                }).retrieve().bodyToMono(responseType).block();
    }

    // Save StudentOptionalProgram - POST /student/conv/studentoptionalprogram
    public StudentOptionalProgram saveStudentOptionalProgram(StudentOptionalProgramRequestDTO toBeSaved, String accessToken) {
        return webClient.post().uri(constants.getSaveStudentOptionalProgram())
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradDataConversionApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
                }).body(BodyInserters.fromValue(toBeSaved)).retrieve().bodyToMono(StudentOptionalProgram.class).block();
    }

    // Remove StudentOptionalProgram - DELETE /student/conv/studentoptionalprogram/{optionalProramID}/{studentID}
    public void removeStudentOptionalProgram(UUID optionalProgramID, UUID studentID, String accessToken) {
        webClient.delete().uri(String.format(constants.getRemoveStudentOptionalProgram(), optionalProgramID, studentID))
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradDataConversionApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
                }).retrieve().onStatus(p -> p.value() == 404, error -> Mono.error(new Exception("Student Optional Program Not Found"))).bodyToMono(Void.class).block();
    }

    // READ StudentCareerProgram - GET /student/studentcareerprogram/studentid/{id}
    public List<StudentCareerProgram> getStudentCareerPrograms(String studentID, String accessToken) {
        final ParameterizedTypeReference<List<StudentCareerProgram>> responseType = new ParameterizedTypeReference<>() {
        };
        return this.webClient.get().uri(String.format(constants.getReadStudentCareerPrograms(), studentID))
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradDataConversionApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
                }).retrieve().bodyToMono(responseType).block();
    }

    // Save StudentCareerProgram - NEW - POST /student/conv/studentcareerprogram
    public StudentCareerProgram saveStudentCareerProgram(StudentCareerProgram toBeSaved, String accessToken) {
        return webClient.post().uri(constants.getSaveStudentCareerProgram())
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradDataConversionApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
                }).body(BodyInserters.fromValue(toBeSaved)).retrieve().bodyToMono(StudentCareerProgram.class).block();
    }

    // Remove StudentCareerProgram - DELETE /student/conv/studentcareerprogram/{careerProgramCode}/{studentID}
    public void removeStudentCareerProgram(String careerProgramCode, UUID studentID, String accessToken) {
        webClient.delete().uri(String.format(constants.getRemoveStudentCareerProgram(), careerProgramCode, studentID))
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradDataConversionApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
                }).retrieve().onStatus(p -> p.value() == 404, error -> Mono.error(new Exception("Student Career Program Not Found"))).bodyToMono(Void.class).block();
    }

    public ConvGradStudent rtGetStudentGradStatusFallback(HttpServerErrorException exception){
        log.error("STUDENT GRAD STATUS NOT Retrievable after many attempts: {}", exception);
        return null;
    }

    public ConvGradStudent rtSaveStudentGradStatusFallback(HttpServerErrorException exception){
        log.error("STUDENT GRAD STATUS NOT Saved after many attempts: {}", exception);
        return null;
    }
}
