package ca.bc.gov.educ.api.dataconversion.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

@Data
@NoArgsConstructor
public class ConversionSummaryDTO {

  private String tableName;

  private long readCount = 0L;
  private long processedCount = 0L;

  private long addedCount = 0L;
  private long updatedCount = 0L;

  private List<ConversionError> errors = new ArrayList<>();
  private String exception;

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
    put("1996", 0L);
    put("1996 Grad", 0L);
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

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  private String accessToken;

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
}
