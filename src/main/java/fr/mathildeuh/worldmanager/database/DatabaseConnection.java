package fr.mathildeuh.worldmanager.database;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Abstract database connection manager
 */
public abstract class DatabaseConnection {

    protected String databaseName;

    public DatabaseConnection(String databaseName) {
        this.databaseName = databaseName;
    }

    /**
     * Get a database connection
     */
    public abstract Connection getConnection() throws SQLException;

    /**
     * Close the connection pool
     */
    public abstract void closeConnection() throws SQLException;

    /**
     * Initialize the database tables
     */
    public abstract void createTables() throws SQLException;

    /**
     * Get the database type
     */
    public abstract String getDatabaseType();
}

