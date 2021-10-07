package ca.bc.gov.educ.api.dataconversion.mappers;

import ca.bc.gov.educ.api.dataconversion.entity.student.GraduationStudentRecordEntity;
import ca.bc.gov.educ.api.dataconversion.model.ConvGradStudent;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ConvGradStudentMapper {
    @Autowired
    ModelMapper modelMapper;

    public ConvGradStudent toModel(GraduationStudentRecordEntity gradStudentEntity) {
        ConvGradStudent convGradStudent = modelMapper.map(gradStudentEntity, ConvGradStudent.class);
        return convGradStudent;
    }

    public GraduationStudentRecordEntity toEntity(ConvGradStudent convGradStudent) {
        GraduationStudentRecordEntity gradStudentEntity = modelMapper.map(convGradStudent, GraduationStudentRecordEntity.class);
        return gradStudentEntity;
    }
}
