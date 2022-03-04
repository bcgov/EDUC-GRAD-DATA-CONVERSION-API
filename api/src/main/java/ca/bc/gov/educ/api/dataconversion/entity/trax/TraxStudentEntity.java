package ca.bc.gov.educ.api.dataconversion.entity.trax;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "TRAX_STUDENT_NO")
public class TraxStudentEntity {
    @Id
    @Column(name = "STUD_NO", unique = true, updatable = false)
    private String studNo;

    @Column(name = "STATUS", nullable = false)
    private String status;
}
