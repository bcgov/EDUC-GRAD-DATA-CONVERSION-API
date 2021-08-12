package ca.bc.gov.educ.api.dataconversion.entity.course;

import ca.bc.gov.educ.api.dataconversion.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "COURSE_RESTRICTION")
public class GradCourseRestrictionEntity extends BaseEntity {
   
	@Id
	@Column(name = "COURSE_RESTRICTION_ID", nullable = false)
    private UUID courseRestrictionId;

	@Column(name = "MAIN_COURSE", nullable = false)
    private String mainCourse;  
	
	@Column(name = "MAIN_COURSE_LEVEL", nullable = true)
    private String mainCourseLevel;
	
	@Column(name = "RESTRICTED_COURSE", nullable = false)
    private String restrictedCourse; 
	
	@Column(name = "RESTRICTED_COURSE_LVL", nullable = true)
    private String restrictedCourseLevel;   
	
	@Column(name = "RESTRICTION_EFFECTIVE_DATE", nullable = true)
    private Date restrictionStartDate; 
	
	@Column(name = "RESTRICTION_EXPIRY_DATE", nullable = true)
    private Date restrictionEndDate;

}
