package ca.bc.gov.educ.api.dataconversion.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public final class LogHelper {
  private static final ObjectMapper mapper = new ObjectMapper();
  private static final String EXCEPTION = "Exception ";

  private LogHelper() {

  }

  public static void logServerHttpReqResponseDetails(@NonNull final HttpServletRequest request, final HttpServletResponse response, final boolean logging) {
    if (!logging) return;
    try {
      final int status = response.getStatus();
      val totalTime = Instant.now().toEpochMilli() - (Long) request.getAttribute("startTime");
      final Map<String, Object> httpMap = new HashMap<>();
      httpMap.put("server_http_response_code", status);
      httpMap.put("server_http_request_method", request.getMethod());
      httpMap.put("server_http_query_params", request.getQueryString());
      val correlationID = request.getHeader(EducGradDataConversionApiConstants.CORRELATION_ID);
      if (correlationID != null) {
        httpMap.put("correlation_id", correlationID);
      }
      val requestSource = request.getHeader(EducGradDataConversionApiConstants.REQUEST_SOURCE);
      if (requestSource != null) {
        httpMap.put("request_source", requestSource);
      }
      httpMap.put("server_http_request_url", String.valueOf(request.getRequestURL()));
      httpMap.put("server_http_request_processing_time_ms", totalTime);
      httpMap.put("server_http_request_payload", String.valueOf(request.getAttribute("payload")));
      httpMap.put("server_http_request_remote_address", request.getRemoteAddr());
      MDC.putCloseable("httpEvent", mapper.writeValueAsString(httpMap));
      log.info("");
      MDC.clear();
    } catch (final Exception exception) {
      log.error(EXCEPTION, exception);
    }
  }

  public static void logClientHttpReqResponseDetails(@NonNull final HttpMethod method, final String url, final int responseCode, final List<String> correlationID, final List<String> requestSource, final boolean logging) {
    if (!logging) return;
    try {
      final Map<String, Object> httpMap = new HashMap<>();
      httpMap.put("client_http_response_code", responseCode);
      httpMap.put("client_http_request_method", method.toString());
      httpMap.put("client_http_request_url", url);
      if (correlationID != null) {
        httpMap.put("correlation_id", String.join(",", correlationID));
      }
      if (requestSource != null) {
        httpMap.put("request_source", String.join(",", requestSource));
      }
      MDC.putCloseable("httpEvent", mapper.writeValueAsString(httpMap));
      log.info("");
      MDC.clear();
    } catch (final Exception exception) {
      log.error(EXCEPTION, exception);
    }
  }

  /**
   * NATS messaging
   * the event is a json string.
   *
   * @param event the json string
   */
  public static void logMessagingEventDetails(final String event, final boolean logging) {
    if (!logging) return;
    try {
      MDC.putCloseable("messageEvent", event);
      log.debug("");
      MDC.clear();
    } catch (final Exception exception) {
      log.error(EXCEPTION, exception);
    }
  }

}
