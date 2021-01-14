package de.tum.handlers;

import com.sun.net.httpserver.HttpExchange;
import de.tum.config.HikariCPDataSource;
import de.tum.services.ConsistentHashingService;
import de.tum.services.PropertiesService;
import de.tum.services.cache.CachingService;
import de.tum.services.cluster.ClusterMember;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import java.io.IOException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Slf4j
public class ResolveAliasHandler extends GeneralHttpHandler{
    private static final ResolveAliasHandler INSTANCE = new ResolveAliasHandler();
    private final ConsistentHashingService consistentHashingService = ConsistentHashingService.getInstance();
    private final CachingService cachingService = CachingService.getInstance();
    private final String selectSql;

    private ResolveAliasHandler(){
        PropertiesService propertiesService = PropertiesService.getInstance();
        this.selectSql = propertiesService.getSqlProperty("sql.resolveAliasService.select");
    }

    public static ResolveAliasHandler getInstance() { return INSTANCE; }

    @Override
    public HTTPServerResponse handleWithResponse(HttpExchange exchange) {
        log.info("Resolve alias method has been executed");
        HTTPServerResponse serverResponse = new HTTPServerResponse();
        String alias = exchange.getRequestURI().getPath().substring(1);
        if (!consistentHashingService.isOurs(alias)) {
            try {
                ClusterMember receiver = consistentHashingService.getStoringNode(alias);
                HttpGet request = new HttpGet("http://" + receiver.getAddress() + ":" + receiver.getPort() + "/" + URLEncoder.encode(alias,"utf-8"));
                HttpClient client = HttpClients.createMinimal();
                log.info("Read request for alias {} forwarded to node {}", alias, receiver);
                HttpResponse httpResponse = client.execute(request);
                int code = httpResponse.getStatusLine().getStatusCode();
                serverResponse.setCode(code);
                String url = IOUtils.toString(httpResponse.getEntity().getContent(), Charsets.UTF_8);
                serverResponse.setBody(url);
            } catch (IOException e) {
                serverResponse.setCode(500);
                serverResponse.setBody("Server error");
            }
        } else {
            String cacheLookupResult = cachingService.getFromCache(alias);
            if (cacheLookupResult == null) {
                try {
                    String databaseLookupResult = getFromDatabase(alias);
                    if (databaseLookupResult == null) {
                        serverResponse.setBody("Unknown alias");
                        serverResponse.setCode(404);
                    } else {
                        log.info("Read database storage succeeded for alias {}", alias);
                        serverResponse.setBody(databaseLookupResult);
                        cachingService.cache(alias, serverResponse.getBody());
                    }
                } catch (SQLException e) {
                    serverResponse.setCode(500);
                    serverResponse.setBody("Server error");
                }
            } else {
                log.info("Read cache succeeded for alias {}", alias);
                serverResponse.setBody(cacheLookupResult);
            }
        }
        return serverResponse;
    }

    private String getFromDatabase(String alias) throws SQLException {
        try (Connection connection = HikariCPDataSource.getConnection()) {
            PreparedStatement selectStatement = connection.prepareStatement(selectSql);
            selectStatement.setString(1, alias);
            ResultSet resultSet = selectStatement.executeQuery();
            return  resultSet.next() ? resultSet.getString(1) : null;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            throw throwables;
        }
    }

}