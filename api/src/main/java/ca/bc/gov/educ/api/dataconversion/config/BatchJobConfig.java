package ca.bc.gov.educ.api.dataconversion.config;

import ca.bc.gov.educ.api.dataconversion.entity.trax.GraduationCourseEntity;
import ca.bc.gov.educ.api.dataconversion.entity.trax.TraxStudentEntity;
import ca.bc.gov.educ.api.dataconversion.listener.*;
import ca.bc.gov.educ.api.dataconversion.model.GradCourseRestriction;
import ca.bc.gov.educ.api.dataconversion.processor.*;
import ca.bc.gov.educ.api.dataconversion.reader.*;
import ca.bc.gov.educ.api.dataconversion.service.conv.DataConversionService;

import ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiConstants;
import ca.bc.gov.educ.api.dataconversion.writer.DataConversionAllTraxStudentsWriter;
import ca.bc.gov.educ.api.dataconversion.writer.DataConversionCourseRequirementWriter;
import ca.bc.gov.educ.api.dataconversion.writer.DataConversionCourseRestrictionWriter;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.annotation.*;
import org.springframework.batch.core.configuration.support.JobRegistryBeanPostProcessor;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ca.bc.gov.educ.api.dataconversion.model.ConvGradStudent;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import ca.bc.gov.educ.api.dataconversion.writer.DataConversionStudentWriter;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

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
    public ItemReader<GraduationCourseEntity> courseRequirementReader(DataConversionService dataConversionService, RestUtils restUtils) {
        return new DataConversionCourseRequirementReader(dataConversionService, restUtils);
    }

    @Bean
    public ItemReader<TraxStudentEntity> addMissingPenReader(DataConversionService dataConversionService, EducGradDataConversionApiConstants constants, RestUtils restUtils) {
        return new DataConversionAllTraxStudentsReader(dataConversionService, constants, restUtils);
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
    public ItemWriter<GraduationCourseEntity> courseRequirementWriter() {
        return new DataConversionCourseRequirementWriter();
    }

    @Bean
    public ItemWriter<TraxStudentEntity> addMissingPenWriter() {
        return new DataConversionAllTraxStudentsWriter();
    }

    @Bean
    public ItemProcessor<ConvGradStudent,ConvGradStudent> studentProcessor() {
        return new DataConversionStudentProcessor();
    }

    @Bean
    public ItemProcessor<GradCourseRestriction,GradCourseRestriction> courseRestrictionProcessor() {
        return new DataConversionCourseRestrictionProcessor();
    }

    @Bean
    public ItemProcessor<GraduationCourseEntity,GraduationCourseEntity> courseRequirementProcessor() {
        return new DataConversionCourseRequirementProcessor();
    }

    @Bean
    public AssessmentRequirementCreator assessmentRequirementCreator() {
        return new AssessmentRequirementCreator();
    }

    @Bean
    public CourseRequirementCreator courseRequirementCreator() {
        return new CourseRequirementCreator();
    }

    @Bean
    public ItemProcessor<TraxStudentEntity,TraxStudentEntity> addNewPenProcessor() {
        return new ReadTraxStudentAndAddNewPenProcessor();
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

    /**
     * Creates a bean that represents the only steps of our batch job.
     */
    @Bean
    public Step courseRequirementDataConversionJobStep(ItemReader<GraduationCourseEntity> courseRequirementReader,
                                                       ItemProcessor<? super GraduationCourseEntity, ? extends GraduationCourseEntity> courseRequirementProcessor,
                                                       ItemWriter<GraduationCourseEntity> courseRequirementWriter,
                                                       StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("courseRequirementDataConversionJobStep")
                .<GraduationCourseEntity, GraduationCourseEntity>chunk(1)
                .reader(courseRequirementReader)
                .processor(courseRequirementProcessor)
                .writer(courseRequirementWriter)
                .build();
    }

    @Bean
    public Step createAssessmentRequirementsJobStep(StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("createAssessmentRequirementsJobStep")
                .tasklet(assessmentRequirementCreator())
                .build();
    }

    @Bean
    public Step createCourseRequirementsJobStep(StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("createCourseRequirementsJobStep")
                .tasklet(courseRequirementCreator())
                .build();
    }

    /**
     * Creates a bean that represents our batch job.
     */
    @Bean
    public Job courseRequirementDataConversionBatchJob(Step courseRequirementDataConversionJobStep,
                                                       Step createAssessmentRequirementsJobStep,
                                                       Step createCourseRequirementsJobStep,
                                                       CourseRequirementDataConversionJobCompletionNotificationListener listener,
                                                       JobBuilderFactory jobBuilderFactory) {
        return jobBuilderFactory.get("courseRequirementDataConversionBatchJob")
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .start(courseRequirementDataConversionJobStep)
                .next(createAssessmentRequirementsJobStep)
                .next(createCourseRequirementsJobStep)
                .build();
    }

    @Bean
    public JobRegistryBeanPostProcessor jobRegistryBeanPostProcessor() {
        JobRegistryBeanPostProcessor postProcessor = new JobRegistryBeanPostProcessor();
        postProcessor.setJobRegistry(jobRegistry);
        return postProcessor;
    }

    /**
     * Creates a bean that represents the only steps of our batch job.
     */
    @Bean
    public Step readTraxAndAddNewPenJobStep(ItemReader<TraxStudentEntity> addMissingPenReader,
                                             ItemProcessor<? super TraxStudentEntity, ? extends TraxStudentEntity> addNewPenProcessor,
                                             ItemWriter<TraxStudentEntity> addMissingPenWriter,
                                             StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("readTraxAndAddNewPenJobStep")
                .<TraxStudentEntity, TraxStudentEntity>chunk(1)
                .reader(addMissingPenReader)
                .processor(addNewPenProcessor)
                .writer(addMissingPenWriter)
                .build();
    }

    /**
     * Creates a bean that represents our batch job.
     */
    @Bean
    public Job readTraxAndAddNewPenBatchJob(Step readTraxAndAddNewPenJobStep,
                                             AddMissingStudentsJobCompletionNotificationListener listener,
                                             JobBuilderFactory jobBuilderFactory) {
        return jobBuilderFactory.get("readTraxAndAddNewPenBatchJob")
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(readTraxAndAddNewPenJobStep)
                .end()
                .build();
    }

    // Partitioning for pen updates
    @Bean
    public Step masterStep(StepBuilderFactory stepBuilderFactory, DataConversionService dataConversionService) {
        return stepBuilderFactory.get("masterStep")
                .partitioner(slaveStep(stepBuilderFactory).getName(), partitioner(dataConversionService))
                .step(slaveStep(stepBuilderFactory))
                .gridSize(5)
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean
    public PenUpdatesPartitioner partitioner(DataConversionService dataConversionService) {
        // Reader to feed input data for each partition
        return new PenUpdatesPartitioner(dataConversionService);
    }

    @Bean
    public Step slaveStep(StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("slaveStep")
                .tasklet(penUpdatesPartitionHandler())
                .build();
    }

    @Bean
    @StepScope
    public PenUpdatesPartitionHandlerCreator penUpdatesPartitionHandler() {
        // Processor for each partition
        return new PenUpdatesPartitionHandlerCreator();
    }

    @Bean
    public Job penUpdatesJob(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory,
                             DataConversionService dataConversionService,
                             PenUpdatesJobCompletionNotificationListener listener) {
        return jobBuilderFactory.get("penUpdatesJob")
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(masterStep(stepBuilderFactory, dataConversionService))
                .end()
                .build();
    }

    @Bean
    public SimpleAsyncTaskExecutor taskExecutor() {
        return new SimpleAsyncTaskExecutor();
    }

}
