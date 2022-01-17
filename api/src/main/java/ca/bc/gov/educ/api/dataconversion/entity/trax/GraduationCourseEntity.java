package ca.bc.gov.educ.api.dataconversion.entity.trax;

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
//@Where(clause = "GRAD_REQT_YEAR in ('2004','2018','1950','1996','1986')")
@Where(clause = "GRAD_REQT_YEAR = '1996' and CRSE_CODE = 'AC'")
@Table(name = "TAB_GRAD_CRSE")
public class GraduationCourseEntity {

    @EmbeddedId
    private GraduationCourseKey graduationCourseKey;

    @Column(name = "START_SESSION")
    private String startSession;

    @Column(name = "END_SESSION")
    private String endSession;

    @Column(name = "GRAD_ENGLISH_10")
    private String english10;
    @Column(name = "GRAD_ENGLISH_11")
    private String english11;
    @Column(name = "GRAD_ENGLISH_12")
    private String english12;

    @Column(name = "GRAD_CAREER_PERSONAL_10")
    private String careerPersonal10;
    @Column(name = "GRAD_CAREER_PERSONAL_11")
    private String careerPersonal11;
    @Column(name = "GRAD_CAREER_PERSONAL_12")
    private String careerPersonal12;

    @Column(name = "GRAD_SOCIALS")
    private String socials;
    @Column(name = "GRAD_SOCIALS_10")
    private String socials10;

    @Column(name = "GRAD_SCIENCE")
    private String science;
    @Column(name = "GRAD_SCIENCE_10")
    private String science10;

    @Column(name = "GRAD_MATH")
    private String math;
    @Column(name = "GRAD_MATH_10")
    private String math10;

    @Column(name = "GRAD_PHYS_ED")
    private String physEd;
    @Column(name = "GRAD_PHYS_ED_10")
    private String physEd10;

    @Column(name = "GRAD_APPLIED_SKILLS")
    private String appliedSkills;
    @Column(name = "GRAD_PORTFOLIO")
    private String portfolio;
    @Column(name = "GRAD_CONS_ED")
    private String consEd;
    @Column(name = "GRAD_FINE_ARTS")
    private String fineArts;
    @Column(name = "GRAD_CAREER_LIFE_CONNECTIONS")
    private String careerLifeConnections;
}
