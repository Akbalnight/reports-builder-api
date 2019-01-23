package com.dias.services.reports.model;

import lombok.*;

/**
 * Подсистема. Логическое объединение таблиц/вью, которые используются конструктором отчетов
 * для иерерахического представления
 * //TODO пока не используется и использование под вопросом. На данный момент в ресурсах сервиса просто лежит json с деревом объектов
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubSystem extends AbstractModel {
    private Long id;
    private String name;
    private String title;
    private String[] views;
}
