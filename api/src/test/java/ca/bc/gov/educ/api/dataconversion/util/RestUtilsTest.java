package ca.bc.gov.educ.api.dataconversion.util;


import ca.bc.gov.educ.api.dataconversion.model.GradSpecialProgram;
import ca.bc.gov.educ.api.dataconversion.model.ResponseObj;
import ca.bc.gov.educ.api.dataconversion.model.Student;
import ca.bc.gov.educ.api.dataconversion.repository.conv.EventRepository;
import lombok.val;
import org.codehaus.jackson.JsonProcessingException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class RestUtilsTest {
    @Autowired
    RestUtils restUtils;

    @MockBean
    WebClient webClient;

    @MockBean
    EventRepository eventRepository;

    @Autowired
    private EducGradDataConversionApiConstants constants;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersMock;
    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriMock;
    @Mock
    private WebClient.RequestBodySpec requestBodyMock;
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriMock;
    @Mock
    private WebClient.ResponseSpec responseMock;

    @Before
    public void setUp() {
        openMocks(this);
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testGetTokenResponseObject_returnsToken_with_APICallSuccess() {
        final ResponseObj tokenObject = new ResponseObj();
        tokenObject.setAccess_token("123");
        tokenObject.setRefresh_token("456");

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(constants.getTokenUrl())).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(ResponseObj.class)).thenReturn(Mono.just(tokenObject));

        val result = this.restUtils.getTokenResponseObject();
        assertThat(result).isNotNull();
        assertThat(result.getAccess_token()).isEqualTo("123");
        assertThat(result.getRefresh_token()).isEqualTo("456");
    }

    @Test
    public void testGetStudentByPen_givenValues_returnsStudent_with_APICallSuccess() {
        final String studentID = UUID.randomUUID().toString();
        final Student student = new Student();
        final String pen = "123456789";
        student.setStudentID(studentID);
        student.setPen(pen);

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(constants.getPenStudentApiByPenUrl(), pen))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);

        final ParameterizedTypeReference<List<Student>> responseType = new ParameterizedTypeReference<>() {
        };
        when(this.responseMock.bodyToMono(responseType)).thenReturn(Mono.just(Arrays.asList(student)));

        val result = this.restUtils.getStudentsByPen(pen, "abc");
        assertThat(result).isNotNull();
        assertThat(result.size() > 0).isTrue();
        assertThat(result.get(0).getPen()).isEqualTo(pen);
    }

    @Test
    public void testGetSpecialProgram_givenValues_returnsGradSpecialProgram_with_APICallSuccess() throws JsonProcessingException {
        final UUID specialProgramID = UUID.randomUUID();
        final GradSpecialProgram specialProgram = new GradSpecialProgram();
        specialProgram.setOptionalProgramID(specialProgramID);
        specialProgram.setGraduationProgramCode("abc");
        specialProgram.setOptProgramCode("def");

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(eq(this.constants.getGradProgramManagementUrl()), any(Function.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);

        when(this.responseMock.bodyToMono(GradSpecialProgram.class)).thenReturn(Mono.just(specialProgram));
        val result = this.restUtils.getGradSpecialProgram("abc", "def", "123");
        assertThat(result).isNotNull();
        assertThat(result.getGraduationProgramCode()).isEqualTo("abc");
        assertThat(result.getOptProgramCode()).isEqualTo("def");
    }
}
