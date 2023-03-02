package ca.bc.gov.educ.api.dataconversion.service;

import ca.bc.gov.educ.api.dataconversion.messaging.NatsConnection;
import ca.bc.gov.educ.api.dataconversion.messaging.jetstream.Subscriber;
import ca.bc.gov.educ.api.dataconversion.model.ConvGradStudent;
import ca.bc.gov.educ.api.dataconversion.model.tsw.*;
import ca.bc.gov.educ.api.dataconversion.model.tsw.report.Code;
import ca.bc.gov.educ.api.dataconversion.model.tsw.report.ReportData;
import ca.bc.gov.educ.api.dataconversion.model.tsw.report.Transcript;
import ca.bc.gov.educ.api.dataconversion.service.student.ReportService;
import ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiConstants;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.RandomUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
@SuppressWarnings({"unchecked","rawtypes"})
public class ReportServiceTest {

    @Autowired
    private ReportService reportService;

    @Autowired
    private ExceptionMessage exception;

    @MockBean
    RestUtils restUtils;

    @MockBean
    WebClient webClient;

    // NATS
    @MockBean
    private NatsConnection natsConnection;
    @MockBean
    private Subscriber subscriber;

    @Autowired
    private EducGradDataConversionApiConstants constants;

    @Before
    public void setUp() {
        openMocks(this);
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testSaveStudentTranscriptReport() {
        String studentID = new UUID(1, 1).toString();
        String accessToken = "accessToken";
        String pen="212321123";
        boolean isGraduated= false;
        ReportData data = new ReportData();
        data.setGradMessage("ABC");
        Transcript transcript = new Transcript();
        Code code= new Code();
        code.setCode("BC1996-PUB");
        transcript.setTranscriptTypeCode(code);
        data.setTranscript(transcript);
        ExceptionMessage exception = new ExceptionMessage();
        Date distributionDate = new Date(System.currentTimeMillis());

        byte[] bytesSAR = RandomUtils.nextBytes(20);

        CommonSchool commSch = new CommonSchool();
        commSch.setSchlNo("1231123");
        commSch.setSchoolCategoryCode("02");

        when(this.restUtils.getSchoolCategoryCode("06011033", accessToken)).thenReturn(commSch.getSchoolCategoryCode());
        when(this.restUtils.getTranscriptReport(any(), eq(accessToken))).thenReturn(bytesSAR);

        reportService.saveStudentTranscriptReportJasper(data, distributionDate, accessToken, UUID.fromString(studentID),isGraduated);
        assertThat(exception.getExceptionName()).isNull();
    }

    @Test
    public void testGetCertificateList() {
        String studentID = new UUID(1, 1).toString();
        String accessToken = "accessToken";
        GradAlgorithmGraduationStudentRecord gradResponse = new GradAlgorithmGraduationStudentRecord();
        gradResponse.setPen("123090109");
        gradResponse.setProgram("2018-EN");
        gradResponse.setProgramCompletionDate(null);
        gradResponse.setSchoolOfRecord("06011033");
        gradResponse.setStudentGrade("11");
        gradResponse.setStudentStatus("D");

        GradAlgorithmGraduationStudentRecord gradAlgorithmGraduationStatus = new GradAlgorithmGraduationStudentRecord();
        gradAlgorithmGraduationStatus.setPen("123090109");
        gradAlgorithmGraduationStatus.setProgram("2018-EN");
        gradAlgorithmGraduationStatus.setProgramCompletionDate(null);
        gradAlgorithmGraduationStatus.setSchoolOfRecord("06011033");
        gradAlgorithmGraduationStatus.setStudentGrade("11");
        gradAlgorithmGraduationStatus.setStudentStatus("A");

        School schoolObj = new School();
        schoolObj.setMinCode("1231123");
        schoolObj.setIndependentDesignation("3");

        GraduationData graduationDataStatus = new GraduationData();
        graduationDataStatus.setDualDogwood(false);
        graduationDataStatus.setGradMessage("DD Graduated");
        graduationDataStatus.setGradStatus(gradAlgorithmGraduationStatus);
        graduationDataStatus.setGraduated(true);
        graduationDataStatus.setSchool(schoolObj);
        graduationDataStatus.setStudentCourses(null);

        GradAlgorithmOptionalStudentProgram spgm = new GradAlgorithmOptionalStudentProgram();
        spgm.setPen("123090109");
        spgm.setOptionalProgramCode("BD");
        spgm.setOptionalProgramName("International Bacculaurette Diploma");
        spgm.setOptionalGraduated(true);
        spgm.setStudentID(UUID.fromString(studentID));
        List<GradAlgorithmOptionalStudentProgram> list = new ArrayList<>();
        list.add(spgm);

        graduationDataStatus.setOptionalGradStatus(list);

        List<ProgramCertificateTranscript> clist= new ArrayList<ProgramCertificateTranscript>();
        ProgramCertificateTranscript pc = new ProgramCertificateTranscript();
        pc.setCertificateTypeCode("E");
        pc.setSchoolCategoryCode(" ");
        clist.add(pc);

        CommonSchool comSchObj = new CommonSchool();
        comSchObj.setDistNo("123");
        comSchObj.setSchlNo("1123");
        comSchObj.setSchoolCategoryCode("02");

        when(this.restUtils.getSchoolCategoryCode("06011033", accessToken)).thenReturn(comSchObj.getSchoolCategoryCode());
        when(this.restUtils.getProgramCertificateTranscriptList(any(), eq(accessToken))).thenReturn(clist);

        List<ProgramCertificateTranscript> listCC =reportService.getCertificateList(graduationDataStatus, comSchObj.getSchoolCategoryCode(), accessToken);
        assertThat(listCC).hasSize(1);
    }

    @Test
    public void testGetCertificateList_PFProgram() {
        String studentID = new UUID(1, 1).toString();
        String accessToken = "accessToken";
        GradAlgorithmGraduationStudentRecord gradResponse = new GradAlgorithmGraduationStudentRecord();
        gradResponse.setPen("123090109");
        gradResponse.setProgram("2018-PF");
        gradResponse.setProgramCompletionDate(null);
        gradResponse.setSchoolOfRecord("06011033");
        gradResponse.setStudentGrade("11");
        gradResponse.setStudentStatus("D");

        GradAlgorithmGraduationStudentRecord gradAlgorithmGraduationStatus = new GradAlgorithmGraduationStudentRecord();
        gradAlgorithmGraduationStatus.setPen("123090109");
        gradAlgorithmGraduationStatus.setProgram("2018-EN");
        gradAlgorithmGraduationStatus.setProgramCompletionDate(null);
        gradAlgorithmGraduationStatus.setSchoolOfRecord("06011033");
        gradAlgorithmGraduationStatus.setStudentGrade("11");
        gradAlgorithmGraduationStatus.setStudentStatus("A");

        School schoolObj = new School();
        schoolObj.setMinCode("1231123");
        schoolObj.setIndependentDesignation("3");

        GraduationData graduationDataStatus = new GraduationData();
        graduationDataStatus.setGradMessage("DD Graduated");
        graduationDataStatus.setGradStatus(gradAlgorithmGraduationStatus);
        graduationDataStatus.setGraduated(true);
        graduationDataStatus.setSchool(schoolObj);
        graduationDataStatus.setStudentCourses(null);
        graduationDataStatus.setDualDogwood(true);

        List<ProgramCertificateTranscript> clist= new ArrayList<ProgramCertificateTranscript>();
        ProgramCertificateTranscript pc = new ProgramCertificateTranscript();
        pc.setCertificateTypeCode("E");
        pc.setSchoolCategoryCode(" ");
        clist.add(pc);

        when(this.restUtils.getProgramCertificateTranscriptList(any(), eq(accessToken))).thenReturn(clist);

        GradAlgorithmOptionalStudentProgram spgm = new GradAlgorithmOptionalStudentProgram();
        spgm.setPen("123090109");
        spgm.setOptionalProgramCode("BD");
        spgm.setOptionalProgramName("International Bacculaurette Diploma");
        spgm.setStudentID(UUID.fromString(studentID));
        List<GradAlgorithmOptionalStudentProgram> list = new ArrayList<>();
        list.add(spgm);

        graduationDataStatus.setOptionalGradStatus(list);

        CommonSchool comSchObj = new CommonSchool();
        comSchObj.setDistNo("123");
        comSchObj.setSchlNo("1123");
        comSchObj.setSchoolCategoryCode("02");

        when(this.restUtils.getSchoolCategoryCode("06011033", accessToken)).thenReturn(comSchObj.getSchoolCategoryCode());

        List<ProgramCertificateTranscript> listCC = reportService.getCertificateList(graduationDataStatus, comSchObj.getSchoolCategoryCode(), accessToken);
        assertThat(listCC).hasSize(1);
    }

    @Test
    public void testGetCertificateList_PFProgram_nodogwood() {
        String studentID = new UUID(1, 1).toString();
        String accessToken = "accessToken";
        GradAlgorithmGraduationStudentRecord gradResponse = new GradAlgorithmGraduationStudentRecord();
        gradResponse.setPen("123090109");
        gradResponse.setProgram("2018-PF");
        gradResponse.setProgramCompletionDate(null);
        gradResponse.setSchoolOfRecord("06011033");
        gradResponse.setStudentGrade("11");
        gradResponse.setStudentStatus("D");

        GradAlgorithmGraduationStudentRecord gradAlgorithmGraduationStatus = new GradAlgorithmGraduationStudentRecord();
        gradAlgorithmGraduationStatus.setPen("123090109");
        gradAlgorithmGraduationStatus.setProgram("2018-EN");
        gradAlgorithmGraduationStatus.setProgramCompletionDate(null);
        gradAlgorithmGraduationStatus.setSchoolOfRecord("06011033");
        gradAlgorithmGraduationStatus.setStudentGrade("11");
        gradAlgorithmGraduationStatus.setStudentStatus("A");

        School schoolObj = new School();
        schoolObj.setMinCode("1231123");
        schoolObj.setIndependentDesignation("3");

        GraduationData graduationDataStatus = new GraduationData();
        graduationDataStatus.setDualDogwood(false);
        graduationDataStatus.setGradMessage("EN Graduated");
        graduationDataStatus.setGradStatus(gradAlgorithmGraduationStatus);
        graduationDataStatus.setGraduated(true);
        graduationDataStatus.setSchool(schoolObj);
        graduationDataStatus.setStudentCourses(null);

        GradAlgorithmOptionalStudentProgram spgm = new GradAlgorithmOptionalStudentProgram();
        spgm.setPen("123090109");
        spgm.setOptionalProgramCode("BD");
        spgm.setOptionalProgramName("International Bacculaurette Diploma");
        spgm.setStudentID(UUID.fromString(studentID));
        List<GradAlgorithmOptionalStudentProgram> list = new ArrayList<>();
        list.add(spgm);

        graduationDataStatus.setOptionalGradStatus(list);

        CommonSchool comSchObj = new CommonSchool();
        comSchObj.setDistNo("123");
        comSchObj.setSchlNo("1123");
        comSchObj.setSchoolCategoryCode("02");

        when(this.restUtils.getSchoolCategoryCode("06011033", accessToken)).thenReturn(comSchObj.getSchoolCategoryCode());

        List<ProgramCertificateTranscript> clist= new ArrayList<ProgramCertificateTranscript>();
        ProgramCertificateTranscript pc = new ProgramCertificateTranscript();
        pc.setCertificateTypeCode("E");
        pc.setSchoolCategoryCode(" ");
        clist.add(pc);

        when(this.restUtils.getProgramCertificateTranscriptList(any(), eq(accessToken))).thenReturn(clist);

        List<ProgramCertificateTranscript> listCC= reportService.getCertificateList(graduationDataStatus, comSchObj.getSchoolCategoryCode(), accessToken);
        assertThat(listCC).hasSize(1);
    }

    @Test
    public void testGetCertificateList_emptyOptionalProgram() {
        UUID studentID = new UUID(1, 1);
        String accessToken = "accessToken";
        GradAlgorithmGraduationStudentRecord gradResponse = new GradAlgorithmGraduationStudentRecord();
        gradResponse.setStudentID(studentID);
        gradResponse.setPen("123090109");
        gradResponse.setProgram("2018-EN");
        gradResponse.setProgramCompletionDate(null);
        gradResponse.setSchoolOfRecord("06011033");
        gradResponse.setStudentGrade("11");
        gradResponse.setStudentStatus("D");

        CommonSchool comSchObj = new CommonSchool();
        comSchObj.setDistNo("123");
        comSchObj.setSchlNo("1123");
        comSchObj.setSchoolCategoryCode("02");

        when(this.restUtils.getSchoolCategoryCode("06011033", accessToken)).thenReturn(comSchObj.getSchoolCategoryCode());

        GradAlgorithmGraduationStudentRecord gradAlgorithmGraduationStatus = new GradAlgorithmGraduationStudentRecord();
        gradAlgorithmGraduationStatus.setPen("123090109");
        gradAlgorithmGraduationStatus.setProgram("2018-EN");
        gradAlgorithmGraduationStatus.setProgramCompletionDate(null);
        gradAlgorithmGraduationStatus.setSchoolOfRecord("06011033");
        gradAlgorithmGraduationStatus.setStudentGrade("11");
        gradAlgorithmGraduationStatus.setStudentStatus("A");

        School schoolObj = new School();
        schoolObj.setMinCode("1231123");
        schoolObj.setIndependentDesignation("3");

        GraduationData graduationDataStatus = new GraduationData();
        graduationDataStatus.setDualDogwood(false);
        graduationDataStatus.setGradMessage("Graduated");
        graduationDataStatus.setGradStatus(gradAlgorithmGraduationStatus);
        graduationDataStatus.setGraduated(true);
        graduationDataStatus.setSchool(schoolObj);
        graduationDataStatus.setStudentCourses(null);

        List<ProgramCertificateTranscript> clist= new ArrayList<ProgramCertificateTranscript>();
        ProgramCertificateTranscript pc = new ProgramCertificateTranscript();
        pc.setCertificateTypeCode("E");
        pc.setSchoolCategoryCode(" ");
        clist.add(pc);

        when(this.restUtils.getProgramCertificateTranscriptList(any(), eq(accessToken))).thenReturn(clist);

        List<GradAlgorithmOptionalStudentProgram> optionalGradStatus = new ArrayList<>();
        graduationDataStatus.setOptionalGradStatus(optionalGradStatus);

        List<ProgramCertificateTranscript> listCC = reportService.getCertificateList(graduationDataStatus, comSchObj.getSchoolCategoryCode(), accessToken);
        assertThat(listCC).hasSize(1);
    }

    @Test
    public void testGetCertificateList_FrenchImmersion() {
        String studentID = new UUID(1, 1).toString();
        String accessToken = "accessToken";
        GradAlgorithmGraduationStudentRecord gradResponse = new GradAlgorithmGraduationStudentRecord();
        gradResponse.setPen("123090109");
        gradResponse.setProgram("2018-EN");
        gradResponse.setProgramCompletionDate(null);
        gradResponse.setSchoolOfRecord("06011033");
        gradResponse.setStudentGrade("11");
        gradResponse.setStudentStatus("D");

        GradAlgorithmGraduationStudentRecord gradAlgorithmGraduationStatus = new GradAlgorithmGraduationStudentRecord();
        gradAlgorithmGraduationStatus.setPen("123090109");
        gradAlgorithmGraduationStatus.setProgram("2018-EN");
        gradAlgorithmGraduationStatus.setProgramCompletionDate(null);
        gradAlgorithmGraduationStatus.setSchoolOfRecord("06011033");
        gradAlgorithmGraduationStatus.setStudentGrade("11");
        gradAlgorithmGraduationStatus.setStudentStatus("A");

        School schoolObj = new School();
        schoolObj.setMinCode("1231123");
        schoolObj.setIndependentDesignation("3");

        GraduationData graduationDataStatus = new GraduationData();
        graduationDataStatus.setDualDogwood(false);
        graduationDataStatus.setGradMessage("EN Graduated");
        graduationDataStatus.setGradStatus(gradAlgorithmGraduationStatus);
        graduationDataStatus.setGraduated(true);
        graduationDataStatus.setSchool(schoolObj);
        graduationDataStatus.setStudentCourses(null);

        GradAlgorithmOptionalStudentProgram spgm = new GradAlgorithmOptionalStudentProgram();
        spgm.setPen("123090109");
        spgm.setOptionalProgramCode("BD");
        spgm.setOptionalProgramName("International Bacculaurette Diploma");
        spgm.setStudentID(UUID.fromString(studentID));
        List<GradAlgorithmOptionalStudentProgram> list = new ArrayList<>();
        list.add(spgm);

        graduationDataStatus.setOptionalGradStatus(list);

        List<ProgramCertificateTranscript> clist= new ArrayList<ProgramCertificateTranscript>();
        ProgramCertificateTranscript pc = new ProgramCertificateTranscript();
        pc.setCertificateTypeCode("E");
        pc.setSchoolCategoryCode(" ");
        clist.add(pc);

        when(this.restUtils.getProgramCertificateTranscriptList(any(), eq(accessToken))).thenReturn(clist);

        CommonSchool comSchObj = new CommonSchool();
        comSchObj.setDistNo("123");
        comSchObj.setSchlNo("1123");
        comSchObj.setSchoolCategoryCode("02");

        when(this.restUtils.getSchoolCategoryCode("06011033", accessToken)).thenReturn(comSchObj.getSchoolCategoryCode());

        List<ProgramCertificateTranscript> listCC =  reportService.getCertificateList(graduationDataStatus, comSchObj.getSchoolCategoryCode(), accessToken);
        assertThat(listCC).hasSize(1);
    }

    @Test
    public void testGetCertificateList_FrenchImmersion_nullProgramCompletionDate() {
        String studentID = new UUID(1, 1).toString();
        String accessToken = "accessToken";
        GradAlgorithmGraduationStudentRecord gradResponse = new GradAlgorithmGraduationStudentRecord();
        gradResponse.setPen("123090109");
        gradResponse.setProgram("2018-EN");
        gradResponse.setProgramCompletionDate(null);
        gradResponse.setSchoolOfRecord("06011033");
        gradResponse.setStudentGrade("11");
        gradResponse.setStudentStatus("D");

        GradAlgorithmGraduationStudentRecord gradAlgorithmGraduationStatus = new GradAlgorithmGraduationStudentRecord();
        gradAlgorithmGraduationStatus.setPen("123090109");
        gradAlgorithmGraduationStatus.setProgram("2018-EN");
        gradAlgorithmGraduationStatus.setProgramCompletionDate(null);
        gradAlgorithmGraduationStatus.setSchoolOfRecord("06011033");
        gradAlgorithmGraduationStatus.setStudentGrade("11");
        gradAlgorithmGraduationStatus.setStudentStatus("A");

        School schoolObj = new School();
        schoolObj.setMinCode("1231123");
        schoolObj.setIndependentDesignation("3");

        GraduationData graduationDataStatus = new GraduationData();
        graduationDataStatus.setDualDogwood(false);
        graduationDataStatus.setGradMessage("EN Graduated");
        graduationDataStatus.setGradStatus(gradAlgorithmGraduationStatus);
        graduationDataStatus.setGraduated(true);
        graduationDataStatus.setSchool(schoolObj);
        graduationDataStatus.setStudentCourses(null);

        GradAlgorithmOptionalStudentProgram spgm = new GradAlgorithmOptionalStudentProgram();
        spgm.setPen("123090109");
        spgm.setOptionalProgramCode("FI");
        spgm.setOptionalProgramName("French Immersion");
        spgm.setStudentID(UUID.fromString(studentID));
        spgm.setOptionalProgramCompletionDate(null);
        List<GradAlgorithmOptionalStudentProgram> list = new ArrayList<GradAlgorithmOptionalStudentProgram>();
        list.add(spgm);

        graduationDataStatus.setOptionalGradStatus(list);

        List<ProgramCertificateTranscript> clist= new ArrayList<ProgramCertificateTranscript>();
        ProgramCertificateTranscript pc = new ProgramCertificateTranscript();
        pc.setCertificateTypeCode("E");
        pc.setSchoolCategoryCode("02");
        clist.add(pc);

        when(this.restUtils.getProgramCertificateTranscriptList(any(), eq(accessToken))).thenReturn(clist);

        CommonSchool comSchObj = new CommonSchool();
        comSchObj.setDistNo("123");
        comSchObj.setSchlNo("1123");
        comSchObj.setSchoolCategoryCode("02");

        when(this.restUtils.getSchoolCategoryCode("06011033", accessToken)).thenReturn(comSchObj.getSchoolCategoryCode());

        List<ProgramCertificateTranscript> listCC = reportService.getCertificateList(graduationDataStatus, comSchObj.getSchoolCategoryCode(), accessToken);
        assertThat(listCC).hasSize(1);
    }

    @Test
    public void testPrepareReportData() {
        String accessToken = "accessToken";
        GradAlgorithmGraduationStudentRecord gradAlgorithmGraduationStatus = new GradAlgorithmGraduationStudentRecord();
        gradAlgorithmGraduationStatus.setPen("123090109");
        gradAlgorithmGraduationStatus.setProgram("2018-EN");
        gradAlgorithmGraduationStatus.setProgramCompletionDate(null);
        gradAlgorithmGraduationStatus.setSchoolOfRecord("06011033");
        gradAlgorithmGraduationStatus.setStudentGrade("11");
        gradAlgorithmGraduationStatus.setStudentStatus("A");

        School schoolObj = new School();
        schoolObj.setMinCode("1231123");
        schoolObj.setIndependentDesignation("1");

        GradSearchStudent stuObj = new GradSearchStudent();
        stuObj.setPen("123123123");
        stuObj.setLegalFirstName("ABC");
        stuObj.setLegalLastName("FDG");
        stuObj.setSchoolOfRecord("12321321");

        StudentCourse sc= new StudentCourse();
        sc.setCourseCode("FDFE");
        sc.setCourseName("DERQ WEW");
        sc.setCreditsUsedForGrad(4);
        sc.setCredits(4);
        sc.setCustomizedCourseName("SREE");
        sc.setSessionDate("1990/12");
        sc.setEquivOrChallenge("E");
        sc.setProvExamCourse("Y");
        sc.setSpecialCase("A");
        sc.setCourseLevel("11");
        sc.setBestExamPercent(60.8D);
        sc.setBestSchoolPercent(90.3D);
        List<StudentCourse> sList= new ArrayList<>();
        sList.add(sc);
        StudentCourses sCourses = new StudentCourses();
        sCourses.setStudentCourseList(sList);

        StudentAssessment sA= new StudentAssessment();
        sA.setAssessmentCode("FDFE");
        sA.setAssessmentName("AASASA");
        sA.setSessionDate("2020/12");
        sA.setSpecialCase("A");
        List<StudentAssessment> aList= new ArrayList<>();
        aList.add(sA);
        StudentAssessments sAssessments = new StudentAssessments();
        sAssessments.setStudentAssessmentList(aList);

        GraduationProgramCode gpCode = new GraduationProgramCode();
        gpCode.setProgramCode("2018-EN");
        gpCode.setProgramName("2018 Graduation Program");

        GraduationData graduationDataStatus = new GraduationData();
        graduationDataStatus.setDualDogwood(false);
        graduationDataStatus.setGradMessage("EN Graduated");
        graduationDataStatus.setGradStatus(gradAlgorithmGraduationStatus);
        graduationDataStatus.setGraduated(true);
        graduationDataStatus.setSchool(schoolObj);
        graduationDataStatus.setStudentCourses(sCourses);
        graduationDataStatus.setStudentAssessments(sAssessments);
        graduationDataStatus.setGradStudent(stuObj);
        graduationDataStatus.setGradProgram(gpCode);

        GradAlgorithmOptionalStudentProgram spgm = new GradAlgorithmOptionalStudentProgram();
        spgm.setPen("123123123");
        spgm.setOptionalProgramCode("FI");
        spgm.setOptionalProgramName("French Immersion");
        spgm.setStudentID(UUID.randomUUID());
        spgm.setOptionalProgramCompletionDate("2020-09-01");
        List<GradAlgorithmOptionalStudentProgram> list = new ArrayList<>();
        list.add(spgm);

        graduationDataStatus.setOptionalGradStatus(list);

        GradProgram gP = new GradProgram();
        gP.setProgramCode("2018-EN");
        gP.setProgramName("2018 Graduation Program");

        SpecialCase sp = new SpecialCase();
        sp.setSpCase("A");
        sp.setLabel("AEG");

        when(this.restUtils.getSpecialCase("A", accessToken)).thenReturn(sp);
        when(this.restUtils.getGradProgram(gradAlgorithmGraduationStatus.getProgram(), accessToken)).thenReturn(gP);

        CommonSchool commSch = new CommonSchool();
        commSch.setSchlNo("1231123");
        commSch.setSchoolCategoryCode("02");

        when(this.restUtils.getSchoolCategoryCode("06011033", accessToken)).thenReturn(commSch.getSchoolCategoryCode());

        ProgramCertificateTranscript programCertificateTranscript = new ProgramCertificateTranscript();
        programCertificateTranscript.setPcId(UUID.randomUUID());
        programCertificateTranscript.setGraduationProgramCode(gP.getProgramCode());
        programCertificateTranscript.setSchoolCategoryCode(commSch.getSchoolCategoryCode());
        programCertificateTranscript.setCertificateTypeCode("E");

        when(this.restUtils.getTranscript(gP.getProgramCode(), commSch.getSchoolCategoryCode(), accessToken)).thenReturn(programCertificateTranscript);

        GradAlgorithmGraduationStudentRecord gradResponse = new GradAlgorithmGraduationStudentRecord();
        gradResponse.setPen("123090109");
        gradResponse.setProgram("2018-EN");
        gradResponse.setProgramCompletionDate(null);
        gradResponse.setSchoolOfRecord("06011033");
        gradResponse.setStudentGrade("11");
        gradResponse.setStudentStatus("D");
        gradResponse.setLastUpdateDate(new Date(System.currentTimeMillis()));

        ConvGradStudent student = ConvGradStudent.builder().pen("123090109").program("2018-EN")
                .studentStatus("A").schoolOfRecord("06011033").graduationRequirementYear("2018")
                .transcriptSchool(schoolObj)
                .certificateSchool(schoolObj)
                .transcriptSchoolCategoryCode(commSch.getSchoolCategoryCode())
                .certificateSchoolCategoryCode(commSch.getSchoolCategoryCode())
                .programCodes(Arrays.asList("DD")).build();

        ReportData dta = reportService.prepareTranscriptData(graduationDataStatus, gradResponse, student, accessToken);
        assertThat(dta).isNotNull();
    }

    @Test
    public void testPrepareReportData_nullProgramData() {
        String accessToken = "accessToken";
        GradAlgorithmGraduationStudentRecord gradAlgorithmGraduationStatus = getGradAlgorithmGraduationStatus("2018-EN");

        School schoolObj = new School();
        schoolObj.setMinCode("1231123");
        schoolObj.setIndependentDesignation("1");

        GradSearchStudent stuObj = getStudentObj();
        StudentCourses sCourses = new StudentCourses();
        sCourses.setStudentCourseList(getStudentCourses(4,4));

        StudentAssessments sAssessments = new StudentAssessments();
        sAssessments.setStudentAssessmentList(getStudentAssessments());

        GraduationData graduationDataStatus = new GraduationData();
        graduationDataStatus.setDualDogwood(false);
        graduationDataStatus.setGradMessage("EN Graduated");
        graduationDataStatus.setGradStatus(gradAlgorithmGraduationStatus);
        graduationDataStatus.setGraduated(true);
        graduationDataStatus.setSchool(schoolObj);
        graduationDataStatus.setStudentCourses(sCourses);
        graduationDataStatus.setStudentAssessments(sAssessments);
        graduationDataStatus.setGradStudent(stuObj);

        GradProgram gP = new GradProgram();
        gP.setProgramCode("2018-EN");
        gP.setProgramName("2018 Graduation Program");

        GraduationProgramCode gpCode = new GraduationProgramCode();
        gpCode.setProgramCode("2018-EN");
        gpCode.setProgramName("2018 Graduation Program");

        graduationDataStatus.setGradProgram(gpCode);

        SpecialCase sp = new SpecialCase();
        sp.setSpCase("A");
        sp.setLabel("AEG");

        when(this.restUtils.getSpecialCase("A", accessToken)).thenReturn(sp);
        when(this.restUtils.getGradProgram(gradAlgorithmGraduationStatus.getProgram(), accessToken)).thenReturn(gP);

        CommonSchool commSch = new CommonSchool();
        commSch.setSchlNo("1231123");
        commSch.setSchoolCategoryCode("02");

        when(this.restUtils.getSchoolCategoryCode("06011033", accessToken)).thenReturn(commSch.getSchoolCategoryCode());

        GradAlgorithmGraduationStudentRecord gradResponse = new GradAlgorithmGraduationStudentRecord();
        gradResponse.setPen("123090109");
        gradResponse.setProgram("2018-EN");
        gradResponse.setProgramCompletionDate(null);
        gradResponse.setSchoolOfRecord("06011033");
        gradResponse.setStudentGrade("11");
        gradResponse.setStudentStatus("D");
        gradResponse.setLastUpdateDate(new Date(System.currentTimeMillis()));

        ConvGradStudent student = ConvGradStudent.builder().pen("123090109").program("2018-EN")
                .studentStatus("A").schoolOfRecord("06011033").graduationRequirementYear("2018")
                .transcriptSchool(schoolObj)
                .certificateSchool(schoolObj)
                .transcriptSchoolCategoryCode(commSch.getSchoolCategoryCode())
                .certificateSchoolCategoryCode(commSch.getSchoolCategoryCode())
                .programCodes(Arrays.asList("DD")).build();

        ReportData dta = reportService.prepareTranscriptData(graduationDataStatus,gradResponse, student, accessToken);
        assertThat(dta).isNotNull();
    }

    @Test
    public void testPrepareReportData_exams_notnull() {
        String accessToken = "accessToken";
        testPrepareReportData_exams_notnull("1996-EN",accessToken);
        testPrepareReportData_exams_notnull("1950",accessToken);
        testPrepareReportData_exams_notnull("2004-EN",accessToken);
    }


    public void testPrepareReportData_exams_notnull(String program,String accessToken) {

        GradAlgorithmGraduationStudentRecord gradAlgorithmGraduationStatus = getGradAlgorithmGraduationStatus(program);

        School schoolObj = new School();
        schoolObj.setMinCode("1231123");
        schoolObj.setIndependentDesignation("1");

        GradSearchStudent stuObj = getStudentObj();

        StudentCourses sCourses = new StudentCourses();
        sCourses.setStudentCourseList(getStudentCourses(2,4));

        StudentExam se= new StudentExam();
        se.setCourseCode("FDFE");
        List<StudentExam> eList= new ArrayList<>();
        eList.add(se);
        StudentExams eCourses = new StudentExams();
        eCourses.setStudentExamList(eList);

        StudentAssessments sAssessments = new StudentAssessments();
        sAssessments.setStudentAssessmentList(getStudentAssessments());

        GraduationData graduationDataStatus = new GraduationData();
        graduationDataStatus.setDualDogwood(false);
        graduationDataStatus.setGradMessage("EN Graduated");
        graduationDataStatus.setGradStatus(gradAlgorithmGraduationStatus);
        graduationDataStatus.setGraduated(true);
        graduationDataStatus.setSchool(schoolObj);
        graduationDataStatus.setStudentCourses(sCourses);
        graduationDataStatus.setStudentAssessments(sAssessments);
        graduationDataStatus.setStudentExams(eCourses);
        graduationDataStatus.setGradStudent(stuObj);

        GradProgram gP = new GradProgram();
        gP.setProgramCode(program);
        gP.setProgramName("2018 Graduation Program");

        GraduationProgramCode gpCode = new GraduationProgramCode();
        gpCode.setProgramCode("2018-EN");
        gpCode.setProgramName("2018 Graduation Program");

        graduationDataStatus.setGradProgram(gpCode);

        SpecialCase sp = new SpecialCase();
        sp.setSpCase("A");
        sp.setLabel("AEG");

        when(this.restUtils.getSpecialCase("A", accessToken)).thenReturn(sp);
        when(this.restUtils.getGradProgram(gradAlgorithmGraduationStatus.getProgram(), accessToken)).thenReturn(gP);

        CommonSchool commSch = new CommonSchool();
        commSch.setSchlNo("06011033");
        commSch.setSchoolCategoryCode("02");

        when(this.restUtils.getSchoolCategoryCode("06011033", accessToken)).thenReturn(commSch.getSchoolCategoryCode());

        GradAlgorithmGraduationStudentRecord gradResponse = new GradAlgorithmGraduationStudentRecord();
        gradResponse.setPen("123090109");
        gradResponse.setProgram(program);
        gradResponse.setProgramCompletionDate(null);
        gradResponse.setSchoolOfRecord("06011033");
        gradResponse.setHonoursStanding("Y");
        gradResponse.setStudentGrade("11");
        gradResponse.setStudentStatus("D");
        gradResponse.setLastUpdateDate(new Date(System.currentTimeMillis()));

        ConvGradStudent student = ConvGradStudent.builder().pen("123090109").program("2018-EN")
                .studentStatus("A").schoolOfRecord("06011033").graduationRequirementYear("2018")
                .transcriptSchool(schoolObj)
                .certificateSchool(schoolObj)
                .transcriptSchoolCategoryCode(commSch.getSchoolCategoryCode())
                .certificateSchoolCategoryCode(commSch.getSchoolCategoryCode())
                .programCodes(Arrays.asList("DD")).build();

        ReportData dta = reportService.prepareTranscriptData(graduationDataStatus,gradResponse,student,accessToken);
        assertThat(dta).isNotNull();
    }

    @Test
    public void testPrepareReportData_Desig_3() {
        String accessToken = "accessToken";
        UUID studentID = UUID.randomUUID();
        GradAlgorithmGraduationStudentRecord gradAlgorithmGraduationStatus = new GradAlgorithmGraduationStudentRecord();
        gradAlgorithmGraduationStatus.setPen("123090109");
        gradAlgorithmGraduationStatus.setProgram("2018-EN");
        gradAlgorithmGraduationStatus.setProgramCompletionDate(null);
        gradAlgorithmGraduationStatus.setSchoolOfRecord("06011033");
        gradAlgorithmGraduationStatus.setStudentGrade("11");
        gradAlgorithmGraduationStatus.setStudentStatus("A");

        School schoolObj = new School();
        schoolObj.setMinCode("1231123");
        schoolObj.setIndependentDesignation("3");

        GradSearchStudent stuObj = new GradSearchStudent();
        stuObj.setPen("123123123");
        stuObj.setLegalFirstName("ABC");
        stuObj.setLegalLastName("FDG");
        stuObj.setSchoolOfRecord("12321321");

        StudentCourse sc= new StudentCourse();
        sc.setCourseCode("FDFE");
        sc.setCourseName("FDFE FREE");
        sc.setCreditsUsedForGrad(4);
        sc.setCredits(4);
        sc.setProvExamCourse("Y");
        sc.setCustomizedCourseName("SREE");
        sc.setSessionDate("2020/12");
        sc.setEquivOrChallenge("E");
        sc.setCourseLevel("11");
        sc.setBestExamPercent(60.8D);
        sc.setBestSchoolPercent(90.3D);
        List<StudentCourse> sList= new ArrayList<>();
        sList.add(sc);
        StudentCourses sCourses = new StudentCourses();
        sCourses.setStudentCourseList(sList);

        StudentAssessment sA= new StudentAssessment();
        sA.setAssessmentCode("FDFE");
        sA.setAssessmentName("AASASA");
        sA.setSessionDate("2020/12");
        sA.setSpecialCase("A");
        List<StudentAssessment> aList= new ArrayList<>();
        aList.add(sA);
        StudentAssessments sAssessments = new StudentAssessments();
        sAssessments.setStudentAssessmentList(aList);

        GraduationData graduationDataStatus = new GraduationData();
        graduationDataStatus.setDualDogwood(false);
        graduationDataStatus.setGradMessage("EN Graduated");
        graduationDataStatus.setGradStatus(gradAlgorithmGraduationStatus);
        graduationDataStatus.setGraduated(true);
        graduationDataStatus.setSchool(schoolObj);
        graduationDataStatus.setStudentCourses(sCourses);
        graduationDataStatus.setStudentAssessments(sAssessments);
        graduationDataStatus.setGradStudent(stuObj);

        GradAlgorithmOptionalStudentProgram spgm = new GradAlgorithmOptionalStudentProgram();
        spgm.setPen("123123123");
        spgm.setOptionalProgramCode("BD");
        spgm.setOptionalProgramName("International Bacculaurette Diploma");
        spgm.setStudentID(studentID);
        List<GradAlgorithmOptionalStudentProgram> list = new ArrayList<>();
        list.add(spgm);

        graduationDataStatus.setOptionalGradStatus(list);

        GradProgram gP = new GradProgram();
        gP.setProgramCode("2018-EN");
        gP.setProgramName("2018 Graduation Program");

        GraduationProgramCode gpCode = new GraduationProgramCode();
        gpCode.setProgramCode("2018-EN");
        gpCode.setProgramName("2018 Graduation Program");

        graduationDataStatus.setGradProgram(gpCode);

        SpecialCase sp = new SpecialCase();
        sp.setSpCase("A");
        sp.setLabel("AEG");

        when(this.restUtils.getSpecialCase("A", accessToken)).thenReturn(sp);
        when(this.restUtils.getGradProgram(gradAlgorithmGraduationStatus.getProgram(), accessToken)).thenReturn(gP);

        CommonSchool commSch = new CommonSchool();
        commSch.setSchlNo("1231123");
        commSch.setSchoolCategoryCode("02");

        when(this.restUtils.getSchoolCategoryCode("06011033", accessToken)).thenReturn(commSch.getSchoolCategoryCode());

        GradAlgorithmGraduationStudentRecord gradResponse = new GradAlgorithmGraduationStudentRecord();
        gradResponse.setPen("123090109");
        gradResponse.setProgram("2018-EN");
        gradResponse.setProgramCompletionDate(null);
        gradResponse.setSchoolOfRecord("06011033");
        gradResponse.setStudentGrade("11");
        gradResponse.setStudentStatus("D");
        gradResponse.setLastUpdateDate(new Date(System.currentTimeMillis()));

        ConvGradStudent student = ConvGradStudent.builder().pen("123090109").program("2018-EN")
                .studentStatus("A").schoolOfRecord("06011033").graduationRequirementYear("2018")
                .transcriptSchool(schoolObj)
                .certificateSchool(schoolObj)
                .transcriptSchoolCategoryCode(commSch.getSchoolCategoryCode())
                .certificateSchoolCategoryCode(commSch.getSchoolCategoryCode())
                .programCodes(Arrays.asList("DD")).build();

        ReportData dta = reportService.prepareTranscriptData(graduationDataStatus,gradResponse,student, accessToken);
        assertThat(dta).isNotNull();
    }

    @Test
    public void testPrepareReportData_Desig_4() {
        String accessToken = "accessToken";
        GradAlgorithmGraduationStudentRecord gradAlgorithmGraduationStatus = new GradAlgorithmGraduationStudentRecord();
        gradAlgorithmGraduationStatus.setPen("123090109");
        gradAlgorithmGraduationStatus.setProgram("2018-EN");
        gradAlgorithmGraduationStatus.setProgramCompletionDate(null);
        gradAlgorithmGraduationStatus.setSchoolOfRecord("06011033");
        gradAlgorithmGraduationStatus.setStudentGrade("11");
        gradAlgorithmGraduationStatus.setStudentStatus("A");

        School schoolObj = new School();
        schoolObj.setMinCode("1231123");
        schoolObj.setIndependentDesignation("4");

        GradSearchStudent stuObj = new GradSearchStudent();
        stuObj.setPen("123123123");
        stuObj.setLegalFirstName("ABC");
        stuObj.setLegalLastName("FDG");
        stuObj.setSchoolOfRecord("12321321");

        StudentCourse sc= new StudentCourse();
        sc.setCourseCode("FDFE");
        sc.setCourseName("FDFE FREE");
        sc.setCreditsUsedForGrad(2);
        sc.setCredits(4);
        sc.setCustomizedCourseName("SREE");
        sc.setSessionDate("2020/12");
        sc.setEquivOrChallenge("E");
        sc.setSpecialCase("A");
        sc.setProvExamCourse("N");
        sc.setCourseLevel("11");
        sc.setBestExamPercent(60.8D);
        sc.setBestSchoolPercent(90.3D);
        List<StudentCourse> sList= new ArrayList<>();
        sList.add(sc);
        StudentCourses sCourses = new StudentCourses();
        sCourses.setStudentCourseList(sList);

        StudentAssessment sA= new StudentAssessment();
        sA.setAssessmentCode("FDFE");
        sA.setAssessmentName("AASASA");
        sA.setSessionDate("2020/12");
        sA.setSpecialCase("A");
        List<StudentAssessment> aList= new ArrayList<>();
        aList.add(sA);
        StudentAssessments sAssessments = new StudentAssessments();
        sAssessments.setStudentAssessmentList(aList);

        GraduationData graduationDataStatus = new GraduationData();
        graduationDataStatus.setDualDogwood(false);
        graduationDataStatus.setGradMessage("EN Graduated");
        graduationDataStatus.setGradStatus(gradAlgorithmGraduationStatus);
        graduationDataStatus.setGraduated(true);
        graduationDataStatus.setSchool(schoolObj);
        graduationDataStatus.setStudentCourses(sCourses);
        graduationDataStatus.setStudentAssessments(sAssessments);
        graduationDataStatus.setGradStudent(stuObj);

        GradAlgorithmOptionalStudentProgram spgm = new GradAlgorithmOptionalStudentProgram();
        spgm.setPen("123123123");
        spgm.setOptionalProgramCode("CP");
        spgm.setOptionalProgramName("Career Program");
        spgm.setStudentID(UUID.randomUUID());
        List<GradAlgorithmOptionalStudentProgram> list = new ArrayList<>();
        list.add(spgm);

        graduationDataStatus.setOptionalGradStatus(list);

        GradProgram gP = new GradProgram();
        gP.setProgramCode("2018-EN");
        gP.setProgramName("2018 Graduation Program");

        GraduationProgramCode gpCode = new GraduationProgramCode();
        gpCode.setProgramCode("2018-EN");
        gpCode.setProgramName("2018 Graduation Program");

        graduationDataStatus.setGradProgram(gpCode);

        SpecialCase sp = new SpecialCase();
        sp.setSpCase("A");
        sp.setLabel("AEG");

        when(this.restUtils.getSpecialCase("A", accessToken)).thenReturn(sp);
        when(this.restUtils.getGradProgram(gradAlgorithmGraduationStatus.getProgram(), accessToken)).thenReturn(gP);

        CommonSchool commSch = new CommonSchool();
        commSch.setSchlNo("1231123");
        commSch.setSchoolCategoryCode("02");

        when(this.restUtils.getSchoolCategoryCode("06011033", accessToken)).thenReturn(commSch.getSchoolCategoryCode());

        GradAlgorithmGraduationStudentRecord gradResponse = new GradAlgorithmGraduationStudentRecord();
        gradResponse.setPen("123090109");
        gradResponse.setProgram("2018-EN");
        gradResponse.setProgramCompletionDate(null);
        gradResponse.setSchoolOfRecord("06011033");
        gradResponse.setStudentGrade("11");
        gradResponse.setStudentStatus("D");
        gradResponse.setLastUpdateDate(new Date(System.currentTimeMillis()));

        ConvGradStudent student = ConvGradStudent.builder().pen("123090109").program("2018-EN")
                .studentStatus("A").schoolOfRecord("06011033").graduationRequirementYear("2018")
                .transcriptSchool(schoolObj)
                .certificateSchool(schoolObj)
                .transcriptSchoolCategoryCode(commSch.getSchoolCategoryCode())
                .certificateSchoolCategoryCode(commSch.getSchoolCategoryCode())
                .programCodes(Arrays.asList("DD")).build();

        ReportData dta = reportService.prepareTranscriptData(graduationDataStatus,gradResponse,student,accessToken);
        assertThat(dta).isNotNull();
    }


    public void testPrepareReportData_Desig_2() {
        String accessToken = "accessToken";
        GradAlgorithmGraduationStudentRecord gradAlgorithmGraduationStatus = new GradAlgorithmGraduationStudentRecord();
        gradAlgorithmGraduationStatus.setPen("123090109");
        gradAlgorithmGraduationStatus.setProgram("2018-EN");
        gradAlgorithmGraduationStatus.setProgramCompletionDate(null);
        gradAlgorithmGraduationStatus.setSchoolOfRecord("06011033");
        gradAlgorithmGraduationStatus.setStudentGrade("11");
        gradAlgorithmGraduationStatus.setStudentStatus("A");

        School schoolObj = new School();
        schoolObj.setMinCode("1231123");
        schoolObj.setIndependentDesignation("2");

        GradSearchStudent stuObj = new GradSearchStudent();
        stuObj.setPen("123123123");
        stuObj.setLegalFirstName("ABC");
        stuObj.setLegalLastName("FDG");
        stuObj.setSchoolOfRecord("12321321");

        StudentCourse sc= new StudentCourse();
        sc.setCourseCode("FDFE");
        sc.setCredits(4);
        sc.setCustomizedCourseName("SREE");
        sc.setSessionDate("2020/12");
        sc.setEquivOrChallenge("E");
        List<StudentCourse> sList= new ArrayList<>();
        sList.add(sc);
        StudentCourses sCourses = new StudentCourses();
        sCourses.setStudentCourseList(sList);

        StudentAssessment sA= new StudentAssessment();
        sA.setAssessmentCode("FDFE");
        sA.setAssessmentName("AASASA");
        sA.setSessionDate("2020/12");
        sA.setSpecialCase("A");
        List<StudentAssessment> aList= new ArrayList<>();
        aList.add(sA);
        StudentAssessments sAssessments = new StudentAssessments();
        sAssessments.setStudentAssessmentList(aList);

        GraduationData graduationDataStatus = new GraduationData();
        graduationDataStatus.setDualDogwood(false);
        graduationDataStatus.setGradMessage("DD Graduated");
        graduationDataStatus.setGradStatus(gradAlgorithmGraduationStatus);
        graduationDataStatus.setGraduated(true);
        graduationDataStatus.setSchool(schoolObj);
        graduationDataStatus.setStudentCourses(sCourses);
        graduationDataStatus.setStudentAssessments(sAssessments);
        graduationDataStatus.setGradStudent(stuObj);

        GradAlgorithmOptionalStudentProgram spgm = new GradAlgorithmOptionalStudentProgram();
        spgm.setPen("123123123");
        spgm.setOptionalProgramCode("FI");
        spgm.setOptionalProgramName("French Immersion");
        spgm.setStudentID(UUID.randomUUID());
        List<GradAlgorithmOptionalStudentProgram> list = new ArrayList<>();
        list.add(spgm);

        GradProgram gP = new GradProgram();
        gP.setProgramCode("2018-EN");
        gP.setProgramName("2018 Graduation Program");

        when(this.restUtils.getGradProgram(gradAlgorithmGraduationStatus.getProgram(), accessToken)).thenReturn(gP);

        GradAlgorithmGraduationStudentRecord gradResponse = new GradAlgorithmGraduationStudentRecord();
        gradResponse.setPen("123090109");
        gradResponse.setProgram("2018-EN");
        gradResponse.setProgramCompletionDate(null);
        gradResponse.setSchoolOfRecord("06011033");
        gradResponse.setStudentGrade("11");
        gradResponse.setStudentStatus("D");
        gradResponse.setLastUpdateDate(new Date(System.currentTimeMillis()));

        ConvGradStudent student = ConvGradStudent.builder().pen("123090109").program("2018-EN")
                .studentStatus("A").schoolOfRecord("06011033").graduationRequirementYear("2018")
                .transcriptSchool(schoolObj)
                .certificateSchool(schoolObj)
//                .schoolCategoryCode(commSch.getSchoolCategoryCode())
                .programCodes(Arrays.asList("DD")).build();

        ReportData dta = reportService.prepareTranscriptData(graduationDataStatus,gradResponse,student,accessToken);
        assertThat(dta).isNotNull();
    }


    private GradAlgorithmGraduationStudentRecord getGradAlgorithmGraduationStatus(String program) {
        GradAlgorithmGraduationStudentRecord gradAlgorithmGraduationStatus = new GradAlgorithmGraduationStudentRecord();
        gradAlgorithmGraduationStatus.setPen("123090109");
        gradAlgorithmGraduationStatus.setProgram(program);
        gradAlgorithmGraduationStatus.setProgramCompletionDate(null);
        gradAlgorithmGraduationStatus.setSchoolOfRecord("06011033");
        gradAlgorithmGraduationStatus.setStudentGrade("11");
        gradAlgorithmGraduationStatus.setStudentStatus("A");
        return gradAlgorithmGraduationStatus;
    }

    private GradSearchStudent getStudentObj () {
        GradSearchStudent stuObj = new GradSearchStudent();
        stuObj.setPen("123123123");
        stuObj.setLegalFirstName("ABC");
        stuObj.setLegalLastName("FDG");
        stuObj.setSchoolOfRecord("12321321");
        return stuObj;
    }

    private List<StudentCourse> getStudentCourses(int totalCredits,int originalCredits) {
        StudentCourse sc= new StudentCourse();
        sc.setCourseCode("FDFE FE");
        sc.setCourseName("FEREE FREE");
        sc.setCourseLevel("11");
        sc.setCredits(totalCredits);
        sc.setOriginalCredits(originalCredits);
        sc.setCreditsUsedForGrad(2);
        sc.setCustomizedCourseName("SREE");
        sc.setSessionDate("2020/12");
        sc.setFineArtsAppliedSkills("B");
        sc.setEquivOrChallenge("E");
        sc.setSpecialCase("F");
        sc.setRestricted(true);
        sc.setProvExamCourse("N");
        sc.setBestExamPercent(50.0D);
        sc.setBestSchoolPercent(50.0D);
        List<StudentCourse> sList= new ArrayList<>();
        sList.add(sc);
        sc= new StudentCourse();
        sc.setCourseCode("CPUY");
        sc.setCourseName("CP FEREE FREE");
        sc.setCourseLevel("12A");
        sc.setCredits(totalCredits);
        sc.setOriginalCredits(originalCredits);
        sc.setCreditsUsedForGrad(2);
        sc.setCustomizedCourseName("SREE");
        sc.setSessionDate("2020/12");
        sc.setFineArtsAppliedSkills("B");
        sc.setEquivOrChallenge("E");
        sc.setSpecialCase("F");
        sc.setProvExamCourse("Y");
        sc.setRestricted(true);
        sList.add(sc);
        sc= new StudentCourse();
        sc.setCourseCode("CPUY FR");
        sc.setCourseName("CP FEREE FREE");
        sc.setCourseLevel("12A");
        sc.setCredits(totalCredits);
        sc.setSessionDate("1990/11");
        sc.setOriginalCredits(originalCredits);
        sc.setCreditsUsedForGrad(2);
        sc.setBestSchoolPercent(89D);
        sc.setCustomizedCourseName("SREE");
        sc.setSessionDate("2020/12");
        sc.setFineArtsAppliedSkills("B");
        sc.setEquivOrChallenge("E");
        sc.setSpecialCase("F");
        sc.setProvExamCourse("Y");
        sc.setRestricted(true);
        sList.add(sc);
        sc= new StudentCourse();
        sc.setCourseCode("CPUY ZS");
        sc.setCourseName("CP FEREE FREE");
        sc.setCourseLevel("12C");
        sc.setCredits(totalCredits);
        sc.setOriginalCredits(originalCredits);
        sc.setCreditsUsedForGrad(2);
        sc.setBestSchoolPercent(89D);
        sc.setCustomizedCourseName("SREE");
        sc.setSessionDate("2020/12");
        sc.setFineArtsAppliedSkills("B");
        sc.setEquivOrChallenge("E");
        sc.setSpecialCase("F");
        sc.setProvExamCourse("Y");
        sc.setRestricted(true);
        sList.add(sc);
        return sList;
    }

    private List<StudentAssessment> getStudentAssessments() {
        StudentAssessment sA= new StudentAssessment();
        sA.setAssessmentCode("FDFE");
        sA.setAssessmentName("AASASA");
        sA.setSessionDate("2020/12");
        sA.setSpecialCase("A");
        List<StudentAssessment> aList= new ArrayList<>();
        aList.add(sA);
        return aList;
    }

    @Test
    public void testReportDataByPen() throws Exception {
        GraduationData gradStatus = createGraduationData("json/gradstatus.json");
        assertNotNull(gradStatus);
        String pen = gradStatus.getGradStudent().getPen();
        GradSearchStudent gradSearchStudent = new GradSearchStudent();
        gradSearchStudent.setPen(pen);
        gradSearchStudent.setStudentID(gradStatus.getGradStudent().getStudentID());

        GradAlgorithmGraduationStudentRecord graduationStudentRecord = new GradAlgorithmGraduationStudentRecord();
        graduationStudentRecord.setPen(pen);
        graduationStudentRecord.setProgramCompletionDate("2003/01");
        graduationStudentRecord.setStudentID(UUID.fromString(gradSearchStudent.getStudentID()));
        graduationStudentRecord.setLastUpdateDate(new Date(System.currentTimeMillis()));

        String accessToken = "accessToken";

        String studentGradData = readFile("json/gradstatus.json");
        assertNotNull(studentGradData);
        graduationStudentRecord.setStudentGradData(new ObjectMapper().writeValueAsString(gradStatus));

        GradProgram gradProgram = new GradProgram();
        gradProgram.setProgramCode("2018-EN");
        gradProgram.setProgramName("2018 Graduation Program");

        when(this.restUtils.getGradProgram(gradStatus.getGradStudent().getProgram(), accessToken)).thenReturn(gradProgram);

        School school = new School();
        school.setMinCode("1231123");
        school.setSchoolName("Test School");

        CommonSchool commSch = new CommonSchool();
        commSch.setSchlNo("1231123");
        commSch.setSchoolCategoryCode("02");

        when(this.restUtils.getSchoolCategoryCode("00502001", accessToken)).thenReturn(commSch.getSchoolCategoryCode());

        ProgramCertificateTranscript programCertificateTranscript = new ProgramCertificateTranscript();
        programCertificateTranscript.setPcId(UUID.randomUUID());
        programCertificateTranscript.setGraduationProgramCode(gradProgram.getProgramCode());
        programCertificateTranscript.setSchoolCategoryCode(commSch.getSchoolCategoryCode());
        programCertificateTranscript.setCertificateTypeCode("E");

        ProgramCertificateReq req = new ProgramCertificateReq();
        req.setProgramCode(gradProgram.getProgramCode());
        req.setSchoolCategoryCode(commSch.getSchoolCategoryCode());

        SpecialCase sp = new SpecialCase();
        sp.setSpCase("A");
        sp.setLabel("AEG");

        when(this.restUtils.getSpecialCase("A", accessToken)).thenReturn(sp);
        when(this.restUtils.getTranscript(gradProgram.getProgramCode(), commSch.getSchoolCategoryCode(), accessToken)).thenReturn(programCertificateTranscript);

        GraduationData graduationDataStatus = new GraduationData();
        graduationDataStatus.setDualDogwood(false);
        graduationDataStatus.setGradMessage("Not Graduated");
        graduationDataStatus.setGradStatus(gradStatus.getGradStatus());
        graduationDataStatus.setGraduated(true);
        graduationDataStatus.setSchool(school);
        graduationDataStatus.setStudentCourses(gradStatus.getStudentCourses());
        graduationDataStatus.setStudentAssessments(gradStatus.getStudentAssessments());
        graduationDataStatus.setGradStudent(gradSearchStudent);

        GraduationProgramCode gpCode = new GraduationProgramCode();
        gpCode.setProgramCode("2018-EN");
        gpCode.setProgramName("2018 Graduation Program");

        graduationDataStatus.setGradProgram(gpCode);

        ConvGradStudent student = ConvGradStudent.builder().pen("129382610").program("2018-EN")
                .studentStatus("A").schoolOfRecord("06011033").graduationRequirementYear("2018")
                .transcriptSchool(school)
                .certificateSchool(school)
                .transcriptSchoolCategoryCode(commSch.getSchoolCategoryCode())
                .certificateSchoolCategoryCode(commSch.getSchoolCategoryCode())
                .programCodes(Arrays.asList("DD")).build();

        ReportData transcriptData = reportService.prepareTranscriptData(graduationDataStatus, graduationStudentRecord, student, "accessToken");
        assertNotNull(transcriptData);
        assertNotNull(transcriptData.getStudent());
        assertNotNull(transcriptData.getTranscript());

        ReportData certificateData = reportService.prepareCertificateData(graduationDataStatus, programCertificateTranscript, student, "accessToken");
        assertNotNull(certificateData);
        assertNotNull(certificateData.getStudent());
        assertNotNull(certificateData.getCertificate());

    }

    protected GraduationData createGraduationData(String jsonPath) throws Exception {
        File file = new File(Objects.requireNonNull(ReportServiceTest.class.getClassLoader().getResource(jsonPath)).getFile());
        return new ObjectMapper().readValue(file, GraduationData.class);

    }

    protected String readFile(String jsonPath) throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(jsonPath);
        return readInputStream(inputStream);
    }

    private String readInputStream(InputStream is) throws Exception {
        StringBuffer sb = new StringBuffer();
        InputStreamReader streamReader = new InputStreamReader(is, StandardCharsets.UTF_8);
        BufferedReader reader = new BufferedReader(streamReader);
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

}
