package com.everxu.beanmock.demo;

import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class LoginService {

    @Resource private UserService userService;

    public User login(){
        User user = userService.getUser();
        System.out.println(user);
        return user;
    }

}
