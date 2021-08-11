CREATE TABLE STUD_XCRSE (
    id RAW(16) NOT NULL,
    stud_no VARCHAR(10) NOT NULL,
    crse_code VARCHAR(5) NOT NULL,
    crse_level VARCHAR(3),
    PRIMARY KEY(id)
);

CREATE TABLE GRAD_COURSE_REQUIREMENT (
    id RAW(16) NOT NULL,
    crse_code VARCHAR(5) NOT NULL,
    crse_lvl VARCHAR(3),
    rule_code VARCHAR(4),
    PRIMARY KEY(id)
);
