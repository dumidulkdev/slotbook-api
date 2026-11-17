package com.slotbook.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings", uniqueConstraints = @UniqueConstraint(
        name = "uk_bookings_provider_active_start",
        columnNames = {"provider_id", "active_start_time"}))
public class BookingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private UserEntity provider;

    @ManyToOne(optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private UserEntity client;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "active_start_time")
    private LocalDateTime activeStartTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status = BookingStatus.BOOKED;

    @Version
    private Long version;

    protected BookingEntity() {
    }

    public BookingEntity(UserEntity provider, UserEntity client,
                         LocalDateTime startTime, LocalDateTime endTime) {
        this.provider = provider;
        this.client = client;
        this.startTime = startTime;
        this.activeStartTime = startTime;
        this.endTime = endTime;
        this.status = BookingStatus.BOOKED;
    }

    public Long getId() {
        return id;
    }

    public UserEntity getProvider() {
        return provider;
    }

    public UserEntity getClient() {
        return client;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public Long getVersion() {
        return version;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
        if (status != BookingStatus.BOOKED) {
            this.activeStartTime = null;
        }
    }

    public LocalDateTime getActiveStartTime() {
        return activeStartTime;
    }

    public boolean isActive() {
        return status == BookingStatus.BOOKED;
    }
}