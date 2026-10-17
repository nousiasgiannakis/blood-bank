package ee.taltech.ite4120.animals.service;

import ee.taltech.ite4120.animals.dto.AnimalQueryParams;
import ee.taltech.ite4120.animals.model.Animal;
import ee.taltech.ite4120.animals.model.Species;
import ee.taltech.ite4120.animals.repository.AnimalRepository;
import java.time.LocalDate;
import java.util.List;
import org.helex.commons.exception.ApiClientException;
import org.helex.commons.exception.ConflictException;
import org.helex.commons.exception.NotFoundException;
import org.helex.commons.model.QueryResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business rules for the animals register. The rules here are exactly the ones
 * written as business tests in docs/specifications/ANIMALS.01 — spec first,
 * tests second, implementation third.
 */
@Service
public class AnimalService {

    private final AnimalRepository repository;

    public AnimalService(AnimalRepository repository) {
        this.repository = repository;
    }

    public QueryResult<Animal> search(AnimalQueryParams params) {
        return repository.search(params);
    }

    public Animal require(Long id) {
        Animal a = repository.load(id);
        if (a == null) {
            throw new NotFoundException("animal " + id + " not found");
        }
        return a;
    }

    @Transactional
    public Animal create(Animal animal) {
        validate(animal);
        // Business rule: registry code is unique among active animals.
        if (repository.loadByRegistryCode(animal.getRegistryCode()) != null) {
            throw new ConflictException("animal",
                    "an active animal with registry code " + animal.getRegistryCode() + " already exists");
        }
        Long id = repository.insert(animal);
        return require(id);
    }

    @Transactional
    public Animal update(Long id, Animal animal) {
        Animal existing = require(id);
        // Registry code is immutable after creation — identity does not change.
        animal.setRegistryCode(existing.getRegistryCode());
        validate(animal);
        animal.setId(id);
        if (!repository.update(animal)) {
            throw new NotFoundException("animal " + id + " not found");
        }
        return require(id);
    }

    @Transactional
    public void retire(Long id) {
        if (!repository.retire(id)) {
            throw new NotFoundException("animal " + id + " not found");
        }
    }

    public List<Species> species() {
        return repository.speciesList();
    }

    private void validate(Animal a) {
        // Business rule: an animal cannot be born in the future.
        if (a.getBirthDate() != null && a.getBirthDate().isAfter(LocalDate.now())) {
            throw new ApiClientException("birthDate must not be in the future");
        }
        // Referential rule surfaced as a clear 400 rather than a raw FK violation.
        if (!repository.speciesExists(a.getSpeciesCode())) {
            throw new ApiClientException("unknown speciesCode: " + a.getSpeciesCode());
        }
    }
}
