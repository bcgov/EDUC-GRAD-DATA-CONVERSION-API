package ca.bc.gov.educ.api.dataconversion.entity.conv;

import lombok.Data;
import org.hibernate.annotations.Immutable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Immutable
@Entity
@Table(name = "TRAX_STUDENTS_LOAD")
public class TraxStudentsLoadEntity {
    @Id
    @Column(name = "STUD_NO", nullable = false)
    private String pen;
}
