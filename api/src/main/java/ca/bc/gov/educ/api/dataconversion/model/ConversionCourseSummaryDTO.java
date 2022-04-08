package ca.bc.gov.educ.api.dataconversion.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@NoArgsConstructor
public class ConversionCourseSummaryDTO extends ConversionBaseSummaryDTO {

  private long addedCountForCourseRequirement = 0L;
  private long updatedCountForCourseRequirement = 0L;

  private long addedCountForCourseRestriction = 0L;
  private long updatedCountForCourseRestriction = 0L;

  private long addedCountForAssessmentRequirement = 0L;
  private long updatedCountForAssessmentRequirement = 0L;

}
