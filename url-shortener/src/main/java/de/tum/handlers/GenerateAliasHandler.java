package de.tum.handlers;

import de.tum.services.ConsistentHashingService;
import de.tum.config.HikariCPDataSource;
import de.tum.services.PropertiesService;
import de.tum.services.URLToAliasService;
import de.tum.services.cache.CachingService;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;


@Slf4j
public abstract class GenerateAliasHandler extends GeneralHttpHandler{
    private final URLToAliasService urlToAliasService = URLToAliasService.getInstance();
    private final CachingService cachingService = CachingService.getInstance();
    private final ConsistentHashingService consistentHashingService = ConsistentHashingService.getInstance();
    private final String insertSql;
    private final String selectSql;

    public GenerateAliasHandler() {
        PropertiesService propertiesService = PropertiesService.getInstance();
        this.insertSql = propertiesService.getSqlProperty("sql.generateAliasService.insert");
        this.selectSql = propertiesService.getSqlProperty("sql.generateAliasService.select");
    }

    protected String generateAlias(String url) throws SQLException {
        String alias;
        try (Connection connection = HikariCPDataSource.getConnection()) {
            PreparedStatement insertStatement = connection.prepareStatement(insertSql);
            PreparedStatement selectStatement = connection.prepareStatement(selectSql);
            alias = generateAndPersistAlias(url, insertStatement, selectStatement, 0);
        }
        if (consistentHashingService.isOurs(alias)) {
            cachingService.cache(alias, url);
        }
        return alias;
    }

    private String generateAndPersistAlias(String defaultUrl, PreparedStatement insert,
                                           PreparedStatement select, int attempt) throws SQLException {
        String url = defaultUrl;
        if (attempt != 0) {
            url = url + (attempt-1);
        } else {
            select.setString(1, defaultUrl);
            ResultSet resultSet = select.executeQuery();
            String existingAlias = resultSet.next() ? resultSet.getString(1): null;
            if (existingAlias != null) {
                log.info("For url {} alias has not been generated, because the url is already present in the database", defaultUrl);
                return existingAlias;
            }
        }
        String alias = urlToAliasService.generateAlias(url);
        insert.setString(1, alias);
        insert.setString(2, url);
        insert.setLong(3, new Date().getTime());
        int result = insert.executeUpdate();
        if (result != 1) {
            log.warn("Hash collision has happened for url {}, making another attempt...", url);
            return generateAndPersistAlias(defaultUrl, insert, select,attempt+1);
        } else {
            log.info("Generated alias {} for url {}", alias, url);
            log.info("Local write alias {} for url {}", alias, url);
            return alias;
        }
    }
}
