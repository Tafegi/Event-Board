package com.example.eventboard.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * A connection factory responsible for configuring and creating
 * database connections.
 */
public class ConnectionFactory {
    private static final String DATABASE_PROPERTIES = "database.properties";

    private final DatabaseConfig databaseConfig;

    /**
     * Default constructor.
     * Initializes the connection factory by loading the database configuration
     * from the properties file and registering the corresponding JDBC driver.
     */
    public ConnectionFactory() {
        this.databaseConfig = loadDatabaseConfig();
        loadDriver(databaseConfig.getDriver());
    }

    /**
     * Creates and returns a new database connection using the loaded
     * configuration data (URL, username, and password).
     *
     * @return a {@link Connection} object representing the database connection.
     * @throws SQLException if a database access error occurs or the connection cannot be established.
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                databaseConfig.getUrl(),
                databaseConfig.getUsername(),
                databaseConfig.getPassword()
        );
    }

    /**
     * Loads the database configuration from the properties file (default is "database.properties").
     *
     * @return a {@link DatabaseConfig} object containing the URL, username, password, and driver class name.
     * @throws IllegalStateException if the configuration file is not found in the classpath or an I/O error occurs while reading it.
     */
    private DatabaseConfig loadDatabaseConfig() {
        Properties properties = new Properties();

        try (InputStream inputStream = getClass()
                .getClassLoader()
                .getResourceAsStream(DATABASE_PROPERTIES)) {

            if (inputStream == null) {
                throw new IllegalStateException("Database configuration file not found: " + DATABASE_PROPERTIES);
            }

            properties.load(inputStream);

            return new DatabaseConfig(
                    properties.getProperty("db.url"),
                    properties.getProperty("db.username"),
                    properties.getProperty("db.password"),
                    properties.getProperty("db.driver")
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load database configuration", e);
        }
    }

    /**
     * Loads and registers the JDBC driver class by its name.
     *
     * @param driver the fully qualified name of the database driver class (e.g., "org.postgresql.Driver").
     * @throws IllegalStateException if the specified driver class is not found.
     */
    private void loadDriver(String driver) {
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Database driver not found: " + driver, e);
        }
    }
}