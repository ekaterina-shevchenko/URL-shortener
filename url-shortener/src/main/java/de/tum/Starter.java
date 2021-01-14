package de.tum;

import com.opencsv.CSVReader;
import com.sun.net.httpserver.HttpServer;
import de.tum.config.HikariCPDataSource;
import de.tum.handlers.Dispatcher;
import de.tum.jobs.ObsoleteDataCleaner;
import de.tum.services.PropertiesService;
import de.tum.services.cache.jobs.CacheCleaner;
import de.tum.services.cluster.ClusterService;
import lombok.extern.slf4j.Slf4j;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class Starter {

    public static void main(String[] args) throws IOException, SQLException, ParseException {
        PropertiesService.getInstance().init();
        initDataSource();
        loadDbState();
        runObsoleteDataCleaner();
        runCacheCleaner();
        ClusterService.getInstance().startClusterService();
        HttpServer httpServer = createAndInitHttpServer();
        httpServer.start();
    }

    private static void loadDbState() throws SQLException, IOException, ParseException {
        try (Connection connection = HikariCPDataSource.getConnection()) {
            PreparedStatement statement = connection
                    .prepareStatement(PropertiesService.getInstance()
                            .getSqlProperty("sql.generateAliasService.insert"));
            CSVReader reader = new CSVReader(new FileReader("db_init.csv"));
            //strings.remove(0);
            String[] string;
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            while((string = reader.readNext()) != null) {
                String alias = string[0];
                String date = string[1];
                String url = string[2];
                statement.setString(1, alias);
                statement.setString(2, url);
                statement.setLong(3, simpleDateFormat.parse(date).getTime());
                statement.execute();
            }
            log.info("Database has been initialized with csv file");
            reader.close();
        } catch (FileNotFoundException e) {
            // do nothing
        }
    }

    private static void initDataSource(){
        HikariCPDataSource.initialize();
    }

    private static void runObsoleteDataCleaner() {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleWithFixedDelay(new ObsoleteDataCleaner(), 3600, 5, TimeUnit.SECONDS);
    }

    private static void runCacheCleaner() {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleWithFixedDelay(new CacheCleaner(), 1, 1, TimeUnit.MINUTES);
    }

    private static HttpServer createAndInitHttpServer() throws IOException {
        PropertiesService propertiesService = PropertiesService.getInstance();
        InetSocketAddress inetSocketAddress = 
                new InetSocketAddress(Integer.parseInt(propertiesService.getAppProperty("server.port")));
        HttpServer httpServer = HttpServer.create(inetSocketAddress,
                Integer.parseInt(propertiesService.getAppProperty("server.backlog")));
        ExecutorService executorService = Executors.newFixedThreadPool(
                Integer.parseInt(propertiesService.getAppProperty("server.pool.size")));
        httpServer.setExecutor(executorService);
        httpServer.createContext("/", new Dispatcher());
        return httpServer;
    }
}
