package com.example.authservice.service;

import com.example.authservice.model.User;
import com.example.authservice.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;


    public UserService(UserRepository userRepository, UserRepository userRepository1) {
        this.userRepository = userRepository1;
    }

    public Optional<User> findByEmail(String email) {
    return  userRepository.findByEmail(email);
    }
}
