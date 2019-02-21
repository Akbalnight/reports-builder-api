package com.dias.services.reports.exception;

import org.springframework.http.HttpStatus;

public class ReportsException extends RuntimeException {

    public static String WRONG_DIAGRAMM_FORMAT = "Отчет: %d - неверный формат диаграммы: не задано свойство \"%s\"";
    public static String WRONG_DIAGRAMM_SERIES_FORMAT = "Отчет: %d - неверный формат серии диаграммы: не задано свойство \"%s\"";
    private final Object[] args;
    private HttpStatus status;

    public ReportsException(String message, Object ...args) {
        this(message, HttpStatus.INTERNAL_SERVER_ERROR, args);
    }

    public ReportsException(String message, HttpStatus status, Object ...args) {
        super(message);
        this.args = args;
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    @Override
    public String getMessage() {
        String message = super.getMessage();
        if (args != null) {
            message = String.format(message, args);
        }
        return message;
    }
}
