package ee.taltech.ite4120.animals;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ee.taltech.ite4120.animals.model.Animal;
import ee.taltech.ite4120.animals.service.AnimalService;
import java.time.LocalDate;
import org.helex.commons.exception.ApiClientException;
import org.helex.commons.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * THE BUSINESS TESTS — the executable form of the plain-language tests in
 * docs/specifications/ANIMALS.01 § Business tests. Written before the
 * implementation; watched fail; then made green. If a rule here feels arbitrary,
 * read the specification — the rule is quoted from it.
 *
 * <p>Runs against a real PostgreSQL via Testcontainers: the whole Liquibase
 * changelog (core + animals) is applied to a throwaway database, so these tests
 * also prove the migrations run clean from empty.
 */
@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class AnimalBusinessRulesIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

    @Autowired
    AnimalService service;

    @Test
    void anAnimalCannotBeBornInTheFuture() {
        // Given a request for an animal born tomorrow
        Animal a = animal("IT-FUT-001");
        a.setBirthDate(LocalDate.now().plusDays(1));

        // When it is registered, then the registration is refused as a client error
        assertThatThrownBy(() -> service.create(a))
                .isInstanceOf(ApiClientException.class)
                .hasMessageContaining("birthDate must not be in the future");
    }

    @Test
    void aRegistryCodeIsUniqueAmongActiveAnimals() {
        // Given an active animal with a registry code
        Animal first = service.create(animal("IT-DUP-001"));
        assertThat(first.getId()).isNotNull();

        // When a second animal claims the same code, then the registration conflicts
        assertThatThrownBy(() -> service.create(animal("IT-DUP-001")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");

        // And when the first is retired, the code becomes free again (soft delete!)
        service.retire(first.getId());
        Animal reused = service.create(animal("IT-DUP-001"));
        assertThat(reused.getId()).isNotEqualTo(first.getId());
    }

    private static Animal animal(String registryCode) {
        Animal a = new Animal();
        a.setRegistryCode(registryCode);
        a.setName("Test Animal");
        a.setSpeciesCode("DOG");
        a.setBirthDate(LocalDate.of(2022, 5, 1));
        return a;
    }
}
