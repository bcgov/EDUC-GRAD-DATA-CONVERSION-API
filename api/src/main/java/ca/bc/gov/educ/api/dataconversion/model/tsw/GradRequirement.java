package ca.bc.gov.educ.api.dataconversion.model.tsw;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GradRequirement {
    String rule;
    String description;
    boolean projected;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GradRequirement that = (GradRequirement) o;
        return Objects.equals(rule, that.rule);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rule);
    }
}
