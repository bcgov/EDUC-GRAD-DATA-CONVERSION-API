package ca.bc.gov.educ.api.dataconversion.repository.student;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ca.bc.gov.educ.api.dataconversion.entity.student.GraduationStudentRecordEntity;

@Repository
public interface GraduationStudentRecordRepository extends JpaRepository<GraduationStudentRecordEntity, UUID> {

    @Modifying
    @Query(value="insert into STUDENT_GUID_PEN_XREF(STUDENT_GUID, STUDENT_PEN, CREATE_USER, CREATE_DATE, UPDATE_USER, UPDATE_DATE)\n"
            + "values (:studentGuid, :pen, :userName, :currentTime, :userName, :currentTime) ", nativeQuery=true)
    void createStudentGuidPenXrefRecord(
            @Param("studentGuid") UUID studentGuid,
            @Param("pen") String pen,
            @Param("userName") String userName,
            @Param("currentTime") LocalDateTime currentTime);

    @Modifying
    @Query(value="update STUDENT_GUID_PEN_XREF\n"
            + "set STUDENT_PEN = :pen, UPDATE_USER = :userName, UPDATE_DATE = :currentTime\n"
            + "where STUDENT_GUID = :studentGuid", nativeQuery=true)
    void updateStudentGuidPenXrefRecord(
            @Param("studentGuid") UUID studentGuid,
            @Param("pen") String pen,
            @Param("userName") String userName,
            @Param("currentTime") LocalDateTime currentTime);

    @Query(value="select count(*) from STUDENT_GUID_PEN_XREF gpx \n" +
            "where gpx.STUDENT_GUID = :studentGuid", nativeQuery=true)
    long countStudentGuidPenXrefRecord(@Param("studentGuid") UUID studentGuid);

    @Query(value="select STUDENT_GUID from STUDENT_GUID_PEN_XREF \n"
            + "where STUDENT_PEN = :pen", nativeQuery = true)
    byte[] findStudentID(@Param("pen") String pen);
}
