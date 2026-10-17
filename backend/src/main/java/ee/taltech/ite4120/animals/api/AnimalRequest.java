package ee.taltech.ite4120.animals.api;

import ee.taltech.ite4120.animals.model.Animal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * The create/update request body. Bean Validation handles the *shape* of the
 * message here at the edge; the *state-dependent* rules (unique registry code,
 * birth date vs today) live in AnimalService — validation in more than one
 * place, on purpose. See the S3 "validation lives in three places" slide.
 */
public class AnimalRequest {

    @NotBlank
    @Size(max = 50)
    private String registryCode;

    @NotBlank
    @Size(max = 255)
    private String name;

    @NotBlank
    private String speciesCode;

    private LocalDate birthDate;

    /** 11 digits — and a string, never a number. */
    @Pattern(regexp = "\\d{11}", message = "ownerIsikukood must be exactly 11 digits")
    private String ownerIsikukood;

    @Size(max = 30)
    private String chipNumber;

    public Animal toModel() {
        Animal a = new Animal();
        a.setRegistryCode(registryCode);
        a.setName(name);
        a.setSpeciesCode(speciesCode);
        a.setBirthDate(birthDate);
        a.setOwnerIsikukood(ownerIsikukood);
        a.setChipNumber(chipNumber);
        return a;
    }

    public String getRegistryCode() { return registryCode; }
    public void setRegistryCode(String registryCode) { this.registryCode = registryCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSpeciesCode() { return speciesCode; }
    public void setSpeciesCode(String speciesCode) { this.speciesCode = speciesCode; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public String getOwnerIsikukood() { return ownerIsikukood; }
    public void setOwnerIsikukood(String ownerIsikukood) { this.ownerIsikukood = ownerIsikukood; }
    public String getChipNumber() { return chipNumber; }
    public void setChipNumber(String chipNumber) { this.chipNumber = chipNumber; }
}
