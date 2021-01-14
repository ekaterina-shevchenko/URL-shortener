package de.tum.services;

import de.tum.Starter;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesService {
    private static final PropertiesService INSTANCE = new PropertiesService();
    private final Properties sqlProperties = new Properties();
    private final Properties appProperties = new Properties();

    private PropertiesService() {}

    public static PropertiesService getInstance() { return INSTANCE; }

    public void init() throws IOException {
        loadResourceProperties(appProperties, "/app.properties");
        loadResourceProperties(sqlProperties, "/sql.properties");
        String appPropertyPath = System.getProperty("app.properties");
        if (appPropertyPath != null) {
            loadProperties(appProperties, new FileInputStream(appPropertyPath));
        }
        String sqlPropertyPath = System.getProperty("sql.properties");
        if (sqlPropertyPath != null) {
            loadProperties(sqlProperties, new FileInputStream(sqlPropertyPath));
        }
    }

    private static void loadResourceProperties(Properties properties, String filename) throws IOException {
        InputStream stream = Starter.class.getResourceAsStream(filename);
        loadProperties(properties,stream);
    }

    private static void loadProperties(Properties properties, InputStream stream) throws IOException {
        properties.load(stream);
        stream.close();
    }

    public String getSqlProperty(String key) {
        return sqlProperties.getProperty(key);
    }

    public String getAppProperty(String key) {
        return appProperties.getProperty(key);
    }

}
