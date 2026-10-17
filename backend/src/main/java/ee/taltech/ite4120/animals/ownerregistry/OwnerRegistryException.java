package ee.taltech.ite4120.animals.ownerregistry;

/** The registry could not answer — transport failure or upstream fault. Maps to 502. */
public class OwnerRegistryException extends RuntimeException {

    public OwnerRegistryException(String message) {
        super(message);
    }

    public OwnerRegistryException(String message, Throwable cause) {
        super(message, cause);
    }
}
