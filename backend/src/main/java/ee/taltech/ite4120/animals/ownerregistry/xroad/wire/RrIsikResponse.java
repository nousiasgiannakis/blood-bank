package ee.taltech.ite4120.animals.ownerregistry.xroad.wire;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * WIRE RECORD — the provider's response, schema-faithful. Estonian element names
 * stay Estonian; the mapper in XRoadOwnerRegistryAdapter does the translating.
 */
@JacksonXmlRootElement(localName = "isikuParingResponse", namespace = RrIsikRequest.NS)
public record RrIsikResponse(
        @JacksonXmlProperty(localName = "isikukood", namespace = RrIsikRequest.NS) String isikukood,
        @JacksonXmlProperty(localName = "eesnimi", namespace = RrIsikRequest.NS) String eesnimi,
        @JacksonXmlProperty(localName = "perekonnanimi", namespace = RrIsikRequest.NS) String perekonnanimi,
        @JacksonXmlProperty(localName = "aadress", namespace = RrIsikRequest.NS) String aadress) {}
