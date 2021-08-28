package ca.bc.gov.educ.api.dataconversion.config;

import ca.bc.gov.educ.api.dataconversion.listener.CourseRestrictionDataConversionJobCompletionNotificationListener;
import ca.bc.gov.educ.api.dataconversion.model.GradCourseRestriction;
import ca.bc.gov.educ.api.dataconversion.processor.DataConversionCourseRestrictionProcessor;
import ca.bc.gov.educ.api.dataconversion.reader.DataConversionCourseRestrictionReader;
import ca.bc.gov.educ.api.dataconversion.service.conv.DataConversionService;

import ca.bc.gov.educ.api.dataconversion.writer.DataConversionCourseRestrictionWriter;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.annotation.*;
import org.springframework.batch.core.configuration.support.JobRegistryBeanPostProcessor;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ca.bc.gov.educ.api.dataconversion.listener.StudentDataConversionJobCompletionNotificationListener;
import ca.bc.gov.educ.api.dataconversion.model.ConvGradStudent;
import ca.bc.gov.educ.api.dataconversion.processor.DataConversionStudentProcessor;
import ca.bc.gov.educ.api.dataconversion.reader.DataConversionStudentReader;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import ca.bc.gov.educ.api.dataconversion.writer.DataConversionStudentWriter;


@Configuration
@EnableBatchProcessing
public class BatchJobConfig {

    @Autowired
    JobRegistry jobRegistry;

    @Bean
    public ItemReader<ConvGradStudent> studentReader(DataConversionService dataConversionService, RestUtils restUtils) {
        return new DataConversionStudentReader(dataConversionService, restUtils);
    }

    @Bean
    public ItemReader<GradCourseRestriction> courseRestrictionReader(DataConversionService dataConversionService, RestUtils restUtils) {
        return new DataConversionCourseRestrictionReader(dataConversionService, restUtils);
    }

    @Bean
    public ItemWriter<ConvGradStudent> studentWriter() {
        return new DataConversionStudentWriter();
    }

    @Bean
    public ItemWriter<GradCourseRestriction> courseRestrictionWriter() {
        return new DataConversionCourseRestrictionWriter();
    }

    @Bean
    public ItemProcessor<ConvGradStudent,ConvGradStudent> studentProcessor() {
        return new DataConversionStudentProcessor();
    }

    @Bean
    public ItemProcessor<GradCourseRestriction,GradCourseRestriction> courseRestrictionProcessor() {
        return new DataConversionCourseRestrictionProcessor();
    }

    /**
    * Creates a bean that represents the only steps of our batch job.
    */
    @Bean
    public Step studentDataConversionJobStep(ItemReader<ConvGradStudent> studentReader,
                                        ItemProcessor<? super ConvGradStudent, ? extends ConvGradStudent> studentProcessor,
                                        ItemWriter<ConvGradStudent> studentWriter,
                                        StepBuilderFactory stepBuilderFactory) {
    return stepBuilderFactory.get("studentDataConversionJobStep")
            .<ConvGradStudent, ConvGradStudent>chunk(1)
            .reader(studentReader)
            .processor(studentProcessor)
            .writer(studentWriter)
            .build();
    }

    /**
    * Creates a bean that represents our batch job.
    */
    @Bean
    public Job studentDataConversionBatchJob(Step studentDataConversionJobStep,
                                        StudentDataConversionJobCompletionNotificationListener listener,
                                        JobBuilderFactory jobBuilderFactory) {
    return jobBuilderFactory.get("studentDataConversionBatchJob")
            .incrementer(new RunIdIncrementer())
            .listener(listener)
            .flow(studentDataConversionJobStep)
            .end()
            .build();
    }

    /**
     * Creates a bean that represents the only steps of our batch job.
     */
    @Bean
    public Step courseRestrictionDataConversionJobStep(ItemReader<GradCourseRestriction> courseRestrictionReader,
                                                    ItemProcessor<? super GradCourseRestriction, ? extends GradCourseRestriction> courseRestrictionProcessor,
                                                    ItemWriter<GradCourseRestriction> courseRestrictionWriter,
                                                    StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("courseRestrictionDataConversionJobStep")
                .<GradCourseRestriction, GradCourseRestriction>chunk(1)
                .reader(courseRestrictionReader)
                .processor(courseRestrictionProcessor)
                .writer(courseRestrictionWriter)
                .build();
    }

    /**
     * Creates a bean that represents our batch job.
     */
    @Bean
    public Job courseRestrictionDataConversionBatchJob(Step courseRestrictionDataConversionJobStep,
                                                    CourseRestrictionDataConversionJobCompletionNotificationListener listener,
                                                    JobBuilderFactory jobBuilderFactory) {
        return jobBuilderFactory.get("courseRestrictionDataConversionBatchJob")
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(courseRestrictionDataConversionJobStep)
                .end()
                .build();
    }

    @Bean
    public JobRegistryBeanPostProcessor jobRegistryBeanPostProcessor() {
        JobRegistryBeanPostProcessor postProcessor = new JobRegistryBeanPostProcessor();
        postProcessor.setJobRegistry(jobRegistry);
        return postProcessor;
    }
}
