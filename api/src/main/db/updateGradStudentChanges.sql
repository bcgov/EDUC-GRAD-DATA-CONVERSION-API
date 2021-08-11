CREATE OR ALTER PROCEDURE [updateGradStudentChanges]
AS
	DECLARE @newStudentList StudentList;
	DECLARE @newStudentDemographicList StudentDemographicList;
	DECLARE @existingStudentList StudentDemographicList;
	DECLARE @existingStudentDemographicList StudentDemographicList;

BEGIN
	SET NOCOUNT ON;

	INSERT INTO @newStudentList 
		SELECT t.* from TRAX_STUDENTS_LOAD t;
	
	Declare @noOfStudentsCount Int;
	select @noOfStudentsCount = count(*) from @newStudentList;
	PRINT N'Student Count: ' + RTRIM(CAST(@noOfStudentsCount AS nvarchar(30)))
	
	INSERT INTO @existingStudentList 
		SELECT t.* from GRAD_STUDENT_TEMP;	

	DELETE FROM @newStudentList where STUD_NO in (
		SELECT STUD_NO from @existingStudentList 
	);
	
	INSERT INTO @newStudentDemographicList 
		SELECT t.* from STUDENT_MASTER where STUD_NO in
					(SELECT STUD_NO from @newStudentList);
	INSERT INTO @existingStudentDemographicList 
		SELECT t.* from STUDENT_MASTER where STUD_NO in
					(SELECT PEN from @existingStudentList);		
BEGIN
	INSERT into GRAD_STUDENT_TEMP
		select 
		t.PEN,
		FROM @newStudentDemographicList t

	UPDATE GRAD_STUDENT_TEMP SET RECALCULATE_GRAD_STATUS = 'Y' where IDX in
					(SELECT PENfrom @existingStudentList);
		
END;
END;