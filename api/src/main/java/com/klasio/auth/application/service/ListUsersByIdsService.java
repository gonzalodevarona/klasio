package com.klasio.auth.application.service;

import com.klasio.auth.application.port.UserRepository;
import com.klasio.auth.application.port.input.ListUsersByIdsUseCase;
import com.klasio.auth.domain.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ListUsersByIdsService implements ListUsersByIdsUseCase {

    private final UserRepository userRepository;

    public ListUsersByIdsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<UserSummary> execute(UUID tenantId, Set<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return userRepository.findAllByIds(tenantId, userIds).stream()
                .map(u -> new UserSummary(
                        u.getId(),
                        fullName(u),
                        u.primaryRole().name()
                ))
                .toList();
    }

    private static String fullName(User u) {
        String first = u.getFirstName();
        String last  = u.getLastName();
        if (first == null && last == null) return "";
        if (first == null) return last;
        if (last == null)  return first;
        return first + " " + last;
    }
}
