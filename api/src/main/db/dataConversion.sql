------------------------------
-- Initial data set from TRAX
------------------------------
-- GRAD the non-graduated STUDENT
select trim(m.stud_no) as PEN, m.mincode as SCHOOL_OF_RECORD, m.mincode_grad as SCHOOL_AT_GRADUATION, m.stud_grade as STUDENT_GRADE,
       m.stud_status as STUDENT_STATUS_CODE, m.archive_flag as ARCHIVE_FLAG,
       m.grad_reqt_year as GRAD_REQT_YEAR, 'Y' as RECALCULATE_GRAD_STATUS, m.grad_date as GRAD_DATE,
       trim(m.prgm_code) as PRGM_CODE1, trim(m.prgm_code2) as PRGM_CODE2, trim(m.prgm_code3) as PRGM_CODE3, trim(m.prgm_code4) as PRGM_CODE4, trim(m.prgm_code5) as PRGM_CODE5,
       m.slp_date as SLP_DATE, trim(m.french_cert) as FRENCH_CERT, trim(m.stud_consed_flag) as STUD_CONSED_FLAG, trim(m.english_cert) as ENGLISH_CERT, m.honour_flag as HONOUR_FLAG
from student_master m
where 1 = 1
  and m.grad_date = 0
--and m.stud_no = ''

-- GRAD the graduated STUDENT
select trim(gs.stud_no) as PEN, gs.mincode as SCHOOL_OF_RECORD, m.mincode_grad as SCHOOL_AT_GRADUATION, gs.stud_grade as STUDENT_GRADE,
       m.stud_status as STUDENT_STATUS_CODE, m.archive_flag as ARCHIVE_FLAG,
       gs.grad_reqt_year as GRAD_REQT_YEAR, null as RECALCULATE_GRAD_STATUS, gs.grad_date as GRAD_DATE,
       trim(m.prgm_code) as PRGM_CODE1, trim(m.prgm_code2) as PRGM_CODE2, trim(m.prgm_code3) as PRGM_CODE3, trim(m.prgm_code4) as PRGM_CODE4, trim(m.prgm_code5) as PRGM_CODE5,
       m.slp_date as SLP_DATE, trim(m.french_cert) as FRENCH_CERT, trim(m.stud_consed_flag) as STUD_CONSED_FLAG, trim(m.english_cert) as ENGLISH_CERT, m.honour_flag as HONOUR_FLAG
from student_master m, tsw_tran_demog gs
where 1 = 1
  and m.stud_no = gs.stud_no
  and gs.grad_date <> 0
--and m.stud_no = ''

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

-- Language Course with blank validation by courseCode & courseLevel
select count(*) from TAB_CRSE cr
where cr.crse_code =  'SMA'
and cr.crse_level = '12'
and cr.language = ' '

-- Language Course with french validation by courseCode & courseLevel
select count(*) from TAB_CRSE cr
where cr.crse_code =  'SMA'
  and cr.crse_level = '12'
  and cr.language = 'F'
