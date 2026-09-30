package com.cinekeep.user;

import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {
    private static final Long DEVELOPMENT_USER_ID = 1L;

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser() {
        return userRepository.findById(DEVELOPMENT_USER_ID)
                .orElseThrow(UserNotFoundException::new);
    }
}
