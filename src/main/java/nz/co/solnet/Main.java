package nz.co.solnet;

import nz.co.solnet.database.DatabaseContext;
import nz.co.solnet.server.JettyServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.OutputStream;

public class Main {

    private static final String TASK_API_DATABASE_URL = "task.api.database.url";

    private static final Logger logger = LogManager.getLogger(Main.class);

    /**
     * Main entry point for the application.
     * Database url and port can be set via environment variables.
     * This makes them configurable when running in a container.
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        logger.info("---------------------------------------");
        logger.info(">>>>>> Launching Task API Server >>>>> ");
        logger.info("---------------------------------------");
        String databaseUsername = getDatabaseUsername();
        String databasePassword = getDatabasePassword();
        String databaseUrl = getDatabaseUrl();
        String port = getPort();
        String shutdownSecret = getShutdownSecret();
        DatabaseContext.getInstance(databaseUrl, databaseUsername, databasePassword); // Initialises the database
        JettyServer jettyServer = new JettyServer();
        jettyServer.start(Integer.parseInt(port), shutdownSecret);
    }

    private static String getPropertyOrDefault(String propertyName, String defaultValue, String logMessage, boolean isSecret) {
        String propertyValue = System.getProperty(propertyName);
        if (propertyValue == null || propertyValue.isBlank()) {
            propertyName = propertyName.replace(".", "_").toUpperCase();
            propertyValue = System.getenv().get(propertyName);;
            if (propertyValue == null || propertyValue.isBlank()) {
                propertyValue = defaultValue;
                logger.warn(propertyName + " environment variable not set, using default value '" + propertyValue + "'");
            } else {
                if (isSecret) {
                    logger.info(propertyName + ": *****");
                } else {
                    logger.info(propertyName + ": " + propertyValue);
                }
            }
        } else {
            if (isSecret) {
                logger.info(propertyName + ": *****");
            } else {
                logger.info(propertyName + ": " + propertyValue);
            }
        }
        return propertyValue;
    }

    private static String getShutdownSecret() {
        return getPropertyOrDefault("task.api.shutdown.secret", "secret", "Using shutdown secret", true);
    }

    private static String getDatabasePassword() {
        return getPropertyOrDefault("task.api.database.password", "admin", "Using database password", true);
    }

    private static String getDatabaseUsername() {
        return getPropertyOrDefault("task.api.database.username", "admin", "Using database username", true);
    }

    private static String getPort() {
        return getPropertyOrDefault("task.api.port", "8080", "Listening on port", false);
    }

    private static String getDatabaseUrl() {
        return getPropertyOrDefault("task.api.database.url", "jdbc:derby:applicationdb", "Using database url", false);
    }
}
