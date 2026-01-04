package com.one.kc.common.utils;


import com.one.kc.chanting.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

public class ResponseEntityUtils {

    public static <T>ResponseEntity<T> getCreatedResponse(T response){
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    public static <T>ResponseEntity<T> badResquest(){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }


    public static <T> ResponseEntity<PageResponse<T>> getPaginatedResponse(Page<?> page, List<T> content) {

        PageResponse<T> response = new PageResponse<>();
        response.setContent(content);
        response.setPageNo(page.getNumber());
        response.setPageSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setLast(page.isLast());

        return ResponseEntity.ok(response);
    }

}
