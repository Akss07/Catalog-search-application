package com.info7255.advancebigdata.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_MODIFIED)
public class ResourceNotModifiedException extends RuntimeException{
    public ResourceNotModifiedException(String message){
        super(message);
    }
}
