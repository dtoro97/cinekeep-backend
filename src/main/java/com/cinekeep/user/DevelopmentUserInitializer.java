package com.cinekeep.user;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DevelopmentUserInitializer implements ApplicationRunner {
    private static final String DEVELOPMENT_USER_NAME = "dev";

    private final UserRepository userRepository;

    public DevelopmentUserInitializer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() == 0) {
            userRepository.save(new User(DEVELOPMENT_USER_NAME));
        }
    }
}
