// ITE4120 course template backend — the ANIMALS REGISTER worked example.
//
// This build consumes the Helex platform exclusively as published artifacts:
//   org.helex.emr:*   from maven.pkg.github.com/helex-solutions/emr-repo
//   org.helex.forge:* from maven.pkg.github.com/helex-solutions/forge
//
// Students: your own component lives in this same build — add a package under
// ee.taltech.ite4120.<yours> and a changelog under src/main/resources/<yours>/db.
// You should not need to touch this file except to read it.

plugins {
    java
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "ee.taltech.ite4120"
version = "0.1.0-SNAPSHOT"

java {
    toolchain { languageVersion = JavaLanguageVersion.of(25) }
}

val helexCommonsVersion: String by project
val forgeVersion: String by project

dependencies {
    // --- Spring Boot -----------------------------------------------------
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-liquibase")
    runtimeOnly("org.postgresql:postgresql")

    // --- Helex platform (published artifacts — see settings.gradle.kts) ---
    // commons-db: BaseRepository, SqlBuilder, PgBeanProcessor — the data-access idiom.
    implementation("org.helex.emr:commons-db:$helexCommonsVersion")
    // commons-db-core: the shared `core` schema as Liquibase resources — sequences
    // (core.seq_id), the sys-column trigger machinery, create_table_metadata().
    // Our master changelog includes it straight from this jar.
    implementation("org.helex.emr:commons-db-core:$helexCommonsVersion")
    // commons-model: QueryParams / QueryResult and the exception vocabulary
    // (NotFoundException, ConflictException) — the list-endpoint contract.
    implementation("org.helex.emr:commons-model:$helexCommonsVersion")
    // commons-util: JsonUtil and friends.
    implementation("org.helex.emr:commons-util:$helexCommonsVersion")

    // forge-xroad: the real X-Road transport (SOAP envelope + security-server HTTP).
    // The course uses the mock registry by default; this powers the `xroad` mode.
    implementation("org.helex.forge:forge-xroad:$forgeVersion")

    // --- OpenAPI / Swagger UI --------------------------------------------
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.1")

    // --- Tests ------------------------------------------------------------
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-postgresql:2.0.5")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter:2.0.5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

springBoot {
    mainClass = "ee.taltech.ite4120.Application"
}

tasks.bootJar {
    archiveFileName = "app.jar"
}
