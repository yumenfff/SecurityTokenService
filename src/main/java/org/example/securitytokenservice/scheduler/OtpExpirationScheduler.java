package org.example.securitytokenservice.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class OtpExpirationScheduler {
    private static final Logger log = LoggerFactory.getLogger(OtpExpirationScheduler.class);
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public void start(long delaySeconds, Runnable task) {
        executor.scheduleAtFixedRate(() -> {
            try {
                task.run();
            } catch (Exception e) {
                log.error("OTP expiration job failed", e);
            }
        }, delaySeconds, delaySeconds, TimeUnit.SECONDS);
    }

    public void stop() {
        executor.shutdownNow();
    }
}
