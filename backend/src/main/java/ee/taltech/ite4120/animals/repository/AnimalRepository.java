package ee.taltech.ite4120.animals.repository;

import ee.taltech.ite4120.animals.dto.AnimalQueryParams;
import ee.taltech.ite4120.animals.model.Animal;
import ee.taltech.ite4120.animals.model.Species;
import java.util.List;
import java.util.Map;
import org.helex.commons.db.bean.PgBeanProcessor;
import org.helex.commons.db.repo.BaseRepository;
import org.helex.commons.db.sql.SqlBuilder;
import org.helex.commons.exception.ApiClientException;
import org.helex.commons.model.QueryResult;
import org.springframework.stereotype.Repository;

/**
 * Data access for the animals register — the canonical Helex repository shape:
 * extend {@link BaseRepository}, map rows with {@link PgBeanProcessor}, and build
 * dynamic filters with {@link SqlBuilder}. Same idiom as every production module.
 */
@Repository
public class AnimalRepository extends BaseRepository {

    private static final String SELECT = "select a.* from animals.animal a ";

    /** The sort keys the list endpoint accepts (`sort=name`, `sort=-name`) and their columns. */
    private static final Map<String, String> SORT = Map.of(
            "name", "a.name",
            "registryCode", "a.registry_code",
            "birthDate", "a.birth_date");

    private final PgBeanProcessor bp = new PgBeanProcessor(Animal.class);
    private final PgBeanProcessor speciesBp = new PgBeanProcessor(Species.class);

    public Animal load(Long id) {
        return getBean(SELECT + "where a.id = ? and a.sys_status = 'A'", bp, id);
    }

    public Animal loadByRegistryCode(String registryCode) {
        return getBean(SELECT + "where a.registry_code = ? and a.sys_status = 'A'", bp, registryCode);
    }

    public QueryResult<Animal> search(AnimalQueryParams p) {
        // The platform pagination pattern: one count function, one page function,
        // both driven by the same filter — see BaseRepository.query.
        return query(p, this::count, this::list);
    }

    private Integer count(AnimalQueryParams p) {
        SqlBuilder sb = new SqlBuilder("select count(1) from animals.animal a where a.sys_status = 'A'");
        sb.append(filter(p));
        return queryForObject(sb.getSql(), Integer.class, sb.getParams());
    }

    private List<Animal> list(AnimalQueryParams p) {
        SqlBuilder sb = new SqlBuilder(SELECT + "where a.sys_status = 'A'");
        sb.append(filter(p));
        sb.append(order(p));
        sb.append(limit(p));
        return getBeans(sb.getSql(), bp, sb.getParams());
    }

    /**
     * The platform's {@code order} throws a plain IllegalArgumentException for a sort
     * key it cannot map. Left alone that surfaces as a 500 — but a mistyped
     * {@code ?sort=} is the caller's fault, so it becomes a 400 with the allowed keys.
     */
    private SqlBuilder order(AnimalQueryParams p) {
        try {
            return order(p, SORT);
        } catch (IllegalArgumentException e) {
            throw new ApiClientException("unknown sort key; sortable: " + String.join(", ", SORT.keySet().stream().sorted().toList()));
        }
    }

    private SqlBuilder filter(AnimalQueryParams p) {
        SqlBuilder sb = new SqlBuilder();
        if (p.getTextContains() != null && !p.getTextContains().isBlank()) {
            String like = "%" + p.getTextContains().toLowerCase() + "%";
            sb.and("(lower(a.name) like ? or lower(a.registry_code) like ?)", like, like);
        }
        sb.appendIfNotNull("and a.species_code = ?", p.getSpeciesCode());
        return sb;
    }

    public Long insert(Animal a) {
        String sql = """
                insert into animals.animal (registry_code, name, species_code, birth_date,
                                            owner_isikukood, chip_number)
                values (?, ?, ?, ?, ?, ?)
                returning id""";
        return queryForObject(sql, Long.class,
                a.getRegistryCode(), a.getName(), a.getSpeciesCode(), a.getBirthDate(),
                a.getOwnerIsikukood(), a.getChipNumber());
    }

    public boolean update(Animal a) {
        String sql = """
                update animals.animal
                   set name = ?, species_code = ?, birth_date = ?, owner_isikukood = ?, chip_number = ?
                 where id = ? and sys_status = 'A'""";
        return jdbcTemplate.update(sql, a.getName(), a.getSpeciesCode(), a.getBirthDate(),
                a.getOwnerIsikukood(), a.getChipNumber(), a.getId()) == 1;
    }

    /** Soft delete — the platform never hard-deletes registry rows. */
    public boolean retire(Long id) {
        return jdbcTemplate.update(
                "update animals.animal set sys_status = 'C' where id = ? and sys_status = 'A'", id) == 1;
    }

    public List<Species> speciesList() {
        return getBeans("select s.* from animals.species s order by s.name", speciesBp);
    }

    public boolean speciesExists(String code) {
        Integer n = queryForObject("select count(1) from animals.species where code = ?", Integer.class, code);
        return n != null && n > 0;
    }
}
