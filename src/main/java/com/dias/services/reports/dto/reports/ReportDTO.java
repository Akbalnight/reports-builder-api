package com.dias.services.reports.dto.reports;

import com.dias.services.reports.report.query.QueryDescriptor;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportDTO {
    private Long id;
    private String name;
    private String type;
    private String title;
    private String createdBy;
    private JsonNode description;
    private Boolean isFavorite;
    private Boolean isPublic;
    private Boolean limit50;
    private QueryDescriptor queryDescriptor;
}
