package com.info7255.advancebigdata.exception;


import com.info7255.advancebigdata.dao.ApiError;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Object> handleBadRequestException(BadRequestException ex){
        ApiError apiError = new ApiError("Not a valid Body", HttpStatus.BAD_REQUEST);
        apiError.setMessage(ex.getMessage());
        apiError.setStatusCode(HttpStatus.BAD_REQUEST);
        return buildResponseEntity(apiError);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Object> handleConflictException(ConflictException ex){
        ApiError apiError = new ApiError("Plan already exist", HttpStatus.CONFLICT);
        apiError.setMessage(ex.getMessage());
        apiError.setStatusCode(HttpStatus.CONFLICT);
        return buildResponseEntity(apiError);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleResourceNotFoundException(ResourceNotFoundException ex){
        ApiError apiError = new ApiError("Plan not found", HttpStatus.NOT_FOUND);
        apiError.setMessage(ex.getMessage());
        apiError.setStatusCode(HttpStatus.NOT_FOUND);
        return buildResponseEntity(apiError);
    }

    @ExceptionHandler(ResourceNotModifiedException.class)
    public ResponseEntity<Object> handleResourceNotModifiedException(ResourceNotFoundException ex){
        ApiError apiError = new ApiError("Plan not modified!", HttpStatus.NOT_MODIFIED);
        apiError.setMessage(ex.getMessage());
        apiError.setStatusCode(HttpStatus.NOT_MODIFIED);
        return buildResponseEntity(apiError);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Object> handleUnauthorizedException(UnauthorizedException ex){
        ApiError apiError = new ApiError();
        apiError.setMessage(ex.getMessage());
        apiError.setStatusCode(HttpStatus.NOT_MODIFIED);
        return buildResponseEntity(apiError);
    }

    private ResponseEntity<Object> buildResponseEntity(ApiError apiError) {
        return new ResponseEntity<>(apiError, apiError.getStatusCode());
    }


}
