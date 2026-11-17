package com.slotbook.config;

import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String timezone = "UTC";
    private Jwt jwt = new Jwt();
    private Booking booking = new Booking();

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public ZoneId zoneId() {
        return ZoneId.of(timezone);
    }

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public static class Jwt {
        private String secret;
        private long expirationMinutes = 60;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getExpirationMinutes() {
            return expirationMinutes;
        }

        public void setExpirationMinutes(long expirationMinutes) {
            this.expirationMinutes = expirationMinutes;
        }
    }

    public static class Booking {
        private int cancellationCutoffHours = 2;

        public int getCancellationCutoffHours() {
            return cancellationCutoffHours;
        }

        public void setCancellationCutoffHours(int cancellationCutoffHours) {
            this.cancellationCutoffHours = cancellationCutoffHours;
        }
    }
}