package ca.bc.gov.educ.api.dataconversion.entity.assessment;

import ca.bc.gov.educ.api.dataconversion.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "ASSESSMENT_REQUIREMENT")
public class AssessmentRequirementEntity extends BaseEntity {
   
	@Id
	@Column(name = "ASSESSMENT_REQUIREMENT_ID", nullable = false)
    private UUID assessmentRequirementId;

    @Column(name = "ASSESSMENT_CODE", nullable = false)
    private String assessmentCode;   

    @OneToOne(cascade = CascadeType.DETACH)
    @JoinColumn(name = "ASSESSMENT_REQUIREMENT_CODE", referencedColumnName = "ASSESSMENT_REQUIREMENT_CODE")
    private AssessmentRequirementCodeEntity ruleCode;   

}
