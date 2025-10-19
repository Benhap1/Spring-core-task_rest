package com.gymcrm.gym_crm_spring.service;

import com.gymcrm.gym_crm_spring.dao.UserDao;
import com.gymcrm.gym_crm_spring.domain.User;
import com.gymcrm.gym_crm_spring.exception.InvalidCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService extends AbstractService<User> {
    private final BCryptPasswordEncoder encoder;
    private final UserDao dao;

    public UserService(BCryptPasswordEncoder encoder, UserDao dao) {
        super(dao);
        this.encoder = encoder;
        this.dao = dao;
    }

    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return dao.findByUsername(username);
    }

    @Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {
        var user = dao.findByUsername(username)
                .orElseThrow(InvalidCredentialsException::new);

        if (!encoder.matches(oldPassword, user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        user.setPassword(encoder.encode(newPassword));
        dao.save(user);
    }
}


