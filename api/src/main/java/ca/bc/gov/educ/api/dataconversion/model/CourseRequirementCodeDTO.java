package ca.bc.gov.educ.api.dataconversion.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.sql.Date;

@Data
@EqualsAndHashCode(callSuper = false)
public class CourseRequirementCodeDTO extends BaseModel {
    private String courseRequirementCode;
    private String label;
    private String description;
    private Date effectiveDate;
    private Date expiryDate;
}
