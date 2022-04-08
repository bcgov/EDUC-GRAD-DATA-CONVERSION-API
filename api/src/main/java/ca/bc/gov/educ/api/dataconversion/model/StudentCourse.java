package ca.bc.gov.educ.api.dataconversion.model;

import lombok.Data;

@Data
public class StudentCourse {

	private String pen;
    private String courseCode;
    private String courseName;
	private Integer originalCredits;
    private String courseLevel;
    private String sessionDate;
    private String customizedCourseName;
	private String gradReqMet;
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
	private boolean isFailed;
	private boolean isDuplicate;
	private Course courseDetails;
	
	public String getPen() {
    	return pen != null ? pen.trim():null;
    }
	
	public String getCourseCode() {
		return courseCode != null ? courseCode.trim(): null;
	}
	public String getCourseName() {
		return courseName != null ? courseName.trim(): null; 
	}	

	public String getCourseLevel() {
		return courseLevel != null ? courseLevel.trim(): null;
	}
	
	public String getCustomizedCourseName() {
		return customizedCourseName != null ? customizedCourseName.trim(): null;
	}

	public String getCompletedCourseLetterGrade() {
		return completedCourseLetterGrade != null ? completedCourseLetterGrade.trim(): null;
	}

	public String getInterimLetterGrade() {
		return interimLetterGrade != null ? interimLetterGrade.trim(): null;
	}

	public String getRelatedCourse() {
		return relatedCourse != null ? relatedCourse.trim(): null;
	}

	public String getRelatedLevel() {
		return  relatedLevel != null ?  relatedLevel.trim(): null;
	}
	
	public String getGenericCourseType() {
		return genericCourseType != null ?  genericCourseType.trim(): null;
	}

	public Double getCompletedCoursePercentage() {
		if(completedCoursePercentage == null) {
			return Double.valueOf("0");
		}
		return completedCoursePercentage; 
	}

    public String getEquivOrChallenge() {
    	return equivOrChallenge != null ? equivOrChallenge.trim() : null;
    }
    
    public String getFineArtsAppliedSkills() {
    	return fineArtsAppliedSkills != null ? fineArtsAppliedSkills.trim() : null;
    }
    
    public String getMetLitNumRequirement() {
    	return metLitNumRequirement != null ? metLitNumRequirement.trim() : null;
    }

	@Override
	public String toString() {
		return "StudentCourse [pen=" + pen + ", courseCode=" + courseCode + ", courseName=" + courseName
				+ ", courseLevel=" + courseLevel + ", sessionDate=" + sessionDate + ", customizedCourseName="
				+ customizedCourseName + ", gradReqMet=" + gradReqMet + ", completedCoursePercentage="
				+ completedCoursePercentage + ", completedCourseLetterGrade=" + completedCourseLetterGrade
				+ ", interimPercent=" + interimPercent + ", interimLetterGrade=" + interimLetterGrade
				+ ", bestSchoolPercent=" + bestSchoolPercent + ", bestExamPercent=" + bestExamPercent
				+ ", equivOrChallenge=" + equivOrChallenge + ", fineArtsAppliedSkills=" + fineArtsAppliedSkills
				+ ", metLitNumRequirement=" + metLitNumRequirement + ", credits=" + credits + ", creditsUsedForGrad="
				+ creditsUsedForGrad + ", relatedCourse=" + relatedCourse + ", relatedCourseName=" + relatedCourseName
				+ ", relatedLevel=" + relatedLevel + ", hasRelatedCourse=" + hasRelatedCourse + ", genericCourseType="
				+ genericCourseType + ", language=" + language + ", workExpFlag=" + workExpFlag + ", specialCase="
				+ specialCase + ", toWriteFlag=" + toWriteFlag + ", isNotCompleted=" + isNotCompleted + ", isFailed="
				+ isFailed + ", isDuplicate=" + isDuplicate + ", courseDetails=" + courseDetails + "]";
	}		
}