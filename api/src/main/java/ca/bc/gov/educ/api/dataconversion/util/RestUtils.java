package ca.bc.gov.educ.api.dataconversion.util;

import ca.bc.gov.educ.api.dataconversion.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.UUID;

@Component
public class RestUtils {

    private final EducGradDataConversionApiConstants constants;

    private final WebClient webClient;

    @Autowired
    public RestUtils(final EducGradDataConversionApiConstants constants, final WebClient webClient) {
        this.constants = constants;
        this.webClient = webClient;
    }

    public ResponseObj getTokenResponseObject() {
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

    public List<Student> getStudentsByPen(String pen, String accessToken) {
        final ParameterizedTypeReference<List<Student>> responseType = new ParameterizedTypeReference<>() {
        };
        return this.webClient.get()
                .uri(String.format(constants.getPenStudentApiByPenUrl(), pen))
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve().bodyToMono(responseType).block();
    }

    public OptionalProgram getOptionalProgram(String programCode, String specialProgramCode, String accessToken) {
        return this.webClient.get()
                .uri(constants.getGradOptionalProgramUrl(), uri -> uri.path("/{programCode}/{specialProgramCode}").build(programCode, specialProgramCode))
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve().bodyToMono(OptionalProgram.class).block();
    }

    public OptionalProgram getOptionalProgramByID(UUID optionalProgramID, String accessToken) {
        return this.webClient.get()
                .uri(constants.getGradOptionalProgramByIDUrl(), uri -> uri.path("/{optionalProgramID}").build(optionalProgramID))
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve().bodyToMono(OptionalProgram.class).block();
    }


    public CareerProgram getCareerProgram(String careerProgramCode, String accessToken) {
        return this.webClient.get()
                .uri(constants.getGradCareerProgramUrl(), uri -> uri.path("/{careerProgramCode}").build(careerProgramCode))
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve().bodyToMono(CareerProgram.class).block();
    }

    public Student addNewPen(Student student, String accessToken) {
        return webClient.post()
                .uri(constants.getAddNewPenFromGradStudentApiUrl())
                .headers(h -> h.setBearerAuth(accessToken))
                .body(BodyInserters.fromValue(student))
                .retrieve().bodyToMono(Student.class).block();
    }

    public AssessmentRequirement addAssessmentRequirement(AssessmentRequirement assessmentRequirement, String accessToken) {
        return webClient.post()
                .uri(constants.getAddAssessmentRequirementApiUrl())
                .headers(h -> h.setBearerAuth(accessToken))
                .body(BodyInserters.fromValue(assessmentRequirement))
                .retrieve().bodyToMono(AssessmentRequirement.class).block();
    }

    public List<StudentAssessment> getStudentAssessmentsByPen(String pen, String accessToken) {
        final ParameterizedTypeReference<List<StudentAssessment>> responseType = new ParameterizedTypeReference<>() {
        };
        return this.webClient.get()
                .uri(String.format(constants.getStudentAssessmentsByPenApiUrl(), pen))
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve().bodyToMono(responseType).block();
    }

}
