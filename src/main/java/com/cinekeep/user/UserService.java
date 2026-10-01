package com.cinekeep.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final CurrentUserService currentUserService;

    public UserService(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile() {
        return UserResponse.from(currentUserService.getCurrentUser());
    }
}
