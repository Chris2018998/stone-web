package org.stone.springboot.annotation;

public interface UserMixIn {
    @com.fasterxml.jackson.annotation.JsonIgnore
    String getAge();
}