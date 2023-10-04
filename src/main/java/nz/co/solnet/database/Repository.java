package nz.co.solnet.database;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sql.DataSource;
import java.sql.*;

/**
 * This class is the base class for all repositories.
 */
public class Repository {

    private final Logger logger = LogManager.getLogger(DatabaseContext.class);

    private final DataSource dataSource;

    public Repository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Connection getConnection() {
        try {
            Connection connection = dataSource.getConnection();
            return connection;
        } catch (SQLException e) {
            logger.error("Error in getting connection", e);
        }
        return null;
    }

    /**
     * Allows repositories to check if their table exists in the database so they can create it if it doesn't.
     * @param tableName
     * @return
     * @throws SQLException
     */
    protected boolean doesTableExist(String tableName) throws SQLException {

        try (Connection conn = getConnection()) {

            DatabaseMetaData meta = conn.getMetaData();
            ResultSet result = meta.getTables(null, null, tableName.toUpperCase(), null);
            return result.next();

        } catch (SQLException e) {
            logger.error("Error in checking if table exists", e);
        }
        return false;
    }
}
