package ca.bc.gov.educ.api.dataconversion.model;

import ca.bc.gov.educ.api.dataconversion.constant.ConversionResultType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConvGradStudent {
  private String pen;
  private String program; // inc
  private Date programCompletionDate; // inc
  private String slpDate; // inc
  private String sccDate;
  private String gpa;
  private String honoursStanding; // inc
  private String studentGradData;
  private String schoolOfRecord; // inc
  private String schoolAtGrad; // inc
  private String studentGrade; // inc
  private String studentStatus; // inc
  private String archiveFlag; // inc
  private String frenchCert;
  private String englishCert;
  private String frenchDogwood;
  private String consumerEducationRequirementMet;
  private String studentCitizenship;

  // extra
  private String graduationRequirementYear;

  // program codes for optional / career program
  private List<String> programCodes;

  // 1950 "AD"
  private boolean adult19Rule;

  // data conversion status after being processed.
  private ConversionResultType result;
}
