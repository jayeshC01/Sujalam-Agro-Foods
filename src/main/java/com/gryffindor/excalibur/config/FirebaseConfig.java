package com.gryffindor.excalibur.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import java.io.IOException;
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

  @Bean
  public FirebaseApp firebaseApp() throws IOException {
    if (!FirebaseApp.getApps().isEmpty()) {
      return FirebaseApp.getInstance();
    }
    FirebaseOptions options =
        FirebaseOptions.builder().setCredentials(GoogleCredentials.getApplicationDefault()).build();
    return FirebaseApp.initializeApp(options);
  }

  @Bean
  public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
    return FirebaseAuth.getInstance(firebaseApp);
  }
}
