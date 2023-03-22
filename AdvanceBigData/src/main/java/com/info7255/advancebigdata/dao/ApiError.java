package com.info7255.advancebigdata.dao;

import org.springframework.http.HttpStatus;

public class ApiError {
    private String message;
    private HttpStatus statusCode;

    public ApiError(){}

    public ApiError(String message, HttpStatus statusCode){
        this.message = message;
        this.statusCode = statusCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public HttpStatus getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(HttpStatus statusCode) {
        this.statusCode = statusCode;
    }
}
