package ca.bc.gov.educ.api.dataconversion.model;

import lombok.Data;
import java.sql.Date;

@Data
public class Course {

	private String courseCode;
    private String courseLevel;
    private String courseName;
    private String language;    
    private Date startDate;
    private Date endDate;
    private String workExpFlag;    
    private String genericCourseType;
    private String coRegID;
	private Integer numCredits;
    
    public String getCourseCode() {
		return courseCode != null ? courseCode.trim(): null;
	}
	public String getCourseName() {
		return courseName != null ? courseName.trim(): null; 
	}	

	public String getCourseLevel() {
		return courseLevel != null ? courseLevel.trim(): null;
	}
	
	public String getLanguage() {
		return language != null ? language.trim(): null;
	}
	
	public String getGenericCourseType() {
		return genericCourseType != null ? genericCourseType.trim(): null;
	}
    
	@Override
	public String toString() {
		return "Course [courseCode=" + courseCode + ", courseLevel=" + courseLevel + ", courseName=" + courseName
				+ ", language=" + language + ", startDate=" + startDate + ", endDate=" + endDate + ", workExpFlag="
				+ workExpFlag + ", genericCourseType=" + genericCourseType + "]";
	}		
}
