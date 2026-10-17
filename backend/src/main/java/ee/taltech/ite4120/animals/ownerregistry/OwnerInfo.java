package ee.taltech.ite4120.animals.ownerregistry;

/**
 * An animal owner, in OUR domain vocabulary — English names, our shape.
 * The registry's own field names live in the wire records and never leak past
 * the mapper. That separation is the two-layer rule from session 3.
 */
public record OwnerInfo(String isikukood, String firstName, String lastName, String address) {}
