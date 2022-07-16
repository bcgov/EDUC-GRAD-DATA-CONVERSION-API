package ca.bc.gov.educ.api.dataconversion.model.tsw;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GradRequirement {
    String rule;
    String description;
    boolean projected;
}
