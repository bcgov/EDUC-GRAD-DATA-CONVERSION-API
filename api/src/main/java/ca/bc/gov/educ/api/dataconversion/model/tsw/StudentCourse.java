package ca.bc.gov.educ.api.dataconversion.model.tsw;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StudentCourse {

	private String pen;
	private String courseCode;
	private String courseName;
	private Integer originalCredits;
	private String courseLevel;
	private String sessionDate;
	private String customizedCourseName;
	private String gradReqMet;
	private String gradReqMetDetail;
	private Double completedCoursePercentage;
	private String completedCourseLetterGrade;
	private Double interimPercent;
	private String interimLetterGrade;
	private Double bestSchoolPercent;
	private Double bestExamPercent;
	private Double schoolPercent;
	private Double examPercent;
	private String equivOrChallenge;
	private String fineArtsAppliedSkills;
	private String metLitNumRequirement;
	private Integer credits;
	private Integer creditsUsedForGrad;
	private String relatedCourse;
	private String relatedCourseName;
	private String relatedLevel;
	private String hasRelatedCourse;
	private String genericCourseType;
	private String language;
	private String workExpFlag;
	private String specialCase;
	private String toWriteFlag;
	private String provExamCourse;
	private boolean isNotCompleted;
	private boolean isProjected;
	private boolean isFailed;
	private boolean isDuplicate;
	private boolean isCareerPrep;
	private boolean isLocallyDeveloped;
	private boolean isUsed;
	private boolean isRestricted;
	private boolean isNotEligibleForElective;
	private boolean isBoardAuthorityAuthorized;
	private boolean isIndependentDirectedStudies;
	private boolean isLessCreditCourse;
	private boolean isValidationCourse;
	private boolean isCutOffCourse;
	private boolean isGrade10Course;

	public Integer getCreditsUsedForGrad() {
		if (creditsUsedForGrad == null)
			return 0;
		else
			return creditsUsedForGrad;
	}

	public String getCourseCode() {
		if (courseCode != null)
			courseCode = courseCode.trim();
		return courseCode;
	}

	public String getCourseLevel() {
		if (courseLevel != null)
			courseLevel = courseLevel.trim();
		return courseLevel;
	}

	@Override
	public String toString() {
		return "StudentCourse [pen=" + pen + ", courseCode=" + courseCode + ", courseName=" + courseName
				+ ", courseLevel=" + courseLevel + ", sessionDate=" + sessionDate + ", customizedCourseName="
				+ customizedCourseName + ", gradReqMet=" + gradReqMet + ", gradReqMetDetail=" + gradReqMetDetail
				+ ", completedCoursePercentage=" + completedCoursePercentage + ", completedCourseLetterGrade="
				+ completedCourseLetterGrade + ", interimPercent=" + interimPercent + ", interimLetterGrade="
				+ interimLetterGrade + ", credits=" + credits + ", creditsUsedForGrad=" + creditsUsedForGrad
				+ ", relatedCourse=" + relatedCourse + ", relatedLevel=" + relatedLevel + ", hasRelatedCourse="
				+ hasRelatedCourse + ", isNotCompleted=" + isNotCompleted + ", isProjected=" + isProjected
				+ ", isFailed=" + isFailed + ", isDuplicate=" + isDuplicate + ", isCareerPrep=" + isCareerPrep
				+ ", isLocallyDeveloped=" + isLocallyDeveloped + ", isUsed=" + isUsed + "]";
	}
}
