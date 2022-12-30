package ca.bc.gov.educ.api.dataconversion.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TraxXProgramDTO extends TraxStudentUpdateDTO {

    // PRGM_CODE, PRGM_CODEw, PRGM_CODE3, PRGM_CODE4, PRGM_CODE5
    private List<String> programList;
}
