package ca.bc.gov.educ.api.dataconversion.mappers;

import ca.bc.gov.educ.api.dataconversion.entity.ConvGradStudentEntity;
import ca.bc.gov.educ.api.dataconversion.model.ConvGradStudent;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ConvGradStudentMapper {
    @Autowired
    ModelMapper modelMapper;

    public ConvGradStudent toModel(ConvGradStudentEntity convGradStudentEntity) {
        ConvGradStudent convGradStudent = modelMapper.map(convGradStudentEntity, ConvGradStudent.class);
        return convGradStudent;
    }

    public ConvGradStudentEntity toEntity(ConvGradStudent convGradStudent) {
        ConvGradStudentEntity convGradStudentEntity = modelMapper.map(convGradStudent, ConvGradStudentEntity.class);
        return convGradStudentEntity;
    }
}
