package com.user.service.UserService.service.impl;

import com.netflix.discovery.converters.Auto;
import com.user.service.UserService.entities.Rating;
import com.user.service.UserService.entities.User;
import com.user.service.UserService.entities.Hotels;
import com.user.service.UserService.exceptions.ResourceNotFoundException;
import com.user.service.UserService.repository.UserRepository;
import com.user.service.UserService.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RestTemplate restTemplate;

    private Logger logger = LoggerFactory.getLogger(UserService.class);
    @Override
    public User saveUser(User user) {
        // generate unik id
        String randomUserId = UUID.randomUUID().toString();
        user.setUserId(randomUserId);
        return userRepository.save(user);
    }

    @Override
    public List<User> getAllUser() {
        return userRepository.findAll();
    }

    @Override
    public User getUser(String userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User with given id is not found server " + userId));

        // Fetch rating of the above user from Rating service
        // http://localhost:8083/ratings/users/fc09ef9b-8d27-439e-b87c-c97be29b2aa8
        Rating[] ratingOfUser = restTemplate.getForObject("http://RETINGS-SERVICE/ratings/users/" +user.getUserId(), Rating[].class);
        logger.info("{}",ratingOfUser);
       List<Rating> ratings = Arrays.stream(ratingOfUser).toList();
       //http://localhost:8082/hotels/2ba55b84-dfca-42cd-baa9-d1425f241e3d

        List<Rating> ratingList = ratings.stream().map(rating -> {

           ResponseEntity<Hotels> forEntity  = restTemplate.getForEntity("http://HOTEL-SERVICE/hotels/"+rating.getHotelId(),Hotels.class);
           Hotels hotels = forEntity.getBody();

           rating.setHotel(hotels);
           return rating;
        }).collect(Collectors.toList());

        user.setRating(ratingList);
        return user;

    }
}
