package ee.taltech.ite4120.animals.dto;

import org.helex.commons.model.QueryParams;

/**
 * Query parameters for the animal list endpoint. Extends the platform
 * {@link QueryParams}, which carries {@code limit}, {@code offset} and {@code sort}
 * — the same list contract every Helex module uses.
 */
public class AnimalQueryParams extends QueryParams {

    /** Case-insensitive substring match on name or registry code. */
    private String textContains;
    /** Exact species filter, e.g. {@code DOG}. */
    private String speciesCode;

    public String getTextContains() { return textContains; }
    public void setTextContains(String textContains) { this.textContains = textContains; }
    public String getSpeciesCode() { return speciesCode; }
    public void setSpeciesCode(String speciesCode) { this.speciesCode = speciesCode; }
}
