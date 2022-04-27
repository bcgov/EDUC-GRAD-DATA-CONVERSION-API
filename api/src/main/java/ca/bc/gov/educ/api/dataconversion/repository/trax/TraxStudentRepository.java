package ca.bc.gov.educ.api.dataconversion.repository.trax;

import ca.bc.gov.educ.api.dataconversion.entity.trax.TraxStudentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TraxStudentRepository extends PagingAndSortingRepository<TraxStudentEntity, String> {
    Page<TraxStudentEntity> findAllByStatus(String status, Pageable pageable);
    Integer countAllByStatus(String status);
}
