package ee.taltech.ite4120.animals.ownerregistry;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ee.taltech.ite4120.animals.ownerregistry.xroad.XRoadOwnerRegistryAdapter;
import org.junit.jupiter.api.Test;

/**
 * The forge-xroad skeleton is not expected to reach a security server in this
 * course — but it must fail FAST and CLEARLY when there is none, not hang or
 * throw something cryptic. That behaviour is the contract this test pins.
 */
class XRoadAdapterSkeletonTest {

    @Test
    void failsFastAndClearlyWithoutASecurityServer() {
        XRoadOwnerRegistryAdapter adapter = new XRoadOwnerRegistryAdapter(
                "http://127.0.0.1:9", // reserved discard port — nothing listens here
                "ee-test", "EDU", "taltech", "ite4120", "GOV", "70008440", "rr");

        assertThatThrownBy(() -> adapter.lookup("38102130265"))
                .isInstanceOf(OwnerRegistryException.class)
                .hasMessageContaining("X-Road call failed");
    }
}
