package ca.bc.gov.educ.api.dataconversion.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
public class TraxStudentNo {
    private String studNo;
    private String status;
    private String reason;
}
