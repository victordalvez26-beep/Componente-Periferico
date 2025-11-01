package uy.edu.tse.hcen.multitenancy;

import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;

import jakarta.annotation.Resource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Multi-tenant provider that switches the schema for each tenant on the same DataSource.
 * This implementation assumes a single shared DataSource (java:/jdbc/MyMainDataSource)
 * and executes a schema switch SQL per connection.
 */
public class SchemaMultiTenantProvider implements MultiTenantConnectionProvider {

    private static final long serialVersionUID = 1L;

    @Resource(lookup = "java:/jdbc/MyMainDataSource")
    private DataSource dataSource;

    @Override
    public Connection getAnyConnection() throws SQLException {
        // If resource injection did not occur yet, try JNDI lookup as a fallback.
        if (dataSource == null) {
            try {
                InitialContext ic = new InitialContext();
                dataSource = (DataSource) ic.lookup("java:/jdbc/MyMainDataSource");
            } catch (NamingException ne) {
                throw new SQLException("Unable to obtain DataSource from JNDI java:/jdbc/MyMainDataSource", ne);
            }
        }
        return dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        try {
            final Connection connection = getAnyConnection();
            // Use PostgreSQL search_path to switch schema for the session
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("SET search_path TO " + tenantIdentifier);
            }
            return connection;
        } catch (final SQLException e) {
            throw new HibernateException("Error trying to alter the schema [" + tenantIdentifier + "]", e);
        }
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        releaseAnyConnection(connection);
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public boolean isUnwrappableAs(Class<?> unwrapType) {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> unwrapType) {
        return null;
    }
}
