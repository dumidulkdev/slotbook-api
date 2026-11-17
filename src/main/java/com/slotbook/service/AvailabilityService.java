package com.slotbook.service;

import com.slotbook.domain.ProviderAvailabilityEntity;
import com.slotbook.domain.Role;
import com.slotbook.domain.UserEntity;
import com.slotbook.dto.availability.AvailabilityRequest;
import com.slotbook.dto.availability.AvailabilityResponse;
import com.slotbook.exception.ForbiddenException;
import com.slotbook.exception.ResourceNotFoundException;
import com.slotbook.mapper.AvailabilityMapper;
import com.slotbook.repository.ProviderAvailabilityRepository;
import com.slotbook.repository.UserRepository;
import com.slotbook.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AvailabilityService {

    private final ProviderAvailabilityRepository availabilityRepository;
    private final UserRepository userRepository;
    private final AvailabilityValidator availabilityValidator;
    private final AvailabilityMapper availabilityMapper;

    public AvailabilityService(ProviderAvailabilityRepository availabilityRepository,
                               UserRepository userRepository,
                               AvailabilityValidator availabilityValidator,
                               AvailabilityMapper availabilityMapper) {
        this.availabilityRepository = availabilityRepository;
        this.userRepository = userRepository;
        this.availabilityValidator = availabilityValidator;
        this.availabilityMapper = availabilityMapper;
    }

    @Transactional
    public AvailabilityResponse setAvailability(UserPrincipal principal, AvailabilityRequest request) {
        assertProvider(principal);
        availabilityValidator.validate(request.slotLengthMinutes(), request.workingWindows());
        UserEntity provider = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found"));
        ProviderAvailabilityEntity availability = availabilityRepository.findByProviderId(provider.getId())
                .orElse(new ProviderAvailabilityEntity(provider, request.slotLengthMinutes(),
                        availabilityMapper.toEmbeddables(request.workingWindows())));
        if (availability.getId() != null) {
            availability.update(request.slotLengthMinutes(),
                    availabilityMapper.toEmbeddables(request.workingWindows()));
        }
        return availabilityMapper.toResponse(availabilityRepository.save(availability));
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse getMyAvailability(UserPrincipal principal) {
        assertProvider(principal);
        ProviderAvailabilityEntity availability = availabilityRepository.findByProviderId(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Availability not configured"));
        return availabilityMapper.toResponse(availability);
    }

    private void assertProvider(UserPrincipal principal) {
        if (principal.getRole() != Role.PROVIDER) {
            throw new ForbiddenException("Only providers can manage availability");
        }
    }
}