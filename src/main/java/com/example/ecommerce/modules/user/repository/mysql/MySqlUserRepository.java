package com.example.ecommerce.modules.user.repository.mysql;

import com.example.ecommerce.modules.user.model.User;
import com.example.ecommerce.modules.user.repository.UserRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@Profile("prod")
public class MySqlUserRepository implements UserRepository {

    private final UserMapper userMapper;

    public MySqlUserRepository(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public User save(User user) {
        String normalizedEmail = normalizeEmail(user.getEmail());
        user.setEmail(normalizedEmail);
        LocalDateTime now = LocalDateTime.now();
        if (user.getId() == null) {
            user.setCreatedAt(now);
            user.setUpdatedAt(now);
            userMapper.insert(user);
            return user;
        }
        user.setUpdatedAt(now);
        userMapper.update(user);
        return user;
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return Optional.ofNullable(userMapper.findByEmail(normalizeEmail(email)));
    }

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(userMapper.findById(id));
    }

    @Override
    public boolean existsByEmail(String email) {
        return userMapper.countByEmail(normalizeEmail(email)) > 0;
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
