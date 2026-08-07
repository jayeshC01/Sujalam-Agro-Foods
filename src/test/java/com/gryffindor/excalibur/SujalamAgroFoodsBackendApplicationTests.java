package com.gryffindor.excalibur;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@Import({TestcontainersConfiguration.class, TestFirebaseConfig.class})
@ActiveProfiles("test")
@SpringBootTest
@Disabled("Requires Docker/Testcontainers and Firebase credentials")
class SujalamAgroFoodsBackendApplicationTests {

  @Test
  void contextLoads() {}
}
