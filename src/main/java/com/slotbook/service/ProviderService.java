package com.slotbook.service;

import com.slotbook.domain.Role;
import com.slotbook.dto.provider.ProviderSummaryResponse;
import com.slotbook.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProviderService {

    private final UserRepository userRepository;

    public ProviderService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<ProviderSummaryResponse> listProviders(Pageable pageable) {
        return userRepository.findByRole(Role.PROVIDER, pageable)
                .map(user -> new ProviderSummaryResponse(user.getId(), user.getName(), user.getEmail()));
    }
}