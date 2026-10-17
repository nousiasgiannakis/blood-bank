package ee.taltech.ite4120.config;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Tells the database who is writing — the platform's {@code JdbcFactory} pattern,
 * at course scale.
 *
 * <p>The trigger {@code core.sys_columns()} fills {@code sys_created_by} and
 * {@code sys_modified_by} from {@code core.session_user()}, which reads the session
 * setting {@code core.client_identifier}. Every connection borrowed from the pool
 * is therefore initialised with {@code core.set_user(?)}: the signed-in user inside
 * a request, the application's own name outside one (startup, tests, jobs).
 * Always overwritten — a pooled connection would otherwise carry the previous
 * borrower's identity into the next request.
 *
 * <p>Only the JdbcTemplate and the transaction manager see the wrapper, and they
 * share it, so a transaction's connection is the one the repositories use.
 * Liquibase keeps the raw pool, whose {@code connection-init-sql} sets the same
 * application default.
 */
@Configuration
public class JdbcConfig {

    /** Who the database sees outside a request — the same name connection-init-sql sets. */
    static final String APPLICATION_USER = "animals";

    @Bean
    @Primary
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(new SessionUserDataSource(dataSource));
    }

    @Bean
    public JdbcTransactionManager transactionManager(JdbcTemplate jdbcTemplate) {
        return new JdbcTransactionManager(jdbcTemplate.getDataSource());
    }

    /** The principal of the current request, or the application itself outside one. */
    static String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return APPLICATION_USER;
        }
        return auth.getName();
    }

    static class SessionUserDataSource extends DelegatingDataSource {

        SessionUserDataSource(DataSource delegate) {
            super(delegate);
        }

        @Override
        public Connection getConnection() throws SQLException {
            return initSessionUser(super.getConnection());
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return initSessionUser(super.getConnection(username, password));
        }

        private static Connection initSessionUser(Connection c) throws SQLException {
            try (PreparedStatement ps = c.prepareStatement("select core.set_user(?)")) {
                ps.setString(1, currentUser());
                ps.execute();
            }
            return c;
        }
    }
}
