package com.bankloan.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads application and database configuration parameters from local db.properties.
 * Searches in config/db.properties, then fallback to db.properties in working directory.
 */
public class DatabaseConfig {

    private static final String DEFAULT_CONFIG_PATH = "config/db.properties";
    private static final String FALLBACK_CONFIG_PATH = "db.properties";

    private final Properties props = new Properties();

    private static DatabaseConfig instance;

    private DatabaseConfig() {
        loadProperties();
    }

    public static synchronized DatabaseConfig getInstance() {
        if (instance == null) {
            instance = new DatabaseConfig();
        }
        return instance;
    }

    private void loadProperties() {
        File file = new File(DEFAULT_CONFIG_PATH);
        if (!file.exists()) {
            file = new File(FALLBACK_CONFIG_PATH);
        }

        if (file.exists()) {
            try (InputStream in = new FileInputStream(file)) {
                props.load(in);
                System.out.println("[CONFIG] Successfully loaded database configuration from: " + file.getPath());
            } catch (IOException e) {
                System.err.println("[CONFIG ERROR] Could not read configuration file: " + e.getMessage());
            }
        } else {
            System.err.println("[CONFIG WARNING] No db.properties found at " + DEFAULT_CONFIG_PATH + " or " + FALLBACK_CONFIG_PATH);
        }
    }

    public int getServerPort() {
        String val = props.getProperty("server.port", "8085");
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return 8085;
        }
    }

    public String getDbDriver() {
        return props.getProperty("db.driver", "oracle.jdbc.OracleDriver").trim();
    }

    public String getDbUrl() {
        return props.getProperty("db.url", "jdbc:oracle:thin:@localhost:1521:xe").trim();
    }

    public String getDbUsername() {
        return props.getProperty("db.username", "").trim();
    }

    public String getDbPassword() {
        return props.getProperty("db.password", "").trim();
    }

    public String getOdbcDsn() {
        return props.getProperty("odbc.dsn", "OracleXE_PAT4").trim();
    }

    public String getOdbcDriver() {
        return props.getProperty("odbc.driver", "Oracle in XE").trim();
    }

    public String getOdbcUsername() {
        return props.getProperty("odbc.username", getDbUsername()).trim();
    }

    public String getOdbcPassword() {
        return props.getProperty("odbc.password", getDbPassword()).trim();
    }
}
