package com.codewithsubhra.money_tracker_backend.user;

import com.codewithsubhra.money_tracker_backend.common.exception.ResourceNotFoundException;
import com.codewithsubhra.money_tracker_backend.user.domain.User;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository users;

    public UserService(UserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public User getById(UUID id) {
        return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User"));
    }
}
