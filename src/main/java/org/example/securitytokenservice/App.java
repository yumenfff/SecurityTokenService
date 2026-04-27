package org.example.securitytokenservice;

import com.sun.net.httpserver.HttpServer;
import org.flywaydb.core.Flyway;
import org.example.securitytokenservice.api.AdminHandler;
import org.example.securitytokenservice.api.AuthHandler;
import org.example.securitytokenservice.api.OtpHandler;
import org.example.securitytokenservice.config.AppConfig;
import org.example.securitytokenservice.dao.OtpCodeDao;
import org.example.securitytokenservice.dao.OtpConfigDao;
import org.example.securitytokenservice.dao.UserDao;
import org.example.securitytokenservice.db.Database;
import org.example.securitytokenservice.delivery.DeliveryChannelFactory;
import org.example.securitytokenservice.scheduler.OtpExpirationScheduler;
import org.example.securitytokenservice.service.AdminService;
import org.example.securitytokenservice.service.AuthService;
import org.example.securitytokenservice.service.OtpCodeGenerator;
import org.example.securitytokenservice.service.OtpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class App {
    private static final Logger log = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws IOException {
        AppConfig config = AppConfig.load();
        Flyway.configure()
                .dataSource(config.dbUrl(), config.dbUser(), config.dbPassword())
                .baselineOnMigrate(true)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        Database database = new Database(config);

        UserDao userDao = new UserDao(database);
        OtpConfigDao otpConfigDao = new OtpConfigDao(database);
        OtpCodeDao otpCodeDao = new OtpCodeDao(database);
        DeliveryChannelFactory deliveryChannelFactory = new DeliveryChannelFactory(config);

        AuthService authService = new AuthService(database, userDao, config);
        AdminService adminService = new AdminService(database, userDao, otpConfigDao, otpCodeDao);
        OtpService otpService = new OtpService(
                database,
                otpConfigDao,
                otpCodeDao,
                userDao,
                deliveryChannelFactory,
                new OtpCodeGenerator()
        );

        HttpServer server = HttpServer.create(new InetSocketAddress(config.port()), 0);
        server.createContext("/api/auth", new AuthHandler(authService));
        server.createContext("/api/admin", new AdminHandler(authService, adminService));
        server.createContext("/api/otp", new OtpHandler(authService, otpService));
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();

        OtpExpirationScheduler scheduler = new OtpExpirationScheduler();
        scheduler.start(config.schedulerDelaySeconds(), otpService::expireOutdated);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down application");
            scheduler.stop();
            deliveryChannelFactory.close();
            server.stop(1);
        }));

        log.info("Security Token Service started on port {}", config.port());
    }
}
