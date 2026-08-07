package com.gryffindor.excalibur;

import com.google.firebase.auth.FirebaseAuth;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

/** Stands in for FirebaseConfig under the "test" profile so tests don't need real Google ADC. */
@TestConfiguration(proxyBeanMethods = false)
@Profile("test")
class TestFirebaseConfig {

  @Bean
  FirebaseAuth firebaseAuth() {
    return Mockito.mock(FirebaseAuth.class);
  }
}
