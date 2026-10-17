package ee.taltech.ite4120.animals.ownerregistry.mock;

import ee.taltech.ite4120.animals.ownerregistry.http.wire.RrPersonResponse;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The imitated population registry — canned data served by the application
 * itself, so the whole class works offline. Enabled only when
 * {@code animals.owner-registry.mock-server=true} (the local profile).
 *
 * <p>It answers in the REGISTRY's vocabulary (the wire record), not ours —
 * so the adapter exercises the same mapping it would against the real thing.
 */
@RestController
@RequestMapping("/mock-registry")
@ConditionalOnProperty(name = "animals.owner-registry.mock-server", havingValue = "true")
public class MockOwnerRegistryController {

    private static final Map<String, RrPersonResponse> PERSONS = Map.of(
            "38102130265", new RrPersonResponse("38102130265", "Jaan", "Tamm", "Ehitajate tee 5, Tallinn"),
            "47503121234", new RrPersonResponse("47503121234", "Mari", "Kask", "Raekoja plats 1, Tartu"),
            "50011020017", new RrPersonResponse("50011020017", "Peeter", "Sepp", "Pikk 12, Pärnu"));

    @GetMapping("/persons/{isikukood}")
    public ResponseEntity<RrPersonResponse> person(@PathVariable String isikukood) {
        RrPersonResponse person = PERSONS.get(isikukood);
        return person == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(person);
    }
}
