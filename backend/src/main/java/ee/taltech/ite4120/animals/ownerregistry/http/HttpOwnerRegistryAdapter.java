package ee.taltech.ite4120.animals.ownerregistry.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.taltech.ite4120.animals.ownerregistry.OwnerInfo;
import ee.taltech.ite4120.animals.ownerregistry.OwnerRegistryAdapter;
import ee.taltech.ite4120.animals.ownerregistry.OwnerRegistryException;
import ee.taltech.ite4120.animals.ownerregistry.http.wire.RrPersonResponse;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Layer A + the mapper: fetches the registry's answer over plain HTTPS+JSON and
 * translates the wire record into our domain {@link OwnerInfo}.
 *
 * <p>In the default (local) configuration the URL points back at this very
 * application's {@code /mock-registry} endpoint — an imitated environment, which
 * is exactly what the assessment criteria permit. Point it at a real registry by
 * changing {@code animals.owner-registry.url}; nothing else changes.
 */
public class HttpOwnerRegistryAdapter implements OwnerRegistryAdapter {

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper json = new ObjectMapper();
    private final String baseUrl;

    public HttpOwnerRegistryAdapter(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @Override
    public OwnerInfo lookup(String isikukood) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/persons/" + isikukood))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404) {
                // A person the registry does not know is a MISS, not a failure.
                return null;
            }
            if (response.statusCode() != 200) {
                throw new OwnerRegistryException(
                        "owner registry answered HTTP " + response.statusCode());
            }
            RrPersonResponse wire = json.readValue(response.body(), RrPersonResponse.class);
            // The mapper — the ONLY place where Estonian wire names become our names.
            return new OwnerInfo(wire.isikukood(), wire.eesnimi(), wire.perekonnanimi(), wire.aadress());
        } catch (IOException e) {
            throw new OwnerRegistryException("owner registry unreachable: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OwnerRegistryException("owner registry call interrupted", e);
        }
    }
}
