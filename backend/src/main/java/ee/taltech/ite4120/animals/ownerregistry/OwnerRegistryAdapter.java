package ee.taltech.ite4120.animals.ownerregistry;

/**
 * The port through which the application asks "who owns this animal?".
 *
 * <p>The rest of the application depends only on this interface. Which transport
 * answers — the built-in mock, a plain-HTTPS registry, or a real X-Road security
 * server — is chosen by configuration ({@code animals.owner-registry.mode}), not
 * by code. Compare {@code IxeAdapter} in the production EMR: same idea, larger.
 */
public interface OwnerRegistryAdapter {

    /**
     * Look up a person by Estonian personal code.
     *
     * @throws OwnerRegistryException when the registry cannot be reached or answers
     *         with a fault — the caller decides whether that fails open or closed.
     */
    OwnerInfo lookup(String isikukood);
}
