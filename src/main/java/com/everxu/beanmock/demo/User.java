package com.everxu.beanmock.demo;

public class User {

    public User(String userName, String source) {
        this.userName = userName;
        this.source = source;
    }

    private String userName;

    private String source;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
