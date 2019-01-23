package com.dias.services.reports.subsystem;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ColumnWithType {

    private String column;
    private String type;
    private String title;
    private boolean requiresQuoting;
}
