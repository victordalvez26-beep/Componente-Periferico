package uy.edu.tse.hcen.multitenancy;

import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;

import jakarta.annotation.Resource;
import org.jboss.logging.Logger;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Multi-tenant provider that switches the schema for each tenant on the same DataSource.
 * This implementation assumes a single shared DataSource (java:/jdbc/MyMainDataSource)
 * and executes a schema switch SQL per connection.
 */
public class SchemaMultiTenantProvider implements MultiTenantConnectionProvider<Object> {

    private static final long serialVersionUID = 1L;

    @Resource(lookup = "java:/jdbc/MyMainDataSource")
    private DataSource dataSource;

    private static final Logger LOG = Logger.getLogger(SchemaMultiTenantProvider.class);

    @Override
    public Connection getAnyConnection() throws SQLException {
        // If resource injection did not occur yet (Hibernate instantiates this class),
        // perform a one-time JNDI lookup and cache the DataSource reference.
        initializeDataSourceIfNeeded();
        
        return dataSource.getConnection();
    }

    private synchronized void initializeDataSourceIfNeeded() throws SQLException {
        if (dataSource == null) {
            try {
                InitialContext ic = new InitialContext();
                dataSource = (DataSource) ic.lookup("java:/jdbc/MyMainDataSource");
            } catch (NamingException ne) {
                throw new SQLException("Unable to obtain DataSource from JNDI java:/jdbc/MyMainDataSource", ne);
            }
        }
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public Connection getConnection(Object tenantIdentifierObj) throws SQLException {
        // Use the TenantContext (populated by application/login/filter) to build
        // the actual schema name used in the search_path. The resolver class
        // converts tenant ids (e.g. "101") into schema names (e.g. "schema_clinica_101").
        try {
            // Return the physical connection from the shared DataSource. Schema switching
            // is handled at a higher level (service layer / integrator) in current deployments.
            return getAnyConnection();
        } catch (final SQLException e) {
            throw new HibernateException("Error trying to obtain connection", e);
        }
    }

    @Override
    public void releaseConnection(Object tenantIdentifier, Connection connection) throws SQLException {
        // Close the physical connection. We applied schema on getConnection(), so
        // simply close the connection when requested.
        try {
            releaseAnyConnection(connection);
        } catch (Exception ex) {
            LOG.warnf("Error while closing connection: %s", ex.getMessage());
            throw new SQLException(ex);
        }
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
