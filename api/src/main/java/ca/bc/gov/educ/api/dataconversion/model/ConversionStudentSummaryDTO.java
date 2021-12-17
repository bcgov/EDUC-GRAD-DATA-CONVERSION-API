package ca.bc.gov.educ.api.dataconversion.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@NoArgsConstructor
public class ConversionStudentSummaryDTO extends ConversionBaseSummaryDTO {

  // stats
  private Map<String, Long> programCountMap = new LinkedHashMap<>() {{
    put("2018-EN", 0L);
    put("2018-EN Grad", 0L);
    put("2018-PF", 0L);
    put("2018-PF Grad", 0L);
    put("2004-EN", 0L);
    put("2004-EN Grad", 0L);
    put("2004-PF", 0L);
    put("2004-PF Grad", 0L);
    put("1996-EN", 0L);
    put("1996-EN Grad", 0L);
    put("1996-PF", 0L);
    put("1996-PF Grad", 0L);
    put("1986", 0L);
    put("1986 Grad", 0L);
    put("1950", 0L);
    put("1950 Grad", 0L);
    put("NOPROG", 0L);
    put("SCCP", 0L);
  }};

  // optional program stats
  private Map<String, Long> optionalProgramCountMap = new LinkedHashMap<>() {{
    put("FI", 0L);
    put("AD", 0L);
    put("BC", 0L);
    put("BD", 0L);
  }};

  // career program stats
  private Map<String, Long> careerProgramCountMap = new HashMap<>();

  public void increment(String programCode, boolean isGraduated) {
    if (isGraduated) {
      programCode = programCode + " Grad";
    }
    Long count = programCountMap.get(programCode);
    if (count != null) {
      count++;
      programCountMap.put(programCode, count);
    }
  }

  public void incrementOptionalProgram(String optionalProgramCode) {
    Long count = optionalProgramCountMap.get(optionalProgramCode);
    if (count != null) {
      count++;
      optionalProgramCountMap.put(optionalProgramCode, count);
    } else {
      optionalProgramCountMap.put(optionalProgramCode, 1L);
    }
  }

  public void incrementCareerProgram(String careerProgramCode) {
    Long count = careerProgramCountMap.get(careerProgramCode);
    if (count != null) {
      count++;
      careerProgramCountMap.put(careerProgramCode, count);
    } else {
      careerProgramCountMap.put(careerProgramCode, 1L);
    }
  }
}
