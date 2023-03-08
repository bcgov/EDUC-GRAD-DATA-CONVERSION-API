package ca.bc.gov.educ.api.dataconversion.process;

import ca.bc.gov.educ.api.dataconversion.model.ConvGradStudent;
import ca.bc.gov.educ.api.dataconversion.model.StudentCareerProgram;
import ca.bc.gov.educ.api.dataconversion.model.tsw.GradProgram;
import ca.bc.gov.educ.api.dataconversion.model.tsw.GradRequirement;
import ca.bc.gov.educ.api.dataconversion.model.tsw.GraduationData;
import ca.bc.gov.educ.api.dataconversion.model.tsw.School;
import ca.bc.gov.educ.api.dataconversion.model.tsw.*;
import ca.bc.gov.educ.api.dataconversion.model.tsw.report.*;
import ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiConstants;
import ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiUtils;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ReportProcess {

	private static final String DOCUMENT_STATUS_COMPLETED = "COMPL";

	@Autowired
	RestUtils restUtils;

	@Autowired
	EducGradDataConversionApiConstants constants;

	public ReportData prepareTranscriptData(GraduationData graduationDataStatus, GradAlgorithmGraduationStudentRecord gradResponse, ConvGradStudent convStudent, String accessToken) {
		ReportData data = new ReportData();
		data.setSchool(getSchoolData(graduationDataStatus.getSchool()));
		data.setStudent(getStudentData(graduationDataStatus.getGradStudent()));
		data.setGradMessage(graduationDataStatus.getGradMessage());
		data.setGradProgram(getGradProgram(graduationDataStatus, accessToken));
		data.setGraduationData(getGraduationData(graduationDataStatus));
		data.setLogo(StringUtils.startsWith(data.getSchool().getMincode(), "098") ? "YU" : "BC");
		data.setTranscript(getTranscriptData(graduationDataStatus, gradResponse, convStudent, accessToken));
		data.setNonGradReasons(getNonGradReasons(graduationDataStatus.getNonGradReasons()));
		return data;
	}

	private List<NonGradReason> getNonGradReasons(List<GradRequirement> nonGradReasons) {
		List<NonGradReason> nList = new ArrayList<>();
		if (nonGradReasons != null) {
			for (GradRequirement gR : nonGradReasons) {
				NonGradReason obj = new NonGradReason();
				obj.setCode(gR.getRule());
				obj.setDescription(gR.getDescription());
				nList.add(obj);
			}
		}
		return nList;
	}

	private Transcript getTranscriptData(GraduationData graduationDataStatus, GradAlgorithmGraduationStudentRecord gradResponse, ConvGradStudent convStudent, String accessToken) {
		Transcript transcriptData = new Transcript();
		transcriptData.setInterim("false");
		ProgramCertificateTranscript pcObj = restUtils.getTranscript(gradResponse.getProgram(),
				convStudent.getTranscriptSchoolCategoryCode(), accessToken);
		if (pcObj != null) {
			Code code = new Code();
			code.setCode(pcObj.getTranscriptTypeCode());
			transcriptData.setTranscriptTypeCode(code);
		}
		transcriptData.setIssueDate(graduationDataStatus.getGradStatus().getLastUpdateDate());
		transcriptData.setResults(getTranscriptResults(graduationDataStatus, accessToken));
		return transcriptData;
	}

	private void createCourseListForTranscript(List<StudentCourse> studentCourseList, GraduationData graduationDataStatus, List<TranscriptResult> tList, String provincially) {
		for (StudentCourse sc : studentCourseList) {
			TranscriptResult result = new TranscriptResult();
			String equivOrChallenge = "";
			if (sc.getEquivOrChallenge() != null) {
				equivOrChallenge = sc.getEquivOrChallenge();
			}
			result.setCourse(setCourseObjForTranscript(sc, graduationDataStatus));
			result.setMark(setMarkObjForTranscript(sc, graduationDataStatus.getGradStatus().getProgram(), provincially));
			result.setRequirement(sc.getGradReqMet());
			result.setRequirementName(sc.getGradReqMetDetail());
			result.setUsedForGrad(sc.getCreditsUsedForGrad() != null ? sc.getCreditsUsedForGrad().toString() : "");
			result.setEquivalency(sc.getSpecialCase() != null? sc.getSpecialCase() : equivOrChallenge);
			tList.add(result);
		}
	}

	private TranscriptCourse setCourseObjForTranscript(StudentCourse sc, GraduationData graduationDataStatus) {
		TranscriptCourse crse = new TranscriptCourse();
		crse.setCode(sc.getCourseCode());
		crse.setCredits(getCredits(graduationDataStatus.getGradStatus().getProgram(), sc.getCourseCode(), sc.getCredits(), sc.isRestricted()));
		crse.setLevel(sc.getCourseLevel());
		crse.setName(sc.getCourseName());

		crse.setRelatedCourse(sc.getRelatedCourse());
		crse.setRelatedLevel(sc.getRelatedLevel());
		crse.setType(sc.getProvExamCourse().equals("Y") ? "1" : "2");
		crse.setSessionDate(sc.getSessionDate() != null ? sc.getSessionDate().replace("/", "") : "");
		return crse;
	}

	private Mark setMarkObjForTranscript(StudentCourse sc, String program, String provincially) {
		Mark mrk = new Mark();
		mrk.setExamPercent(getExamPercent(sc.getBestExamPercent(), program, sc.getCourseLevel(), sc.getSpecialCase(), sc.getSessionDate(), sc.getExamPercent()));
		mrk.setFinalLetterGrade(sc.getCompletedCourseLetterGrade());
		mrk.setFinalPercent(getFinalPercent(getValue(sc.getCompletedCoursePercentage()), sc.getSessionDate(), provincially));
		mrk.setInterimLetterGrade(sc.getInterimLetterGrade());
		mrk.setInterimPercent(getValue(sc.getInterimPercent()));
		mrk.setSchoolPercent(getSchoolPercent(sc.getBestSchoolPercent(), program, sc.getCourseLevel(), sc.getSessionDate(), sc.getSchoolPercent()));
		return mrk;
	}

	private String getExamPercent(Double bestExamPercent, String program, String courseLevel, String specialCase, String sDate, Double examPercent) {
		String res = checkCutOffCourseDate(sDate, examPercent);
		if (res == null) {
			String bExam = getValue(bestExamPercent);
			if (specialCase != null && specialCase.compareTo("A") == 0) {
				return "AEG";
			} else if ((program.contains("2004") || program.contains("2018")) && !courseLevel.contains("12")) {
				return "";
			} else {
				return bExam;
			}
		}
		return res;
	}

	private String checkCutOffCourseDate(String sDate, Double value) {
		String cutoffDate = "1991-11-01";
		String sessionDate = sDate + "/01";
		Date temp = EducGradDataConversionApiUtils.parseDate(sessionDate, EducGradDataConversionApiConstants.SECONDARY_DATE_FORMAT);
		sessionDate = EducGradDataConversionApiUtils.formatDate(temp, EducGradDataConversionApiConstants.DEFAULT_DATE_FORMAT);

		int diff = EducGradDataConversionApiUtils.getDifferenceInMonths(sessionDate, cutoffDate);

		if (diff > 0) {
			return getValue(value);
		} else {
			return null;
		}
	}

	private String getSchoolPercent(Double bestSchoolPercent, String program, String courseLevel, String sDate, Double schoolPercent) {

		String res = checkCutOffCourseDate(sDate, schoolPercent);
		if (res == null) {
			String sExam = getValue(bestSchoolPercent);
			if ((program.contains("2004") || program.contains("2018")) && !courseLevel.contains("12")) {
				return "";
			} else {
				return sExam;
			}
		}
		return res;
	}

	private void createAssessmentListForTranscript(List<StudentAssessment> studentAssessmentList, List<TranscriptResult> tList, String accessToken) {
		for (StudentAssessment sc : studentAssessmentList) {
			TranscriptResult result = new TranscriptResult();
			TranscriptCourse crse = new TranscriptCourse();
			crse.setCode(sc.getAssessmentCode());
			crse.setLevel("");
			crse.setCredits("NA");
			crse.setName(sc.getAssessmentName());
			crse.setType("3");
			crse.setSessionDate(sc.getSessionDate() != null ? sc.getSessionDate().replace("/", "") : "");
			result.setCourse(crse);

			Mark mrk = new Mark();

			mrk.setExamPercent("");
			mrk.setFinalLetterGrade("");
			mrk.setInterimLetterGrade("");
			mrk.setInterimPercent("");
			mrk.setSchoolPercent("");
			mrk.setFinalLetterGrade("NA");
			mrk.setFinalPercent(getAssessmentFinalPercentTranscript(sc, accessToken));
			result.setMark(mrk);
			result.setRequirement(sc.getGradReqMet());
			result.setRequirementName(sc.getGradReqMetDetail());
			if (!tList.contains(result)) {
				tList.add(result);
			}
		}
	}

	private List<TranscriptResult> getTranscriptResults(GraduationData graduationDataStatus, String accessToken) {
		List<TranscriptResult> tList = new ArrayList<>();
		String program = graduationDataStatus.getGradStatus().getProgram();
		List<StudentCourse> studentCourseList = graduationDataStatus.getStudentCourses().getStudentCourseList();
		if (!studentCourseList.isEmpty()) {
			if (program.contains("1950") || program.contains("1986")) {
				List<StudentCourse> provinciallyExaminable = studentCourseList.stream().filter(sc -> sc.getProvExamCourse().compareTo("Y") == 0).collect(Collectors.toList());
				if (!provinciallyExaminable.isEmpty()) {
					sortOnCourseCode(provinciallyExaminable);
					createCourseListForTranscript(provinciallyExaminable, graduationDataStatus, tList, "provincially");
				}

				List<StudentCourse> nonExaminable = studentCourseList.stream().filter(sc -> sc.getProvExamCourse().compareTo("N") == 0).collect(Collectors.toList());
				if (!nonExaminable.isEmpty()) {
					sortOnCourseCode(nonExaminable);
					createCourseListForTranscript(nonExaminable, graduationDataStatus, tList, "non-examinable");
				}
			} else {
				studentCourseList.sort(Comparator.comparing(StudentCourse::getCourseLevel, Comparator.nullsLast(String::compareTo)).thenComparing(StudentCourse::getCourseName));
				createCourseListForTranscript(studentCourseList, graduationDataStatus, tList, "regular");
			}
		}
		List<StudentAssessment> studentAssessmentList = graduationDataStatus.getStudentAssessments().getStudentAssessmentList();
		if (studentAssessmentList == null) {
			studentAssessmentList = new ArrayList<>();
		}
		if (!studentAssessmentList.isEmpty()) {
			studentAssessmentList.sort(Comparator.comparing(StudentAssessment::getAssessmentCode));
		}

		createAssessmentListForTranscript(studentAssessmentList, tList, accessToken);
		return tList;
	}

	private void sortOnCourseCode(List<StudentCourse> cList) {
		cList.sort(Comparator.comparing(StudentCourse::getCourseCode));
	}

	private String getCredits(String program, String courseCode, Integer totalCredits, boolean isRestricted) {
		if (((program.contains("2004") || program.contains("2018")) && (courseCode.startsWith("X") || courseCode.startsWith("CP"))) || isRestricted) {
			return String.format("(%s)", totalCredits);
		}
		return String.valueOf(totalCredits);
	}

	private String getFinalPercent(String finalCompletedPercentage, String sDate, String provincially) {
		String cutoffDate = "1994-09-01";
		String sessionDate = sDate + "/01";
		if (provincially.equalsIgnoreCase("provincially")) {
			return finalCompletedPercentage;
		}
		Date temp = EducGradDataConversionApiUtils.parseDate(sessionDate, EducGradDataConversionApiConstants.SECONDARY_DATE_FORMAT);
		sessionDate = EducGradDataConversionApiUtils.formatDate(temp, EducGradDataConversionApiConstants.DEFAULT_DATE_FORMAT);

		int diff = EducGradDataConversionApiUtils.getDifferenceInMonths(sessionDate, cutoffDate);

		if (diff >= 0) {
			return "---";
		} else {
			return finalCompletedPercentage;
		}
	}

	private String getAssessmentFinalPercentTranscript(StudentAssessment sA, String accessToken) {
		String finalPercent = getValue(sA.getProficiencyScore());
		if ((sA.getAssessmentCode().equalsIgnoreCase("LTE10") || sA.getAssessmentCode().equalsIgnoreCase("LTP10")) && (sA.getSpecialCase() == null || StringUtils.isBlank(sA.getSpecialCase().trim())) && StringUtils.isNotBlank(finalPercent)) {
			finalPercent = "RM";
		}
		if (sA.getSpecialCase() != null && StringUtils.isNotBlank(sA.getSpecialCase().trim()) && !sA.getSpecialCase().equalsIgnoreCase("X") && !sA.getSpecialCase().equalsIgnoreCase("Q")) {
			SpecialCase spC = this.restUtils.getSpecialCase(sA.getSpecialCase(), accessToken);
			finalPercent = spC != null ? spC.getLabel() : "";
		}
		return finalPercent;
	}

	private String getValue(Double value) {
		return value != null && value != 0.0 ? new DecimalFormat("#").format(value) : "";
	}

	private ca.bc.gov.educ.api.dataconversion.model.tsw.report.GraduationData getGraduationData(
			GraduationData graduationDataStatus) {
		ca.bc.gov.educ.api.dataconversion.model.tsw.report.GraduationData data = new ca.bc.gov.educ.api.dataconversion.model.tsw.report.GraduationData();
		data.setDogwoodFlag(graduationDataStatus.isDualDogwood());
		if (graduationDataStatus.isGraduated()) {
			if (!graduationDataStatus.getGradStatus().getProgram().equalsIgnoreCase("SCCP")) {
				if (graduationDataStatus.getGradStatus().getProgramCompletionDate() != null) {
					if (graduationDataStatus.getGradStatus().getProgramCompletionDate().length() > 7) {
						data.setGraduationDate(EducGradDataConversionApiUtils.formatIssueDateForReportJasper(graduationDataStatus.getGradStatus().getProgramCompletionDate()));
					} else {
						data.setGraduationDate(EducGradDataConversionApiUtils.formatIssueDateForReportJasper(EducGradDataConversionApiUtils.parsingNFormating(graduationDataStatus.getGradStatus().getProgramCompletionDate())));
					}
				}
				data.setHonorsFlag(StringUtils.equals(graduationDataStatus.getGradStatus().getHonoursStanding(), "Y"));
			} else {
				data.setGraduationDate(EducGradDataConversionApiUtils.formatIssueDateForReportJasper(EducGradDataConversionApiUtils.parsingNFormating(graduationDataStatus.getGradStatus().getProgramCompletionDate())));
			}
		}

		if (graduationDataStatus.getOptionalGradStatus() != null) {
			for (GradAlgorithmOptionalStudentProgram op : graduationDataStatus.getOptionalGradStatus()) {
				switch (op.getOptionalProgramCode()) {
					case "FR":
					case "DD":
					case "FI":
						break;
					case "CP":
						setGraduationDataSpecialPrograms(data, op.getCpList());
						break;
					default: // "AD", "BC", "BD"
						data.getProgramCodes().add(op.getOptionalProgramCode());
						break;
				}
			}
		}

		return data;
	}

	private void setGraduationDataSpecialPrograms(ca.bc.gov.educ.api.dataconversion.model.tsw.report.GraduationData data, List<StudentCareerProgram> studentCareerPrograms) {
		if (studentCareerPrograms != null) {
			for (StudentCareerProgram cp : studentCareerPrograms) {
				data.getProgramCodes().add(cp.getCareerProgramCode());
			}
		}
	}

	private ca.bc.gov.educ.api.dataconversion.model.tsw.report.GradProgram getGradProgram(GraduationData graduationDataStatus, String accessToken) {
		ca.bc.gov.educ.api.dataconversion.model.tsw.report.GradProgram gPgm = new ca.bc.gov.educ.api.dataconversion.model.tsw.report.GradProgram();
		Code code = new Code();
		if (graduationDataStatus.getGradStatus().getProgram() != null) {
			GradProgram gradProgram = this.restUtils.getGradProgram(graduationDataStatus.getGradStatus().getProgram(), accessToken);
			if (gradProgram != null) {
				code.setDescription(gradProgram.getProgramName());
				code.setName(gradProgram.getProgramName());
			}
		}
		code.setCode(graduationDataStatus.getGradStatus().getProgram());
		gPgm.setCode(code);
		return gPgm;
	}

	private Student getStudentData(GradSearchStudent gradStudent) {
		Student std = new Student();
		std.setBirthdate(EducGradDataConversionApiUtils.parseDate(gradStudent.getDob()));
		std.setGrade(gradStudent.getStudentGrade());
		std.setStudStatus(gradStudent.getStudentStatus());
		std.setFirstName(gradStudent.getLegalFirstName());
		std.setMiddleName(gradStudent.getLegalMiddleNames());
		std.setLastName(gradStudent.getLegalLastName());
		std.setGender(gradStudent.getGenderCode());
		std.setCitizenship(gradStudent.getStudentCitizenship());
		Pen pen = new Pen();
		pen.setPen(gradStudent.getPen());
		pen.setEntityID(gradStudent.getStudentID());
		std.setPen(pen);
		return std;
	}


	private ca.bc.gov.educ.api.dataconversion.model.tsw.report.School getSchoolData(School school) {
		ca.bc.gov.educ.api.dataconversion.model.tsw.report.School schObj = new ca.bc.gov.educ.api.dataconversion.model.tsw.report.School();
		Address addRess = new Address();
		addRess.setCity(school.getCity());
		addRess.setCode(school.getPostal());
		addRess.setCountry(school.getCountryCode());
		addRess.setRegion(school.getProvCode());
		addRess.setStreetLine1(school.getAddress1());
		addRess.setStreetLine2(school.getAddress2());
		schObj.setTypeIndicator(school.getIndependentDesignation());
		schObj.setAddress(addRess);
		schObj.setMincode(school.getMinCode());
		schObj.setName(school.getSchoolName());
		schObj.setSignatureCode(school.getMinCode().substring(0, 3));
		schObj.setDistno(school.getMinCode().substring(0, 3));
		schObj.setSchlno(school.getMinCode());
		schObj.setStudents(new ArrayList<>());
		return schObj;
	}

	public void saveStudentTranscriptReportJasper(ReportData sample, Date distributionDate, String accessToken, UUID studentID, boolean isGraduated) {

		String encodedPdfReportTranscript = generateStudentTranscriptReportJasper(sample, accessToken);
		GradStudentTranscripts requestObj = new GradStudentTranscripts();
		requestObj.setTranscript(encodedPdfReportTranscript);
		requestObj.setStudentID(studentID);
		requestObj.setTranscriptTypeCode(sample.getTranscript().getTranscriptTypeCode().getCode());
		requestObj.setDocumentStatusCode("IP");
		requestObj.setDistributionDate(distributionDate);
		if (isGraduated)
			requestObj.setDocumentStatusCode(DOCUMENT_STATUS_COMPLETED);

		this.restUtils.saveGradStudentTranscript(requestObj, isGraduated, accessToken);
	}

	private String generateStudentTranscriptReportJasper(ReportData sample,String accessToken) {
		ReportOptions options = new ReportOptions();
		options.setReportFile("transcript");
		options.setReportName("Transcript Report.pdf");
		ReportRequest reportParams = new ReportRequest();
		reportParams.setOptions(options);
		reportParams.setData(sample);
		byte[] bytesSAR = this.restUtils.getTranscriptReport(reportParams, accessToken);
		byte[] encoded = org.apache.commons.codec.binary.Base64.encodeBase64(bytesSAR);
		return new String(encoded, StandardCharsets.US_ASCII);
	}

	public List<ProgramCertificateTranscript> getCertificateList(GraduationData graduationDataStatus, String schoolCategoryCode, String accessToken) {
		ProgramCertificateReq req = new ProgramCertificateReq();
		req.setProgramCode(graduationDataStatus.getGradStatus().getProgram());
		for (GradAlgorithmOptionalStudentProgram optionalPrograms : graduationDataStatus.getOptionalGradStatus()) {
			if (optionalPrograms.getOptionalProgramCode().equals("FI") || optionalPrograms.getOptionalProgramCode().equals("DD") || optionalPrograms.getOptionalProgramCode().equals("FR")) {
				req.setOptionalProgram(optionalPrograms.getOptionalProgramCode());
				break;
			}
		}
		req.setSchoolCategoryCode(schoolCategoryCode);
		return this.restUtils.getProgramCertificateTranscriptList(req, accessToken);
	}

	public ReportData prepareCertificateData(GraduationData graduationDataStatus, ProgramCertificateTranscript certType, ConvGradStudent convStudent, String accessToken) {
		ReportData data = new ReportData();
		if (convStudent.getCertificateSchool() != null) {
			data.setSchool(getSchoolData(convStudent.getCertificateSchool()));
		} else {
			data.setSchool(getSchoolData(convStudent.getTranscriptSchool()));
		}
		data.setStudent(getStudentData(graduationDataStatus.getGradStudent()));
		data.setGradProgram(getGradProgram(graduationDataStatus,accessToken));
		data.setGraduationData(getGraduationData(graduationDataStatus));
		data.setUpdateDate(EducGradDataConversionApiUtils.formatDate(new Date()));
		data.setCertificate(getCertificateData(graduationDataStatus.getGradStatus(),certType));
		if(certType.getCertificateTypeCode().equalsIgnoreCase("E") || certType.getCertificateTypeCode().equalsIgnoreCase("A") || certType.getCertificateTypeCode().equalsIgnoreCase("EI") || certType.getCertificateTypeCode().equalsIgnoreCase("AI")) {
			data.getStudent().setEnglishCert(certType.getCertificateTypeCode());
		} else if(certType.getCertificateTypeCode().equalsIgnoreCase("F") || certType.getCertificateTypeCode().equalsIgnoreCase("S") || certType.getCertificateTypeCode().equalsIgnoreCase("SCF")) {
			data.getStudent().setFrenchCert(certType.getCertificateTypeCode());
		}
		return data;
	}

	public void saveStudentCertificateReportJasper(GraduationData graduationDataStatus, ConvGradStudent convStudent,
												   String accessToken, ProgramCertificateTranscript certType) {
		ReportData certData = prepareCertificateData(graduationDataStatus, certType, convStudent, accessToken);
		String encodedPdfReportCertificate = generateStudentCertificateReportJasper(certData,accessToken);
		GradStudentCertificates requestObj = new GradStudentCertificates();
		requestObj.setPen(graduationDataStatus.getGradStatus().getPen());
		requestObj.setStudentID(graduationDataStatus.getGradStatus().getStudentID());
		requestObj.setCertificate(encodedPdfReportCertificate);
		requestObj.setGradCertificateTypeCode(certType.getCertificateTypeCode());
		requestObj.setDocumentStatusCode(DOCUMENT_STATUS_COMPLETED);
		requestObj.setDistributionDate(convStudent.getDistributionDate());
		this.restUtils.saveGradStudentCertificate(requestObj, accessToken);
	}

	private Certificate getCertificateData(GradAlgorithmGraduationStudentRecord gradResponse,ProgramCertificateTranscript certData) {
		Certificate cert = new Certificate();
		Date issuedDate = null;
		if (gradResponse.getProgramCompletionDate() != null) {
			if (gradResponse.getProgramCompletionDate().length() > 7) {
				issuedDate = EducGradDataConversionApiUtils.formatIssueDateForReportJasper(gradResponse.getProgramCompletionDate());
			} else {
				issuedDate = EducGradDataConversionApiUtils.formatIssueDateForReportJasper(EducGradDataConversionApiUtils.parsingNFormating(gradResponse.getProgramCompletionDate()));
			}
		}
		cert.setIssued(issuedDate);

		OrderType orTy = new OrderType();
		orTy.setName("Certificate");
		CertificateType certType = new CertificateType();
		PaperType pType = new PaperType();
		pType.setCode(certData.getCertificatePaperType());
		certType.setPaperType(pType);
		certType.setReportName(certData.getCertificateTypeCode());
		orTy.setCertificateType(certType);
		cert.setOrderType(orTy);
		cert.setCertStyle("Original");
		return cert;
	}

	private String generateStudentCertificateReportJasper(ReportData sample,
														  String accessToken) {
		ReportOptions options = new ReportOptions();
		options.setReportFile("certificate");
		options.setReportName("Certificate.pdf");
		ReportRequest reportParams = new ReportRequest();
		reportParams.setOptions(options);
		reportParams.setData(sample);
		byte[] bytesSAR = this.restUtils.getCertificateReport(reportParams, accessToken);
		byte[] encoded = Base64.encodeBase64(bytesSAR);
		return new String(encoded,StandardCharsets.US_ASCII);
	}
}
