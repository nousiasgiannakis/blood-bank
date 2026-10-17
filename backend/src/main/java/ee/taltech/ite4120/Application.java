package ee.taltech.ite4120;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ITE4120 course template — one application, several components.
 *
 * <p>Lives in the ROOT package on purpose: Spring Boot scans the package of this
 * class and everything below it, so every component under
 * {@code ee.taltech.ite4120.<component>} — animals, library, yours — is found
 * without registering anything. (It used to sit inside {@code animals}, which
 * silently limited the scan to that one component.)
 */
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
