package de.tum.jobs;

import de.tum.config.HikariCPDataSource;
import de.tum.services.PropertiesService;
import de.tum.services.cache.CachingService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ObsoleteDataCleaner implements Runnable {
    private final Integer timeout = Integer.parseInt(
            PropertiesService.getInstance().getAppProperty("alias.timeout"));
    private final String select = PropertiesService.getInstance().getSqlProperty("sql.databaseCleaner.select");
    private final String delete = PropertiesService.getInstance().getSqlProperty("sql.databaseCleaner.delete");

    @Override
    public void run() {
        try (Connection connection = HikariCPDataSource.getConnection()) {
            List<String> expiredAliases = getExpiredAliases(connection);
            cleanDatabase(connection, expiredAliases);
            cleanCache(expiredAliases);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private Long getMinimumDate(){
        return new Date().getTime() - timeout * 60 * 1000;
    }

    private List<String> getExpiredAliases(Connection connection) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(select);
        statement.setLong(1, getMinimumDate());
        ResultSet result = statement.executeQuery();
        List<String> expiredAliases = new ArrayList<>();
        while (result.next()) {
            expiredAliases.add(result.getString(1));
        }
        return expiredAliases;
    }

    private void cleanDatabase(Connection connection, List<String> expiredAliases) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(delete);
        for (String expiredAlias : expiredAliases) {
            statement.setString(1, expiredAlias);
            statement.execute();
        }
    }

    private void cleanCache(List<String> expiredAliases) {
        expiredAliases.forEach(CachingService.getInstance()::removeFromCache);
    }
}
