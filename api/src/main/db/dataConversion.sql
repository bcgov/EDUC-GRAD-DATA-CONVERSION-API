------------------------------
-- Initial data set from TRAX
------------------------------
-- GRAD_STUDENT
select trim(m.stud_no) as PEN, m.mincode as SCHOOL_OF_RECORD, m.mincode_grad as SCHOOL_AT_GRADUATION, m.stud_grade as STUDENT_GRADE,
       m.stud_status as STUDENT_STATUS_CODE, m.archive_flag as ARCHIVE_FLAG,
       m.grad_reqt_year as GRAD_REQT_YEAR, 'Y' as RECALCULATE_GRAD_STATUS, m.grad_date as GRAD_DATE,
       trim(m.prgm_code) as PRGM_CODE1, trim(m.prgm_code2) as PRGM_CODE2, trim(m.prgm_code3) as PRGM_CODE3, trim(m.prgm_code4) as PRGM_CODE4, trim(m.prgm_code5) as PRGM_CODE5
from trax_students_load l, student_master m
where 1 = 1
  and l.stud_no = m.stud_no
--and m.grad_date = 0
--and m.archive_flag = 'A'

-- GRAD_COURSE_RESTRICTIONS
select trim(c1.crse_code) as CRSE_MAIN, trim(c1.crse_level) as CRSE_MAIN_LVL,
 trim(c2.crse_code) as CRSE_RESTRICTED, trim(c2.crse_level) as CRSE_RESTRICTED_LVL,
 trim(c1.start_restrict_session) as RESTRICTION_START_DT, trim(c1.end_restrict_session) as RESTRICTION_END_DT
from tab_crse c1
join tab_crse c2
on c1.restriction_code = c2.restriction_code
and (c1.crse_code  <> c2.crse_code
 or  c1.crse_level <> c2.crse_level)
and c1.restriction_code <> ' '

------------------------------
-- Validation Queries
------------------------------
-- French immersion validation by pen
-- Old
select count(*) from STUD_XCRSE sx, COURSE_REQUIREMENT cr
where 1 = 1
  and sx.stud_no = '131493553'  -- pen
  and trim(sx.crse_code) = cr.course_code
  and trim(sx.crse_level) = cr.course_level
  and cr.course_requirement_code = 202

-- New
select count(*) from TRAX_STUDENT_COURSES tsc, COURSE_REQUIREMENT cr
where 1 = 1
  and tsc.pen = '131493553'  -- pen
  and trim(tsc.crse_code) = cr.course_code
  and trim(tsc.crse_level) = cr.course_level
  and cr.course_requirement_code = 202
