package de.tum.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.tum.Starter;
import de.tum.services.PropertiesService;
import lombok.SneakyThrows;
import org.apache.commons.codec.Charsets;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class HikariCPDataSource {

    private static HikariConfig config = new HikariConfig();
    private static HikariDataSource ds;

    public static void initialize(){
        PropertiesService propertiesService = PropertiesService.getInstance();
        config.setJdbcUrl(propertiesService.getAppProperty("database.url"));
        config.setUsername(propertiesService.getAppProperty("database.username"));
        config.setPassword(propertiesService.getAppProperty("database.password"));
        config.setMaximumPoolSize(Integer.parseInt(propertiesService.getAppProperty("database.pool.size")));
        ds = new HikariDataSource(config);
        createSchema();
    }

    public static Connection getConnection() throws SQLException {
        return ds.getConnection();
    }

    @SneakyThrows
    private static void createSchema(){
        try (Connection connection = getConnection()) {
            final InputStream resourceStream = Starter.class.getResourceAsStream("/ddl.sql");
            String fileContent = IOUtils.toString(resourceStream, Charsets.UTF_8);
            List<String> fileStatements = Arrays.stream(fileContent.split(";"))
                    .filter(line -> !line.isEmpty()).collect(Collectors.toList());
            for (String fileStatement : fileStatements) {
                Statement statement = connection.createStatement();
                statement.execute(fileStatement);
            }
        }
    }

    private HikariCPDataSource(){}
}
