package ee.taltech.ite4120.animals.model;

/** Reference row from {@code animals.species} — the lookup table of 01-species.sql. */
public class Species {

    private String code;
    private String name;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
