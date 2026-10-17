package ee.taltech.ite4120.animals.ownerregistry.http.wire;

/**
 * WIRE RECORD — the registry's response exactly as the registry defines it.
 *
 * <p>Field names are the provider's Estonian names, deliberately NOT translated:
 * a wire record must stay diffable against the provider's published schema. When
 * they change {@code eesnimi}, you must be able to grep for {@code eesnimi}.
 * The English renaming happens in exactly one place — the mapper in
 * {@link ee.taltech.ite4120.animals.ownerregistry.http.HttpOwnerRegistryAdapter}.
 */
public record RrPersonResponse(
        String isikukood,
        String eesnimi,
        String perekonnanimi,
        String aadress) {}
