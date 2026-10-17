package ee.taltech.ite4120.animals.ownerregistry;

import ee.taltech.ite4120.animals.ownerregistry.http.HttpOwnerRegistryAdapter;
import ee.taltech.ite4120.animals.ownerregistry.xroad.XRoadOwnerRegistryAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Chooses the transport behind {@link OwnerRegistryAdapter} from configuration.
 *
 * <ul>
 *   <li>{@code animals.owner-registry.mode=http} (default) — plain HTTPS+JSON against
 *       {@code animals.owner-registry.url}. Locally that URL points at this very
 *       application's built-in mock registry: an imitated environment.</li>
 *   <li>{@code animals.owner-registry.mode=xroad} — the forge-xroad transport against a
 *       real security server. A skeleton in this course; see XRoadOwnerRegistryAdapter.</li>
 * </ul>
 */
@Configuration
public class OwnerRegistryConfig {

    @Bean
    public OwnerRegistryAdapter ownerRegistryAdapter(
            @Value("${animals.owner-registry.mode:http}") String mode,
            @Value("${animals.owner-registry.url:http://localhost:18440/mock-registry}") String url,
            @Value("${animals.owner-registry.xroad.security-server-url:http://localhost:8080}") String ssUrl,
            @Value("${animals.owner-registry.xroad.instance:ee-test}") String instance,
            @Value("${animals.owner-registry.xroad.client-member-class:EDU}") String cMemberClass,
            @Value("${animals.owner-registry.xroad.client-member-code:taltech}") String cMemberCode,
            @Value("${animals.owner-registry.xroad.client-subsystem:ite4120}") String cSubsystem,
            @Value("${animals.owner-registry.xroad.service-member-class:GOV}") String sMemberClass,
            @Value("${animals.owner-registry.xroad.service-member-code:70008440}") String sMemberCode,
            @Value("${animals.owner-registry.xroad.service-subsystem:rr}") String sSubsystem) {
        if ("xroad".equalsIgnoreCase(mode)) {
            return new XRoadOwnerRegistryAdapter(ssUrl, instance,
                    cMemberClass, cMemberCode, cSubsystem, sMemberClass, sMemberCode, sSubsystem);
        }
        return new HttpOwnerRegistryAdapter(url);
    }
}
