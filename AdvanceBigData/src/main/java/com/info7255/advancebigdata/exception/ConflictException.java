package com.info7255.advancebigdata.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictException extends RuntimeException{
    private String field;

    public ConflictException(String message, String field){
        super(message);
        this.field = field;
    }
}
