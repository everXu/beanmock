package com.everxu.beanmock.demo;

import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class UserService {

    @Resource private ConfigurationService configurationService;

    public User getUser(){
        if (configurationService.userCache()){
            return getFromCache();
        }
        return getFromDb();
    }

    private User getFromCache(){
        return new User("ever","cache");
    }

    private User getFromDb(){
        return new User("ever","db");
    }

}
