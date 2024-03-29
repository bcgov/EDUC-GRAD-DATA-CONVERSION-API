package ca.bc.gov.educ.api.dataconversion.messaging.jetstream;

import ca.bc.gov.educ.api.dataconversion.constant.Topics;
import ca.bc.gov.educ.api.dataconversion.model.ChoreographedEvent;
import ca.bc.gov.educ.api.dataconversion.service.EventHandlerDelegatorService;
import ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiConstants;
import ca.bc.gov.educ.api.dataconversion.util.JsonUtil;
import ca.bc.gov.educ.api.dataconversion.util.LogHelper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import io.nats.client.Message;
import io.nats.client.PushSubscribeOptions;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.DeliverPolicy;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jboss.threads.EnhancedQueueExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;


/**
 * The type Subscriber.
 */
@Component
@Slf4j
public class Subscriber {

  private final Executor subscriberExecutor;
  private final EventHandlerDelegatorService eventHandlerDelegatorService;
  private final Map<String, List<String>> streamTopicsMap = new HashMap<>(); // one stream can have multiple topics.
  private final Connection natsConnection;
  private final EducGradDataConversionApiConstants constants;

  /**
   * Instantiates a new Subscriber.
   *
   * @param natsConnection               the nats connection
   * @param eventHandlerDelegatorService the event handler delegator service
   */
  @Autowired
  public Subscriber(final Connection natsConnection, final EventHandlerDelegatorService eventHandlerDelegatorService, final EducGradDataConversionApiConstants constants) {
    this.eventHandlerDelegatorService = eventHandlerDelegatorService;
    this.natsConnection = natsConnection;
    this.constants = constants;
    this.subscriberExecutor = new EnhancedQueueExecutor.Builder()
            .setThreadFactory(new ThreadFactoryBuilder().setNameFormat("jet-stream-subscriber-%d").build())
            .setCorePoolSize(10).setMaximumPoolSize(10).setKeepAliveTime(Duration.ofSeconds(60)).build();
    this.initializeStreamTopicMap();
  }

  /**
   * this is the source of truth for all the topics this api subscribes to.
   */
  private void initializeStreamTopicMap() {
    final List<String> traxStatusEventsTopics = new ArrayList<>();
    traxStatusEventsTopics.add(Topics.TRAX_UPDATE_EVENT_TOPIC.name());
    this.streamTopicsMap.put(EducGradDataConversionApiConstants.TRAX_STREAM_NAME, traxStatusEventsTopics);
  }

  @PostConstruct
  public void subscribe() throws IOException, JetStreamApiException {
    val qName = EducGradDataConversionApiConstants.API_NAME.concat("-QUEUE");
    val autoAck = false;
    for (val entry : this.streamTopicsMap.entrySet()) {
      for (val topic : entry.getValue()) {
        final PushSubscribeOptions options = PushSubscribeOptions.builder().stream(entry.getKey())
            .durable(EducGradDataConversionApiConstants.API_NAME.concat("-DURABLE"))
            .configuration(ConsumerConfiguration.builder().deliverPolicy(DeliverPolicy.New).build()).build();
        this.natsConnection.jetStream().subscribe(topic, qName, this.natsConnection.createDispatcher(), this::onMessage,
            autoAck, options);
      }
    }
  }


  /**
   * This method will process the event message pushed into different topics of different APIS.
   * All APIs publish ChoreographedEvent
   *
   * @param message the string representation of {@link ChoreographedEvent} if it not type of event then it will throw exception and will be ignored.
   */
  public void onMessage(final Message message) {
    if (message != null) {
      log.info("Received message Subject:: {} , SID :: {} , sequence :: {}, pending :: {} ", message.getSubject(), message.getSID(), message.metaData().consumerSequence(), message.metaData().pendingCount());
      try {
        val eventString = new String(message.getData());
        LogHelper.logMessagingEventDetails(eventString, constants.isSplunkLogHelperEnabled());
        final ChoreographedEvent event = JsonUtil.getJsonObjectFromString(ChoreographedEvent.class, eventString);
        if (event.getEventPayload() == null) {
          message.ack();
          log.warn("payload is null, ignoring event :: {}", event);
          return;
        }
        this.subscriberExecutor.execute(() -> {
          try {
            this.eventHandlerDelegatorService.handleChoreographyEvent(event, message);
          } catch (final IOException e) {
            log.error("IOException ", e);
          }
        });
        log.debug("received event :: {} ", event);
      } catch (final Exception ex) {
        log.error("Exception ", ex);
      }
    }
  }

}
