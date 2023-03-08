package ca.bc.gov.educ.api.dataconversion.config;

import ca.bc.gov.educ.api.dataconversion.listener.*;
import ca.bc.gov.educ.api.dataconversion.model.CourseRestriction;
import ca.bc.gov.educ.api.dataconversion.model.GradCourse;
import ca.bc.gov.educ.api.dataconversion.processor.*;
import ca.bc.gov.educ.api.dataconversion.reader.*;

import ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiConstants;
import ca.bc.gov.educ.api.dataconversion.writer.*;
import io.netty.channel.ConnectTimeoutException;
import org.hibernate.exception.JDBCConnectionException;
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
import org.springframework.core.task.TaskExecutor;
import org.springframework.retry.RetryPolicy;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.PrematureCloseException;

import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;

@Configuration
@EnableBatchProcessing
public class BatchJobConfig {

    @Autowired
    JobRegistry jobRegistry;

    @Bean
    public ItemReader<CourseRestriction> courseRestrictionReader(RestUtils restUtils) {
        return new DataConversionCourseRestrictionReader(restUtils);
    }

    @Bean
    public ItemReader<GradCourse> courseRequirementReader(RestUtils restUtils) {
        return new DataConversionCourseRequirementReader(restUtils);
    }

    @Bean
    public ItemWriter<CourseRestriction> courseRestrictionWriter() {
        return new DataConversionCourseRestrictionWriter();
    }

    @Bean
    public ItemWriter<GradCourse> courseRequirementWriter() {
        return new DataConversionCourseRequirementWriter();
    }

    @Bean
    public ItemProcessor<CourseRestriction, CourseRestriction> courseRestrictionProcessor() {
        return new DataConversionCourseRestrictionProcessor();
    }

    @Bean
    public ItemProcessor<GradCourse,GradCourse> courseRequirementProcessor() {
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

    /**
     * Creates a bean that represents the only steps of our batch job.
     */
    @Bean
    public Step courseRestrictionDataConversionJobStep(ItemReader<CourseRestriction> courseRestrictionReader,
                                                    ItemProcessor<? super CourseRestriction, ? extends CourseRestriction> courseRestrictionProcessor,
                                                    ItemWriter<CourseRestriction> courseRestrictionWriter,
                                                    StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("courseRestrictionDataConversionJobStep")
                .<CourseRestriction, CourseRestriction>chunk(1)
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
    public Step courseRequirementDataConversionJobStep(ItemReader<GradCourse> courseRequirementReader,
                                                       ItemProcessor<? super GradCourse, ? extends GradCourse> courseRequirementProcessor,
                                                       ItemWriter<GradCourse> courseRequirementWriter,
                                                       StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("courseRequirementDataConversionJobStep")
                .<GradCourse, GradCourse>chunk(1)
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

    // Partitioning for pen updates ---------------------------------------------------------------------------
    @Bean
    public Step masterStepForPenUpdates(StepBuilderFactory stepBuilderFactory, RestUtils restUtils, EducGradDataConversionApiConstants constants) {
        return stepBuilderFactory.get("masterStepForPenUpdates")
                .partitioner(slaveStepForPenUpdates(stepBuilderFactory).getName(), partitioner(restUtils))
                .step(slaveStepForPenUpdates(stepBuilderFactory))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor(constants))
                .build();
    }

    @Bean
    public StudentLoadPartitioner partitioner(RestUtils restUtils) {
        // Reader to feed input data for each partition
        return new StudentLoadPartitioner(restUtils);
    }

    @Bean
    public Step slaveStepForPenUpdates(StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("slaveStepForPenUpdates")
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
                             RestUtils restUtils,
                             EducGradDataConversionApiConstants constants,
                             PenUpdatesJobCompletionNotificationListener listener) {
        return jobBuilderFactory.get("penUpdatesJob")
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(masterStepForPenUpdates(stepBuilderFactory, restUtils, constants))
                .end()
                .build();
    }

    // Partitioning for student load ---------------------------------------------------------------------------
    @Bean
    public Step masterStepForStudent(StepBuilderFactory stepBuilderFactory, RestUtils restUtils, EducGradDataConversionApiConstants constants) {
        return stepBuilderFactory.get("masterStepForStudent")
                .partitioner(slaveStepForStudent(stepBuilderFactory, restUtils).getName(), partitioner(restUtils))
                .step(slaveStepForStudent(stepBuilderFactory, restUtils))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor(constants))
                .build();
    }

    @Bean
    public Step slaveStepForStudent(StepBuilderFactory stepBuilderFactory, RestUtils restUtils) {
        return stepBuilderFactory.get("slaveStepForStudent")
                .<String, ConvGradStudent>chunk(1)
                .reader(studentPartitionReader(restUtils))
                .processor(studentPartitionProcessor())
                .writer(studentPartitionWriter())
                .faultTolerant()
                .retryLimit(3)
                .retry(TransactionSystemException.class)
                .retry(PrematureCloseException.class)
                .retry(WebClientResponseException.class)
                .retry(ConnectTimeoutException.class)
                .retry(SQLTransientConnectionException.class)
                .retry(JDBCConnectionException.class)
                .retry(SQLException.class)
                .build();
    }

    @Bean
    @StepScope
    public StudentPartitionReader studentPartitionReader(RestUtils restUtils) {
        return new StudentPartitionReader(restUtils);
    }

    @Bean
    @StepScope
    public StudentPartitionProcessor studentPartitionProcessor() {
        return new StudentPartitionProcessor();
    }

    @Bean
    @StepScope
    public StudentPartitionWriter studentPartitionWriter() {
        return new StudentPartitionWriter();
    }

    @Bean
    public Job studentLoadJob(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory,
                              RestUtils restUtils,
                              EducGradDataConversionApiConstants constants,
                              StudentDataConversionJobCompletionNotificationListener listener) {
        return jobBuilderFactory.get("studentLoadJob")
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(masterStepForStudent(stepBuilderFactory, restUtils,constants))
                .end()
                .build();
    }

    @Bean
    public TaskExecutor taskExecutor(EducGradDataConversionApiConstants constants) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(constants.getNumberOfPartitions());
        executor.setMaxPoolSize(constants.getNumberOfPartitions());
        executor.setThreadNamePrefix("partition-");
        executor.initialize();

        return executor;
    }

}
