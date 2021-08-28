package ca.bc.gov.educ.api.dataconversion.entity.course;

import ca.bc.gov.educ.api.dataconversion.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "COURSE_REQUIREMENT")
public class CourseRequirementEntity extends BaseEntity {
   
	@Id
	@Column(name = "COURSE_REQUIREMENT_ID", nullable = false)
    private UUID courseRequirementId;

    @Column(name = "COURSE_CODE", nullable = false)
    private String courseCode;   

    @Column(name = "COURSE_LEVEL", nullable = true)
    private String courseLevel;   

    @OneToOne(cascade = CascadeType.DETACH)
    @JoinColumn(name = "COURSE_REQUIREMENT_CODE", referencedColumnName = "COURSE_REQUIREMENT_CODE")
    private CourseRequirementCodeEntity ruleCode;
}
