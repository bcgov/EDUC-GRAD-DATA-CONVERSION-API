package ca.bc.gov.educ.api.dataconversion.entity.assessment;

import ca.bc.gov.educ.api.dataconversion.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Date;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "ASSESSMENT_REQUIREMENT_CODE")
public class AssessmentRequirementCodeEntity extends BaseEntity {
   
	@Id
	@Column(name = "ASSESSMENT_REQUIREMENT_CODE", nullable = false)
    private String assmtRequirementCode;

    @Column(name = "LABEL", nullable = true)
    private String label;

    @Column(name = "DESCRIPTION", nullable = false)
    private String description;

    @Column(name = "EFFECTIVE_DATE", nullable = false)
    private Date effectiveDate;

    @Column(name = "EXPIRY_DATE", nullable = false)
    private Date expiryDate;
}
