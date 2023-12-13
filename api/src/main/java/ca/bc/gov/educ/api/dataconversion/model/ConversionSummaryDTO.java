package ca.bc.gov.educ.api.dataconversion.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class ConversionSummaryDTO implements Serializable {
  private Long batchId;
  private String tableName;

  private long readCount = 0L;
  private long processedCount = 0L;

  private long addedCount = 0L;
  private long updatedCount = 0L;
  private long erroredCount = 0L;
  private List<ConversionAlert> errors = new ArrayList<>();
  private String exception;

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  private String accessToken;
}
