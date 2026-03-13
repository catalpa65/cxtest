package com.example.ecommerce.modules.user.repository;

import com.example.ecommerce.modules.user.model.User;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@Profile("memory")
public class InMemoryUserRepository implements UserRepository {

    private final ConcurrentHashMap<Long, User> usersById = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> emailIndex = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(0);

    @Override
    public User save(User user) {
        LocalDateTime now = LocalDateTime.now();
        if (user.getId() == null) {
            user.setId(idGenerator.incrementAndGet());
            user.setCreatedAt(now);
        }
        user.setUpdatedAt(now);

        usersById.put(user.getId(), user);
        emailIndex.put(normalizeEmail(user.getEmail()), user.getId());
        return user;
    }

    @Override
    public Optional<User> findByEmail(String email) {
        Long userId = emailIndex.get(normalizeEmail(email));
        if (userId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(usersById.get(userId));
    }

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(usersById.get(id));
    }

    @Override
    public boolean existsByEmail(String email) {
        return emailIndex.containsKey(normalizeEmail(email));
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
