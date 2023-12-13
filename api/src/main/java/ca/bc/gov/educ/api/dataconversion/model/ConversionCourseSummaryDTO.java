package ca.bc.gov.educ.api.dataconversion.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class ConversionCourseSummaryDTO extends ConversionSummaryDTO implements Serializable {

  private long addedCountForCourseRequirement = 0L;
  private long updatedCountForCourseRequirement = 0L;

  private long addedCountForCourseRestriction = 0L;
  private long updatedCountForCourseRestriction = 0L;

  private long addedCountForAssessmentRequirement = 0L;
  private long updatedCountForAssessmentRequirement = 0L;

}
