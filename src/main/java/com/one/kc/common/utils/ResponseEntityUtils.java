package com.one.kc.common.utils;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ResponseEntityUtils {

    public static <T>ResponseEntity<T> getCreatedResponse(T response){
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    public static <T>ResponseEntity<T> badResquest(){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }


}
