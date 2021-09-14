package com.dias.services.reports.model;

import lombok.*;

/**
 * Отчет. Объект, инкапсулирующий в себе SQL запрос в виде <code>QueryDescriptor</code>
 * Применяется для получения данных с заранее определлными группировками, сортировкой, аггрегатными функциями и т.д.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report extends AbstractModel {
    private String name;
    private String type;
    private String title;
    private String createdBy;
    private Boolean isFavorite;
    private Boolean isPublic;
    private Boolean limit50;
    private String queryDescriptor;
    private String description;
}
