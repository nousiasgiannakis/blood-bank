package ee.taltech.ite4120.config;

import static org.assertj.core.api.Assertions.assertThat;

import ee.taltech.ite4120.animals.model.Animal;
import ee.taltech.ite4120.animals.service.AnimalService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * The database records WHO wrote a row — {@link JdbcConfig} hands the signed-in
 * user to {@code core.set_user()} on every borrowed connection, and the platform
 * trigger stamps it into {@code sys_created_by} / {@code sys_modified_by}. Pinned
 * here because it is invisible when it silently stops working: every row would
 * simply say the application wrote it.
 */
@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class WriteAttributionIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

    @Autowired
    AnimalService animals;

    @AfterEach
    void signOut() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rowsRememberWhoCreatedAndWhoLastChangedThem() {
        signIn("alice");
        Animal created = animals.create(animal("IT-WHO-001"));
        assertThat(created.getSysCreatedBy()).isEqualTo("alice");
        assertThat(created.getSysModifiedBy()).isEqualTo("alice");

        signIn("bob");
        Animal update = animal("IT-WHO-001");
        update.setName("Renamed by Bob");
        Animal updated = animals.update(created.getId(), update);
        assertThat(updated.getSysCreatedBy()).as("creation is history — it keeps the first author").isEqualTo("alice");
        assertThat(updated.getSysModifiedBy()).isEqualTo("bob");
    }

    @Test
    void outsideARequestTheApplicationItselfIsTheAuthor() {
        SecurityContextHolder.clearContext();
        Animal created = animals.create(animal("IT-WHO-002"));
        assertThat(created.getSysCreatedBy()).isEqualTo(JdbcConfig.APPLICATION_USER);
    }

    private static void signIn(String user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    private static Animal animal(String registryCode) {
        Animal a = new Animal();
        a.setRegistryCode(registryCode);
        a.setName("Attributed animal");
        a.setSpeciesCode("CAT");
        a.setBirthDate(LocalDate.of(2020, 1, 1));
        return a;
    }
}
