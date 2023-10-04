package nz.co.solnet.database;

import org.apache.derby.jdbc.EmbeddedDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sql.DataSource;
import java.io.File;
import java.sql.*;

/**
 * This class is the DatabaseContext.
 * It is responsible for creating the database and returning the repositories.
 * It also stores the context of the database, which is at this time the database url.
 * It also provides a bunch of database utility methods.
 */
public class DatabaseContext {

    private static volatile DatabaseContext instance;

    private final Logger logger = LogManager.getLogger(DatabaseContext.class);

    private final String DATABASE_URL;

    private final String DATABASE_USERNAME;

    private final String DATABASE_PASSWORD;

    private final DataSource dataSource;

    /**
     * Private constructor to prevent instantiation.
     * @param databaseUrl
     * @param databaseUsername
     * @param databasePassword
     */
    private DatabaseContext(String databaseUrl, String databaseUsername, String databasePassword) {

        System.setProperty("derby.connection.pooling", "true");
        System.setProperty("derby.pool.initialSize", "1");
        System.setProperty("derby.pool.maxConnections", "10");

        DATABASE_URL = databaseUrl;
        DATABASE_USERNAME = databaseUsername;
        DATABASE_PASSWORD = databasePassword;
        createDatabase();
        EmbeddedDataSource dataSource = getEmbeddedDataSource();
        this.dataSource = dataSource;
        TaskRepositoryImpl taskRepository = new TaskRepositoryImpl(dataSource);
        taskRepository.initialiseTasksTable();
    }

    /**
     * Create the embedded data source. This is Derby's native connection pooling implementation.
     * @return
     */
    private EmbeddedDataSource getEmbeddedDataSource() {
        EmbeddedDataSource dataSource = new EmbeddedDataSource();
        dataSource.setDatabaseName(extractDatabaseNameFromDerbyJDBCUrl(DATABASE_URL));
        dataSource.setUser(DATABASE_USERNAME);
        dataSource.setPassword(DATABASE_PASSWORD);
        return dataSource;
    }

    /**
     * Create the database.
     */
    private void createDatabase() {
        try (Connection connection = DriverManager.getConnection(DATABASE_URL + ";create=true", DATABASE_USERNAME, DATABASE_PASSWORD)) {
            // Database created
        } catch (SQLException e) {
            logger.error("Error in creating database", e);
        }
    }

    private String extractDatabaseNameFromDerbyJDBCUrl(String jdbcUrl) {
        String databaseName = null;
        String[] parts = jdbcUrl.split(":");

        if (parts.length >= 3) {
            if (parts[1].equalsIgnoreCase("derby") && parts[2].equalsIgnoreCase("memory")) {
                // In-memory database
                if (parts.length >= 4) {
                    databaseName = parts[2] + ":" + parts[3];
                }
            } else {
                // Regular database
                databaseName = parts[2];
            }
        }
        return databaseName;
    }

    public static DatabaseContext getInstance(String databaseUrl, String databaseUsername, String databasePassword) {
        if (instance == null) {
            synchronized (DatabaseContext.class) {
                if (instance == null) {
                    instance = new DatabaseContext(databaseUrl, databaseUsername, databasePassword);
                }
            }
        }
        return instance;
    }

    public static DatabaseContext getInstance() {
        if (instance == null) {
            throw new IllegalStateException("DatabaseContext not initialised");
        }
        return instance;
    }

    /**
     * This method returns the TaskRepository.
     * This is the only way to get a TaskRepository.
     * @return
     */
    public TaskRepository getTaskRepository() {
        return new TaskRepositoryImpl(dataSource);
    }

    /**
     * Graceful database shutdown.
     */
    public void shutdown() {
        String shutdownURL = "jdbc:derby:;shutdown=true";
        try {
            DriverManager.getConnection(shutdownURL);
        } catch (SQLException e) {
            // Expecting SQLException with SQL state XJ015 to indicate successful shutdown
            if (!"XJ015".equals(e.getSQLState())) {
                logger.error("Error in shutting down database", e);
            } else {
                logger.info("Database shutdown complete");
            }
        } finally {
            instance = null;
        }
    }

    /**
     * Utility method to delete all database records.
     */
    void cleanDatabase() {
        TaskRepositoryImpl taskRepository = new TaskRepositoryImpl(dataSource);
        taskRepository.cleanTaskData();
    }

}
