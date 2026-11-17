package com.slotbook.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "provider_availability")
public class ProviderAvailabilityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "provider_id", unique = true, nullable = false)
    private UserEntity provider;

    @Column(name = "slot_length_minutes", nullable = false)
    private int slotLengthMinutes;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<WorkingWindowEmbeddable> workingWindows = new ArrayList<>();

    protected ProviderAvailabilityEntity() {
    }

    public ProviderAvailabilityEntity(UserEntity provider, int slotLengthMinutes,
                                      List<WorkingWindowEmbeddable> workingWindows) {
        this.provider = provider;
        this.slotLengthMinutes = slotLengthMinutes;
        this.workingWindows = new ArrayList<>(workingWindows);
    }

    public Long getId() {
        return id;
    }

    public UserEntity getProvider() {
        return provider;
    }

    public int getSlotLengthMinutes() {
        return slotLengthMinutes;
    }

    public List<WorkingWindowEmbeddable> getWorkingWindows() {
        return workingWindows;
    }

    public void update(int slotLengthMinutes, List<WorkingWindowEmbeddable> workingWindows) {
        this.slotLengthMinutes = slotLengthMinutes;
        this.workingWindows.clear();
        this.workingWindows.addAll(workingWindows);
    }
}