package ca.bc.gov.educ.api.dataconversion.entity.conv;

import lombok.Data;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Where;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

@Data
@Immutable
@Entity
@Where(clause = "GRAD_REQT_YEAR in ('2004','2018')")
@Table(name = "TAB_GRAD_CRSE")
public class GraduationCourseEntity {

    @EmbeddedId
    private GraduationCourseKey graduationCourseKey;

    @Column(name = "START_SESSION", nullable = true)
    private String startSession;

    @Column(name = "END_SESSION", nullable = true)
    private String endSession;

    @Column(name = "GRAD_ENGLISH_10", nullable = true)
    private String english10;
    @Column(name = "GRAD_ENGLISH_11", nullable = true)
    private String english11;
    @Column(name = "GRAD_ENGLISH_12", nullable = true)
    private String english12;

    @Column(name = "GRAD_CAREER_PERSONAL_10", nullable = true)
    private String careerPersonal10;
    @Column(name = "GRAD_CAREER_PERSONAL_11", nullable = true)
    private String careerPersonal11;
    @Column(name = "GRAD_CAREER_PERSONAL_12", nullable = true)
    private String careerPersonal12;

    @Column(name = "GRAD_SOCIALS", nullable = true)
    private String socials;
    @Column(name = "GRAD_SOCIALS_10", nullable = true)
    private String socials10;

    @Column(name = "GRAD_SCIENCE", nullable = true)
    private String science;
    @Column(name = "GRAD_SCIENCE_10", nullable = true)
    private String science10;

    @Column(name = "GRAD_MATH", nullable = true)
    private String math;
    @Column(name = "GRAD_MATH_10", nullable = true)
    private String math10;

    @Column(name = "GRAD_PHYS_ED", nullable = true)
    private String physEd;
    @Column(name = "GRAD_PHYS_ED_10", nullable = true)
    private String physEd10;

    @Column(name = "GRAD_APPLIED_SKILLS", nullable = true)
    private String appliedSkills;
    @Column(name = "GRAD_PORTFOLIO", nullable = true)
    private String portfolio;
    @Column(name = "GRAD_CONS_ED", nullable = true)
    private String consEd;
    @Column(name = "GRAD_FINE_ARTS", nullable = true)
    private String fineArts;
    @Column(name = "GRAD_CAREER_LIFE_CONNECTIONS", nullable = true)
    private String careerLifeConnections;
}
