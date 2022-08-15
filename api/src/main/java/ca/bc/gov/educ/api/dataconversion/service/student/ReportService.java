package ca.bc.gov.educ.api.dataconversion.service.student;

import ca.bc.gov.educ.api.dataconversion.model.tsw.GradProgram;
import ca.bc.gov.educ.api.dataconversion.model.tsw.GradRequirement;
import ca.bc.gov.educ.api.dataconversion.model.tsw.GraduationData;
import ca.bc.gov.educ.api.dataconversion.model.tsw.School;
import ca.bc.gov.educ.api.dataconversion.model.tsw.*;
import ca.bc.gov.educ.api.dataconversion.model.tsw.report.*;
import ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiConstants;
import ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiUtils;
import ca.bc.gov.educ.api.dataconversion.util.ThreadLocalStateUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

	private static final String GRAD_REPORT_API_DOWN = "GRAD-REPORT-API IS DOWN";
	private static final String GRAD_GRADUATION_REPORT_API_DOWN = "GRAD-GRADUATION-REPORT-API IS DOWN";
	private static final String DOCUMENT_STATUS_COMPLETED = "COMPL";

	@Autowired
	WebClient webClient;

	@Autowired
	EducGradDataConversionApiConstants constants;

	public ProgramCertificateTranscript getTranscript(GradAlgorithmGraduationStudentRecord gradResponse, GraduationData graduationDataStatus, String accessToken) {
		ProgramCertificateReq req = new ProgramCertificateReq();
		req.setProgramCode(gradResponse.getProgram());
		req.setSchoolCategoryCode(getSchoolCategoryCode(accessToken, graduationDataStatus.getGradStatus().getSchoolOfRecord()));
		return webClient.post().uri(constants.getTranscript())
				.headers(h -> {
					h.setBearerAuth(accessToken);
					h.set(EducGradDataConversionApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
				}).body(BodyInserters.fromValue(req)).retrieve().bodyToMono(ProgramCertificateTranscript.class).block();
	}

	public String getSchoolCategoryCode(String accessToken, String mincode) {
		CommonSchool commonSchoolObj = webClient.get().uri(String.format(constants.getSchoolCategoryCode(), mincode))
				.headers(h -> {
					h.setBearerAuth(accessToken);
					h.set(constants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
				}).retrieve().bodyToMono(CommonSchool.class).block();
		if (commonSchoolObj != null) {
			return commonSchoolObj.getSchoolCategoryCode();
		}
		return null;
	}

	public ReportData prepareTranscriptData(GraduationData graduationDataStatus, GradAlgorithmGraduationStudentRecord gradResponse, String accessToken) {
		ReportData data = new ReportData();
		data.setSchool(getSchoolData(graduationDataStatus.getSchool()));
		data.setStudent(getStudentData(graduationDataStatus.getGradStudent()));
		data.setGradMessage(graduationDataStatus.getGradMessage());
		data.setGradProgram(getGradProgram(graduationDataStatus, accessToken));
		data.setGraduationData(getGraduationData(graduationDataStatus));
		data.setLogo(StringUtils.startsWith(data.getSchool().getMincode(), "098") ? "YU" : "BC");
		data.setTranscript(getTranscriptData(graduationDataStatus, gradResponse,accessToken));
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

	private Transcript getTranscriptData(GraduationData graduationDataStatus, GradAlgorithmGraduationStudentRecord gradResponse, String accessToken) {
		Transcript transcriptData = new Transcript();
		transcriptData.setInterim("false");
		ProgramCertificateTranscript pcObj = getTranscript(gradResponse, graduationDataStatus, accessToken);
		if (pcObj != null) {
			Code code = new Code();
			code.setCode(pcObj.getTranscriptTypeCode());
			transcriptData.setTranscriptTypeCode(code);
		}
		transcriptData.setIssueDate(EducGradDataConversionApiUtils.formatIssueDateForReportJasper(new Date().toString()));
		transcriptData.setResults(getTranscriptResults(graduationDataStatus, accessToken));
		return transcriptData;
	}

	private void createCourseListForTranscript(List<StudentCourse> studentCourseList, GraduationData graduationDataStatus, List<TranscriptResult> tList, String provincially) {
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("PST"), Locale.CANADA);
		String today = EducGradDataConversionApiUtils.formatDate(cal.getTime(), constants.DEFAULT_DATE_FORMAT);
		for (StudentCourse sc : studentCourseList) {
			TranscriptResult result = new TranscriptResult();
			String equivOrChallenge = "";
			if (sc.getEquivOrChallenge() != null) {
				equivOrChallenge = sc.getEquivOrChallenge();
			}
			result.setCourse(setCourseObjForTranscript(sc, graduationDataStatus));
			result.setMark(setMarkObjForTranscript(sc, graduationDataStatus.getGradStatus().getProgram(), provincially));
			result.setRequirement(sc.getGradReqMet());
			result.setUsedForGrad(sc.getCreditsUsedForGrad() != null ? sc.getCreditsUsedForGrad().toString() : "");
			result.setRequirementName(sc.getGradReqMetDetail());
			result.setEquivalency(sc.getSpecialCase() != null && sc.getSpecialCase().compareTo("C") == 0 ? "C" : equivOrChallenge);
			tList.add(result);

		}
	}

	private TranscriptCourse setCourseObjForTranscript(StudentCourse sc, GraduationData graduationDataStatus) {
		TranscriptCourse crse = new TranscriptCourse();
		crse.setCode(sc.getCourseCode());
		crse.setCredits(getCredits(graduationDataStatus.getGradStatus().getProgram(), sc.getCourseCode(), sc.getCredits(), sc.isRestricted()));
		crse.setLevel(sc.getCourseLevel());
		crse.setName(getCourseNameLogic(sc));

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
		Date temp = EducGradDataConversionApiUtils.parseDate(sessionDate, constants.SECONDARY_DATE_FORMAT);
		sessionDate = EducGradDataConversionApiUtils.formatDate(temp, constants.DEFAULT_DATE_FORMAT);

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

	private void createAssessmentListForTranscript(List<StudentAssessment> studentAssessmentList, GraduationData graduationDataStatus, List<TranscriptResult> tList, String accessToken) {
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("PST"), Locale.CANADA);
		String today = EducGradDataConversionApiUtils.formatDate(cal.getTime(), constants.DEFAULT_DATE_FORMAT);
		for (StudentAssessment sc : studentAssessmentList) {
			String finalPercent = getValue(sc.getProficiencyScore());
			String cutoffDate = EducGradDataConversionApiUtils.formatDate(graduationDataStatus.getGradProgram().getAssessmentReleaseDate(), constants.DEFAULT_DATE_FORMAT);
			if (sc.getSessionDate() != null) {
				String sessionDate = sc.getSessionDate() + "/01";
				Date temp = EducGradDataConversionApiUtils.parseDate(sessionDate, constants.SECONDARY_DATE_FORMAT);
				sessionDate = EducGradDataConversionApiUtils.formatDate(temp, constants.DEFAULT_DATE_FORMAT);

				int diff = EducGradDataConversionApiUtils.getDifferenceInMonths(sessionDate, cutoffDate);

				if (diff < 0 && !finalPercent.equals("") && !finalPercent.equals("0")) {
					continue;
				}
			}
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
			tList.add(result);
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

		createAssessmentListForTranscript(studentAssessmentList, graduationDataStatus, tList, accessToken);
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
		Date temp = EducGradDataConversionApiUtils.parseDate(sessionDate, constants.SECONDARY_DATE_FORMAT);
		sessionDate = EducGradDataConversionApiUtils.formatDate(temp, constants.DEFAULT_DATE_FORMAT);

		int diff = EducGradDataConversionApiUtils.getDifferenceInMonths(sessionDate, cutoffDate);

		if (diff >= 0) {
			return "---";
		} else {
			return finalCompletedPercentage;
		}
	}

	private String getAssessmentFinalPercentAchievement(StudentAssessment sA, Date assessmentReleaseDate, String accessToken) {
		String finalPercent = getValue(sA.getProficiencyScore());
		if (sA.getSessionDate() != null) {
			String cutoffDate = EducGradDataConversionApiUtils.formatDate(assessmentReleaseDate, constants.DEFAULT_DATE_FORMAT);
			String sessionDate = sA.getSessionDate() + "/01";
			Date temp = EducGradDataConversionApiUtils.parseDate(sessionDate, constants.SECONDARY_DATE_FORMAT);
			sessionDate = EducGradDataConversionApiUtils.formatDate(temp, constants.DEFAULT_DATE_FORMAT);

			int diff = EducGradDataConversionApiUtils.getDifferenceInMonths(sessionDate, cutoffDate);

			if (diff < 0 && !finalPercent.equals("") && !finalPercent.equals("0")) {
				return "";
			}
		}
		if (sA.getSpecialCase() != null && StringUtils.isNotBlank(sA.getSpecialCase().trim())) {
			SpecialCase spC = webClient.get().uri(String.format(constants.getSpecialCase(), sA.getSpecialCase()))
					.headers(h -> {
						h.setBearerAuth(accessToken);
						h.set(constants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
					}).retrieve().bodyToMono(SpecialCase.class).block();
			finalPercent = spC != null ? spC.getLabel() : "";
		}

		if (sA.getExceededWriteFlag() != null && StringUtils.isNotBlank(sA.getExceededWriteFlag().trim()) && sA.getExceededWriteFlag().compareTo("Y") == 0) {
			finalPercent = "INV";
		}
		return finalPercent;
	}

	private String getAssessmentFinalPercentTranscript(StudentAssessment sA, String accessToken) {
		String finalPercent = getValue(sA.getProficiencyScore());
		if ((sA.getAssessmentCode().equalsIgnoreCase("LTE10") || sA.getAssessmentCode().equalsIgnoreCase("LTP10")) && (sA.getSpecialCase() == null || StringUtils.isBlank(sA.getSpecialCase().trim())) && StringUtils.isNotBlank(finalPercent)) {
			finalPercent = "RM";
		}
		if (sA.getSpecialCase() != null && StringUtils.isNotBlank(sA.getSpecialCase().trim()) && !sA.getSpecialCase().equalsIgnoreCase("X") && !sA.getSpecialCase().equalsIgnoreCase("Q")) {
			SpecialCase spC = webClient.get().uri(String.format(constants.getSpecialCase(), sA.getSpecialCase()))
					.headers(h -> {
						h.setBearerAuth(accessToken);
						h.set(constants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
					}).retrieve().bodyToMono(SpecialCase.class).block();
			finalPercent = spC != null ? spC.getLabel() : "";
		}
		return finalPercent;
	}

	private String getCourseNameLogic(StudentCourse sc) {
		if (sc.getGenericCourseType() != null && sc.getGenericCourseType().equalsIgnoreCase("I") && StringUtils.isNotBlank(sc.getRelatedCourse()) && StringUtils.isNotBlank(sc.getRelatedLevel()) && StringUtils.isNotBlank(sc.getRelatedCourseName())) {
			return "IDS " + sc.getRelatedCourseName();
		}
		if (StringUtils.isNotBlank(sc.getCustomizedCourseName())) {
			return sc.getCustomizedCourseName();
		}
		return sc.getCourseName();
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
				data.setHonorsFlag(graduationDataStatus.getGradStatus().getHonoursStanding().equals("Y"));
			} else {
				data.setGraduationDate(EducGradDataConversionApiUtils.formatIssueDateForReportJasper(EducGradDataConversionApiUtils.parsingNFormating(graduationDataStatus.getGradStatus().getProgramCompletionDate())));
			}
		}

		return data;
	}

	private ca.bc.gov.educ.api.dataconversion.model.tsw.report.GradProgram getGradProgram(GraduationData graduationDataStatus, String accessToken) {
		ca.bc.gov.educ.api.dataconversion.model.tsw.report.GradProgram gPgm = new ca.bc.gov.educ.api.dataconversion.model.tsw.report.GradProgram();
		Code code = new Code();
		if (graduationDataStatus.getGradStatus().getProgram() != null) {
			GradProgram gradProgram = webClient.get().uri(String.format(constants.getProgramNameEndpoint(), graduationDataStatus.getGradStatus().getProgram()))
					.headers(h -> {
						h.setBearerAuth(accessToken);
						h.set(constants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
					}).retrieve().bodyToMono(GradProgram.class).block();
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
		std.setGender(gradStudent.getGenderCode());
		std.setLastName(gradStudent.getLegalLastName());
		Pen pen = new Pen();
		pen.setPen(gradStudent.getPen());
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

	private ca.bc.gov.educ.api.dataconversion.model.tsw.report.School getSchoolDataAchvReport(School school) {
		ca.bc.gov.educ.api.dataconversion.model.tsw.report.School schObj = new ca.bc.gov.educ.api.dataconversion.model.tsw.report.School();
		schObj.setMincode(school.getMinCode());
		schObj.setName(school.getSchoolName());
		schObj.setStudents(new ArrayList<>());
		return schObj;
	}

	private ca.bc.gov.educ.api.dataconversion.model.tsw.report.GraduationStatus getGraduationStatus(GraduationData graduationData) {
		ca.bc.gov.educ.api.dataconversion.model.tsw.report.GraduationStatus gradStatus = new ca.bc.gov.educ.api.dataconversion.model.tsw.report.GraduationStatus();
		gradStatus.setGraduationMessage(graduationData.getGradMessage());
		return gradStatus;
	}

	private ca.bc.gov.educ.api.dataconversion.model.tsw.report.Student getStudentDataAchvReport(GradSearchStudent studentObj) {
		ca.bc.gov.educ.api.dataconversion.model.tsw.report.Student studObj = new ca.bc.gov.educ.api.dataconversion.model.tsw.report.Student();
		studObj.setGender(StudentGenderEnum.valueOf(studentObj.getGenderCode()).toString());
		studObj.setFirstName(studentObj.getLegalFirstName());
		studObj.setLastName(studentObj.getLegalLastName());
		studObj.setGrade(studentObj.getStudentGrade());
		Pen pen = new Pen();
		pen.setPen(studentObj.getPen());
		studObj.setPen(pen);
		studObj.setLocalId(studentObj.getLocalID());
		studObj.setGradProgram(studentObj.getProgram());
		studObj.setBirthdate(EducGradDataConversionApiUtils.formatIssueDateForReportJasper(studentObj.getDob()));
		studObj.setHasOtherProgram(" ");
		return studObj;
	}

	private void getStudentCoursesAssessmentsNExams(ReportData data, GraduationData graduationDataStatus, String accessToken) {
		List<StudentCourse> sCList = graduationDataStatus.getStudentCourses().getStudentCourseList();
		List<StudentCourse> studentExamList = sCList
				.stream()
				.filter(sc -> "Y".compareTo(sc.getProvExamCourse()) == 0)
				.collect(Collectors.toList());
		List<StudentCourse> studentCourseList = sCList
				.stream()
				.filter(sc -> "N".compareTo(sc.getProvExamCourse()) == 0)
				.collect(Collectors.toList());
		List<StudentAssessment> studentAssessmentList = graduationDataStatus.getStudentAssessments().getStudentAssessmentList();
		List<AchievementCourse> sCourseList = new ArrayList<>();
		List<Exam> sExamList = new ArrayList<>();
		data.setStudentCourses(processStudentCourses(sCourseList, studentCourseList));
		Assessment achv = new Assessment();
		achv.setIssueDate(EducGradDataConversionApiUtils.formatIssueDateForReportJasper(EducGradDataConversionApiUtils.getSimpleDateFormat(new Date())));
		achv.setResults(getAssessmentResults(studentAssessmentList, graduationDataStatus.getGradProgram().getAssessmentReleaseDate(), accessToken));
		data.setAssessment(achv);
		data.setStudentExams(processStudentExams(sExamList, studentExamList));
	}

	private List<Exam> processStudentExams(List<Exam> sExamList, List<StudentCourse> studentExamList) {
		for (StudentCourse sc : studentExamList) {
			Exam crse = new Exam();
			String equivOrChallenge = "";
			if (sc.getEquivOrChallenge() != null) {
				equivOrChallenge = sc.getEquivOrChallenge();
			}
			crse.setCourseCode(sc.getCourseCode());
			crse.setCredits(sc.getCredits().toString());
			crse.setCourseLevel(sc.getCourseLevel());
			crse.setCourseName(getCourseNameLogic(sc));
			crse.setSessionDate(sc.getSessionDate() != null ? sc.getSessionDate() : "");
			crse.setCompletedCourseLetterGrade(sc.getCompletedCourseLetterGrade());
			crse.setCompletedCoursePercentage(getValue(sc.getCompletedCoursePercentage()));
			crse.setGradReqMet(sc.getGradReqMet());
			crse.setProjected(sc.isProjected());
			crse.setCreditsUsedForGrad(sc.getCreditsUsedForGrad() != null ? sc.getCreditsUsedForGrad() : 0);
			crse.setEquivOrChallenge(equivOrChallenge);
			crse.setBestSchoolPercent(getValue(sc.getBestSchoolPercent()));
			crse.setBestExamPercent(getValue(sc.getBestExamPercent()));
			crse.setMetLitNumRequirement(sc.getMetLitNumRequirement() != null ? sc.getMetLitNumRequirement() : "");
			sExamList.add(crse);
		}

		if (!sExamList.isEmpty()) {
			sExamList.sort(Comparator.comparing(Exam::getCourseCode)
					.thenComparing(Exam::getCourseLevel)
					.thenComparing(Exam::getSessionDate));
		}
		return sExamList;
	}

	private List<AchievementCourse> processStudentCourses(List<AchievementCourse> sCourseList, List<StudentCourse> studentCourseList) {
		for (StudentCourse sc : studentCourseList) {
			AchievementCourse crse = new AchievementCourse();
			String equivOrChallenge = "";
			if (sc.getEquivOrChallenge() != null) {
				equivOrChallenge = sc.getEquivOrChallenge();
			}

			crse.setCourseCode(sc.getCourseCode());
			crse.setCredits(sc.getCredits().toString());
			crse.setCourseLevel(sc.getCourseLevel());
			crse.setCourseName(getCourseNameLogic(sc));
			crse.setProjected(sc.isProjected());
			crse.setInterimPercent(getValue(sc.getInterimPercent()));
			crse.setSessionDate(sc.getSessionDate() != null ? sc.getSessionDate() : "");
			crse.setCompletedCourseLetterGrade(sc.getCompletedCourseLetterGrade());
			crse.setCompletedCoursePercentage(getValue(sc.getCompletedCoursePercentage()));
			crse.setGradReqMet(sc.getGradReqMet());
			crse.setUsedForGrad(sc.getCreditsUsedForGrad() != null ? sc.getCreditsUsedForGrad().toString() : "0");
			crse.setEquivOrChallenge(equivOrChallenge);
			sCourseList.add(crse);
		}
		if (!sCourseList.isEmpty()) {
			sCourseList.sort(Comparator.comparing(AchievementCourse::getCourseCode)
					.thenComparing(AchievementCourse::getCourseLevel)
					.thenComparing(AchievementCourse::getSessionDate));
		}
		return sCourseList;
	}

	private List<AssessmentResult> getAssessmentResults(List<StudentAssessment> studentAssessmentList, Date assessmentReleaseDate, String accessToken) {
		List<AssessmentResult> tList = new ArrayList<>();
		for (StudentAssessment sA : studentAssessmentList) {
			AssessmentResult result = new AssessmentResult();
			result.setAssessmentCode(sA.getAssessmentCode());
			result.setAssessmentName(sA.getAssessmentName());
			result.setGradReqMet(sA.getGradReqMet());
			result.setSessionDate(sA.getSessionDate() != null ? sA.getSessionDate() : "");
			result.setProficiencyScore(getAssessmentFinalPercentAchievement(sA, assessmentReleaseDate, accessToken));
			result.setSpecialCase(sA.getSpecialCase());
			result.setExceededWriteFlag(sA.getExceededWriteFlag());
			result.setProjected(sA.isProjected());
			tList.add(result);
		}
		if (!tList.isEmpty()) {
			tList.sort(Comparator.comparing(AssessmentResult::getAssessmentCode)
					.thenComparing(AssessmentResult::getSessionDate));
		}
		return tList;
	}

	public void saveStudentTranscriptReportJasper(ReportData sample, String accessToken, UUID studentID, boolean isGraduated) {

		String encodedPdfReportTranscript = generateStudentTranscriptReportJasper(sample, accessToken);
		GradStudentTranscripts requestObj = new GradStudentTranscripts();
		requestObj.setTranscript(encodedPdfReportTranscript);
		requestObj.setStudentID(studentID);
		requestObj.setTranscriptTypeCode(sample.getTranscript().getTranscriptTypeCode().getCode());
		requestObj.setDocumentStatusCode("IP");
		if (isGraduated)
			requestObj.setDocumentStatusCode(DOCUMENT_STATUS_COMPLETED);

		webClient.post().uri(String.format(constants.getUpdateGradStudentTranscript(), isGraduated))
				.headers(h -> {
					h.setBearerAuth(accessToken);
					h.set(constants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
				}).body(BodyInserters.fromValue(requestObj)).retrieve().bodyToMono(GradStudentTranscripts.class).block();
	}

	private String generateStudentTranscriptReportJasper(ReportData sample,String accessToken) {
		ReportOptions options = new ReportOptions();
		options.setReportFile("transcript");
		options.setReportName("Transcript Report.pdf");
		ReportRequest reportParams = new ReportRequest();
		reportParams.setOptions(options);
		reportParams.setData(sample);
		byte[] bytesSAR = webClient.post().uri(constants.getTranscriptReport())
				.headers(h -> {
					h.setBearerAuth(accessToken);
					h.set(constants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
				}).body(BodyInserters.fromValue(reportParams)).retrieve().bodyToMono(byte[].class).block();
		byte[] encoded = org.apache.commons.codec.binary.Base64.encodeBase64(bytesSAR);
		return new String(encoded, StandardCharsets.US_ASCII);
	}
}
