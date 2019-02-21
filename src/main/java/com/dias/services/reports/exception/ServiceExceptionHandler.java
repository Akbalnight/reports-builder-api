package com.dias.services.reports.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
public class ServiceExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler({ObjectNotFoundException.class})
    @ResponseBody
    protected ResponseEntity<Map<String, Object>> handleNotFoundException(HttpServletRequest req, Throwable ex) {
        return handleException(req, ex, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({ReportsException.class})
    @ResponseBody
    protected ResponseEntity<Map<String, Object>> handleControllerException(HttpServletRequest req, Throwable ex) {
        return handleException(req, ex, ((ReportsException)ex).getStatus());
    }

    private ResponseEntity<Map<String, Object>> handleException(HttpServletRequest req, Throwable ex, HttpStatus status) {
        Map<String, Object> errorAttributes = new LinkedHashMap<>();
        errorAttributes.put("severity", "Error");
        errorAttributes.put("errorMessage", ex.getMessage());
        return new ResponseEntity<>(errorAttributes, HttpStatus.valueOf(status.value()));
    }
}
