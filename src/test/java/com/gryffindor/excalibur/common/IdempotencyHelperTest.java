package com.gryffindor.excalibur.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gryffindor.excalibur.model.exception.IdempotencyPayloadMismatchException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IdempotencyHelperTest {

  private IdempotencyHelper idempotencyHelper;

  @BeforeEach
  void setUp() {
    idempotencyHelper = new IdempotencyHelper(new ObjectMapper());
  }

  @Test
  @DisplayName("computeSha256 produces deterministic 64-char hex string for object payload")
  void computeSha256_returnsDeterministicHexHash() {
    Map<String, Object> payload = Map.of("productId", "p1", "qty", 2);

    String hash1 = idempotencyHelper.computeSha256(payload);
    String hash2 = idempotencyHelper.computeSha256(payload);

    assertThat(hash1).isNotNull();
    assertThat(hash1).hasSize(64);
    assertThat(hash1).isEqualTo(hash2);
  }

  @Test
  @DisplayName("computeSha256 returns standard empty-string hash when payload is null")
  void computeSha256_handlesNullPayload() {
    String hash = idempotencyHelper.computeSha256(null);

    assertThat(hash).isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
  }

  @Test
  @DisplayName("validatePayloadIntegrity succeeds when hashes match")
  void validatePayloadIntegrity_succeeds_whenHashesMatch() {
    idempotencyHelper.validatePayloadIntegrity("hash123", "hash123", "key-1");
    // No exception thrown
  }

  @Test
  @DisplayName(
      "validatePayloadIntegrity throws IdempotencyPayloadMismatchException when hashes differ")
  void validatePayloadIntegrity_throwsException_whenHashesMismatch() {
    assertThatThrownBy(
            () ->
                idempotencyHelper.validatePayloadIntegrity(
                    "new-hash-456", "original-hash-123", "key-1"))
        .isInstanceOf(IdempotencyPayloadMismatchException.class)
        .hasMessageContaining("previously used with a different request payload");
  }
}
