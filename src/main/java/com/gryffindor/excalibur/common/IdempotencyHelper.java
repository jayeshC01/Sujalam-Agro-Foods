package com.gryffindor.excalibur.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gryffindor.excalibur.model.exception.IdempotencyPayloadMismatchException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class IdempotencyHelper {

  private static final Logger log = LoggerFactory.getLogger(IdempotencyHelper.class);
  private final ObjectMapper objectMapper;

  public IdempotencyHelper(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /**
   * Computes a deterministic SHA-256 hash string for any request payload object.
   *
   * @param payload Request DTO payload to hash
   * @return 64-character hexadecimal SHA-256 hash string
   */
  public String computeSha256(Object payload) {
    if (payload == null) {
      return "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    }
    try {
      String json = objectMapper.writeValueAsString(payload);
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (JsonProcessingException | NoSuchAlgorithmException e) {
      log.warn("Failed to compute SHA-256 payload hash, falling back to hashCode string", e);
      return Integer.toHexString(payload.hashCode());
    }
  }

  /**
   * Validates that an incoming request's hash matches the stored hash on record. Throws
   * IdempotencyPayloadMismatchException (422 Unprocessable Entity) if altered.
   *
   * @param incomingHash SHA-256 hash of the incoming request
   * @param storedHash SHA-256 hash stored on the existing database entity
   * @param idempotencyKey The idempotency key passed by the client
   */
  public void validatePayloadIntegrity(
      String incomingHash, String storedHash, String idempotencyKey) {
    if (storedHash != null && !storedHash.equals(incomingHash)) {
      log.warn("Idempotency payload tampering detected for key: {}", idempotencyKey);
      throw new IdempotencyPayloadMismatchException(
          "Idempotency-Key '"
              + idempotencyKey
              + "' was previously used with a different request payload.");
    }
  }
}
