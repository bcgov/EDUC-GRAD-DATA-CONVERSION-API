package ca.bc.gov.educ.api.dataconversion.config;

import ca.bc.gov.educ.api.dataconversion.service.conv.DataConversionService;
import ca.bc.gov.educ.api.dataconversion.service.student.StudentService;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.support.JobRegistryBeanPostProcessor;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ca.bc.gov.educ.api.dataconversion.listener.DataConversionJobCompletionNotificationListener;
import ca.bc.gov.educ.api.dataconversion.model.ConvGradStudent;
import ca.bc.gov.educ.api.dataconversion.processor.DataConversionStudentProcessor;
import ca.bc.gov.educ.api.dataconversion.reader.DataConversionStudentReader;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import ca.bc.gov.educ.api.dataconversion.writer.DataConversionStudentWriter;

@Configuration
public class BatchJobConfig {

    @Autowired
    JobRegistry jobRegistry;

    @Bean
    public ItemReader<ConvGradStudent> itemReader(DataConversionService dataConversionService, RestUtils restUtils) {
        return new DataConversionStudentReader(dataConversionService, restUtils);
    }

    @Bean
    public ItemWriter<ConvGradStudent> itemWriter() {
        return new DataConversionStudentWriter();
    }

    @Bean
    public ItemProcessor<ConvGradStudent,ConvGradStudent> itemProcessor() {
        return new DataConversionStudentProcessor();
    }

    /**
    * Creates a bean that represents the only step of our batch job.
    */
    @Bean
    public Step dataConversionJobStep(ItemReader<ConvGradStudent> dataConversionReader,
                                    ItemProcessor<? super ConvGradStudent, ? extends ConvGradStudent> dataConversionProcessor,
                                    ItemWriter<ConvGradStudent> dataConversionWriter,
                                    StepBuilderFactory stepBuilderFactory) {
    return stepBuilderFactory.get("dataConversionJobStep")
            .<ConvGradStudent, ConvGradStudent>chunk(1)
            .reader(dataConversionReader)
            .processor(dataConversionProcessor)
            .writer(dataConversionWriter)
            .build();
    }

    /**
    * Creates a bean that represents our batch job.
    */
    @Bean
    public Job dataConversionBatchJob(Step dataConversionJobStep, DataConversionJobCompletionNotificationListener listener,
                                    JobBuilderFactory jobBuilderFactory) {
    return jobBuilderFactory.get("dataConversionBatchJob")
            .incrementer(new RunIdIncrementer())
            .listener(listener)
            .flow(dataConversionJobStep)
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
