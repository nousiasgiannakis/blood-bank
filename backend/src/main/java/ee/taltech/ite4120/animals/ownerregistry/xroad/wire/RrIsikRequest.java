package ee.taltech.ite4120.animals.ownerregistry.xroad.wire;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * WIRE RECORD — the X-Road request body for the (illustrative) population-registry
 * person query, in the provider's namespace and vocabulary. Modelled on the real
 * KIRST/RR adapters in production: element names match the provider's schema
 * verbatim; no English renaming here.
 *
 * <p>The service identifiers and this schema are TEACHING STAND-INS — before
 * calling a real service, replace them with the provider's published contract.
 */
@JacksonXmlRootElement(localName = "isikuParing", namespace = RrIsikRequest.NS)
public record RrIsikRequest(
        @JacksonXmlProperty(localName = "request", namespace = NS) Request request) {

    public static final String NS = "http://rr.x-road.eu";

    public record Request(
            @JacksonXmlProperty(localName = "isikukood", namespace = NS) String isikukood) {}

    public static RrIsikRequest of(String isikukood) {
        return new RrIsikRequest(new Request(isikukood));
    }
}
