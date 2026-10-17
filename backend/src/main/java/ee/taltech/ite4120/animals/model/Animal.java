package ee.taltech.ite4120.animals.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * One registered animal. Maps 1:1 onto {@code animals.animal} — see the Liquibase
 * changesets under {@code resources/animals/db/changelog/animals/}.
 *
 * <p>The {@code sys*} fields are filled by the shared {@code core.sys_columns()}
 * trigger (from the published commons-db-core jar), never by application code.
 */
public class Animal {

    private Long id;
    /** Registry code, e.g. EE-2026-0001. Unique among active rows — see 02-animal.sql. */
    private String registryCode;
    private String name;
    /** FK → animals.species(code). */
    private String speciesCode;
    private LocalDate birthDate;
    /** Estonian personal code of the owner. A STRING, never a number — see the S3 slide. */
    private String ownerIsikukood;
    /** Microchip number; added later in changeset 04 — schema evolution, demonstrated. */
    private String chipNumber;

    private String sysStatus;
    private Long sysVersion;
    private OffsetDateTime sysCreatedAt;
    private String sysCreatedBy;
    private OffsetDateTime sysModifiedAt;
    private String sysModifiedBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public String getSysStatus() { return sysStatus; }
    public void setSysStatus(String sysStatus) { this.sysStatus = sysStatus; }
    public Long getSysVersion() { return sysVersion; }
    public void setSysVersion(Long sysVersion) { this.sysVersion = sysVersion; }
    public OffsetDateTime getSysCreatedAt() { return sysCreatedAt; }
    public void setSysCreatedAt(OffsetDateTime sysCreatedAt) { this.sysCreatedAt = sysCreatedAt; }
    public OffsetDateTime getSysModifiedAt() { return sysModifiedAt; }
    public void setSysModifiedAt(OffsetDateTime sysModifiedAt) { this.sysModifiedAt = sysModifiedAt; }
    public String getSysCreatedBy() { return sysCreatedBy; }
    public void setSysCreatedBy(String sysCreatedBy) { this.sysCreatedBy = sysCreatedBy; }
    public String getSysModifiedBy() { return sysModifiedBy; }
    public void setSysModifiedBy(String sysModifiedBy) { this.sysModifiedBy = sysModifiedBy; }
}
