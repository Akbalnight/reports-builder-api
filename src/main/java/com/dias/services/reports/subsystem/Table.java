package com.dias.services.reports.subsystem;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class Table {
    private String title;
    private List<String> ignoreFields;
    private Map<String, String> translations;
}
