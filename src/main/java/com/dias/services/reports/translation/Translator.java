package com.dias.services.reports.translation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class Translator {

    private final MessageSource messageSource;

    @Autowired
    public Translator(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String translate(String input) {
        return messageSource.getMessage(input, null, input, Locale.forLanguageTag("ru"));
    }
}
