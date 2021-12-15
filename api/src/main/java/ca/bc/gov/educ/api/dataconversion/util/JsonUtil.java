package ca.bc.gov.educ.api.dataconversion.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * The type Json util.
 */
public class JsonUtil {
  public static final ObjectMapper mapper = new ObjectMapper();

  private JsonUtil() {
  }

  /**
   * Gets json string from object.
   *
   * @param payload the payload
   * @return the json string from object
   * @throws JsonProcessingException the json processing exception
   */
  public static String getJsonStringFromObject(Object payload) throws JsonProcessingException {
    return mapper.writeValueAsString(payload);
  }

  /**
   * Gets json object from string.
   *
   * @param <T>     the type parameter
   * @param clazz   the clazz
   * @param payload the payload
   * @return the json object from string
   * @throws JsonProcessingException the json processing exception
   */
  public static <T> T getJsonObjectFromString(Class<T> clazz, String payload) throws JsonProcessingException {
    return mapper.readValue(payload, clazz);
  }

  /**
   * Get json bytes from object byte [ ].
   *
   * @param payload the payload
   * @return the byte [ ]
   * @throws JsonProcessingException the json processing exception
   */
  public static byte[] getJsonBytesFromObject(Object payload) throws JsonProcessingException {
    return mapper.writeValueAsBytes(payload);
  }

  /**
   * Get object from json byte [ ].
   *
   * @param <T>     the type parameter
   * @param clazz   the clazz
   * @param payload the byte [ ]
   * @return the json object from byte []
   * @throws JsonProcessingException the json processing exception
   */
  public static <T> T getObjectFromJsonBytes(Class<T> clazz, byte[] payload) throws IOException {
    return mapper.readValue(payload, clazz);
  }
}
