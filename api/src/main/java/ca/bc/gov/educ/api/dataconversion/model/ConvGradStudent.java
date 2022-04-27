package ca.bc.gov.educ.api.dataconversion.model;

import ca.bc.gov.educ.api.dataconversion.constant.ConversionResultType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConvGradStudent {
  private String pen;
  private String program; // inc
  private Date programCompletionDate;
  private String slpDate; // inc
  private String gpa;
  private String honoursStanding;
  private String recalculateGradStatus;
  private String studentGradData;
  private String schoolOfRecord; // inc
  private String schoolAtGrad; // inc
  private String studentGrade; // inc
  private String studentStatus; // inc
  private String archiveFlag;
  private String frenchCert;
  private String consumerEducationRequirementMet; // inc

  // extra
  private String graduationRequestYear;

  // program codes for optional / career program
  private List<String> programCodes;

  // grad or non-grad
  private boolean graduated;

  // data conversion status after being processed.
  private ConversionResultType result;
}
