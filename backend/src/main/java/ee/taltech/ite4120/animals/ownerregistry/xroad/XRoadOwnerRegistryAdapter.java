package ee.taltech.ite4120.animals.ownerregistry.xroad;

import ee.taltech.ite4120.animals.ownerregistry.OwnerInfo;
import ee.taltech.ite4120.animals.ownerregistry.OwnerRegistryAdapter;
import ee.taltech.ite4120.animals.ownerregistry.OwnerRegistryException;
import ee.taltech.ite4120.animals.ownerregistry.xroad.wire.RrIsikRequest;
import ee.taltech.ite4120.animals.ownerregistry.xroad.wire.RrIsikResponse;
import java.net.URI;
import java.time.Duration;
import org.helex.forge.xroad.XRoadClient;
import org.helex.forge.xroad.XRoadClientConfig;
import org.helex.forge.xroad.XRoadClientId;
import org.helex.forge.xroad.XRoadException;
import org.helex.forge.xroad.XRoadResponse;
import org.helex.forge.xroad.XRoadServiceId;

/**
 * The REAL X-Road transport, on the published {@code org.helex.forge:forge-xroad}
 * library — the same one the production EMR adapters (KIRST, Rahvastikuregister,
 * Ravimiregister) are built on.
 *
 * <p>This is a SKELETON by design: it compiles, builds a correct envelope, and
 * fails fast with a clear error when no security server is reachable — which in
 * this course there is not. It exists so you can see that switching the demo from
 * the imitated registry to the real one is a change of configuration and wire
 * records, not of application code: the controller and service never know.
 *
 * <p>Activated by {@code animals.owner-registry.mode=xroad}; the identifiers come
 * from {@code animals.owner-registry.xroad.*} in application.yml.
 */
public class XRoadOwnerRegistryAdapter implements OwnerRegistryAdapter {

    private final XRoadClient client;
    private final XRoadServiceId service;

    public XRoadOwnerRegistryAdapter(String securityServerUrl, String instance,
            String clientMemberClass, String clientMemberCode, String clientSubsystem,
            String serviceMemberClass, String serviceMemberCode, String serviceSubsystem) {
        XRoadClientId clientId =
                new XRoadClientId(instance, clientMemberClass, clientMemberCode, clientSubsystem);
        XRoadClientConfig config = new XRoadClientConfig(
                URI.create(securityServerUrl), clientId, "ITE4120", Duration.ofSeconds(30));
        this.client = new XRoadClient(config);
        // EE/GOV/70008440/rr/isikuParing/v1 — a teaching stand-in shaped like the real thing.
        this.service = new XRoadServiceId(instance, serviceMemberClass, serviceMemberCode,
                serviceSubsystem, "isikuParing", "v1");
    }

    @Override
    public OwnerInfo lookup(String isikukood) {
        try {
            XRoadResponse<RrIsikResponse> response =
                    client.send(service, RrIsikRequest.of(isikukood), RrIsikResponse.class);
            RrIsikResponse wire = response.body();
            if (wire == null || wire.isikukood() == null) {
                return null; // registry does not know this person — a miss, not a failure
            }
            // The mapper: wire vocabulary → domain vocabulary, in one place.
            return new OwnerInfo(wire.isikukood(), wire.eesnimi(), wire.perekonnanimi(), wire.aadress());
        } catch (XRoadException e) {
            throw new OwnerRegistryException("X-Road call failed: " + e.getMessage(), e);
        }
    }
}
