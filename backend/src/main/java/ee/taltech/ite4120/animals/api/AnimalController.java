package ee.taltech.ite4120.animals.api;

import ee.taltech.ite4120.animals.dto.AnimalQueryParams;
import ee.taltech.ite4120.animals.model.Animal;
import ee.taltech.ite4120.animals.model.Species;
import ee.taltech.ite4120.animals.ownerregistry.OwnerInfo;
import ee.taltech.ite4120.animals.ownerregistry.OwnerRegistryAdapter;
import ee.taltech.ite4120.animals.service.AnimalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.helex.commons.model.QueryResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The animals REST API. Nouns in the path, verbs in the method; 201 + Location on
 * create; the platform QueryResult on lists — exactly the S3 material, realized.
 */
@RestController
@RequestMapping("/api/animals")
@Tag(name = "Animals", description = "The animals register — the course's worked example component")
public class AnimalController {

    private final AnimalService service;
    private final OwnerRegistryAdapter ownerRegistry;

    public AnimalController(AnimalService service, OwnerRegistryAdapter ownerRegistry) {
        this.service = service;
        this.ownerRegistry = ownerRegistry;
    }

    @Operation(summary = "Search animals — paginated, filterable, sortable")
    @GetMapping
    public QueryResult<Animal> search(@ModelAttribute AnimalQueryParams params) {
        return service.search(params);
    }

    @Operation(summary = "One animal by id")
    @ApiResponse(responseCode = "200", description = "The animal")
    @ApiResponse(responseCode = "404", description = "No active animal with that id")
    @GetMapping("/{id}")
    public Animal byId(@PathVariable Long id) {
        return service.require(id);
    }

    @Operation(summary = "Register a new animal")
    @ApiResponse(responseCode = "201", description = "Created — Location header points at the new animal")
    @ApiResponse(responseCode = "400", description = "Validation failed (future birth date, unknown species…)")
    @ApiResponse(responseCode = "409", description = "Registry code already in use by an active animal")
    @PostMapping
    public ResponseEntity<Animal> create(@Valid @RequestBody AnimalRequest body) {
        Animal saved = service.create(body.toModel());
        return ResponseEntity.created(URI.create("/api/animals/" + saved.getId())).body(saved);
    }

    @Operation(summary = "Update an animal (registry code is immutable)")
    @PutMapping("/{id}")
    public Animal update(@PathVariable Long id, @Valid @RequestBody AnimalRequest body) {
        return service.update(id, body.toModel());
    }

    @Operation(summary = "Retire an animal (soft delete)")
    @ApiResponse(responseCode = "204", description = "Retired")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> retire(@PathVariable Long id) {
        service.retire(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Owner of an animal, looked up from the (mock or X-Road) population registry",
            description = "Demonstrates the adapter pattern: the controller knows only the "
                    + "OwnerRegistryAdapter interface; whether the answer comes from the built-in "
                    + "mock, a plain-HTTP registry, or X-Road is configuration.")
    @GetMapping("/{id}/owner")
    public OwnerInfo owner(@PathVariable Long id) {
        Animal animal = service.require(id);
        return ownerRegistry.lookup(animal.getOwnerIsikukood());
    }

    @Operation(summary = "Species reference list")
    @GetMapping("/species")
    public List<Species> species() {
        return service.species();
    }
}
