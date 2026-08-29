package com.gryffindor.excalibur.config;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import java.io.IOException;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Credentials are resolved via Application Default Credentials: set the
 * GOOGLE_APPLICATION_CREDENTIALS environment variable to the path of the Firebase service account
 * JSON (never commit that file). On GCP/Cloud Run this is picked up automatically.
 *
 * <p>Disabled under the "test" profile - tests supply mock FirebaseApp/FirebaseAuth beans instead
 * so the suite can run without real Google credentials (see TestFirebaseConfig).
 */
@Configuration
@Profile("!test")
public class FirebaseConfig {

  private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

  @Bean
  public FirebaseApp firebaseApp() {
    if (!FirebaseApp.getApps().isEmpty()) {
      return FirebaseApp.getInstance();
    }
    try {
      FirebaseOptions options =
          FirebaseOptions.builder()
              .setCredentials(GoogleCredentials.getApplicationDefault())
              .build();
      return FirebaseApp.initializeApp(options);
    } catch (IOException e) {
      log.warn(
          "Google Application Default Credentials not found ({}). Initializing FirebaseApp in local/development mode.",
          e.getMessage());
      FirebaseOptions fallbackOptions =
          FirebaseOptions.builder()
              .setCredentials(
                  GoogleCredentials.create(
                      new AccessToken(
                          "mock-dev-token",
                          new Date(System.currentTimeMillis() + 365L * 24 * 3600 * 1000))))
              .setProjectId("sujalam-agro-dev")
              .build();
      return FirebaseApp.initializeApp(fallbackOptions);
    }
  }

  @Bean
  public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
    return FirebaseAuth.getInstance(firebaseApp);
  }
}
