package com.dias.services.notifications;

/**
 * NotifificationsData.java
 * Date: 29 июл. 2019 г.
 * Users: vmeshkov
 * Description: Список уведомлений
 */
public enum NotifificationsData {
    REPORT_ADDED_FAVORITE(23), // Добавлен в избранное
    REPORT_ADDED_PUBLIC(24), // Добавлен в публичные
    REPORT_ADDED_PRIVATE(25), // Добавлен в личное
    REPORT_UPDATED(26),  // Oтчет отредактирован
    REPORT_DELETED(27),  // Удален отчет
    REPORT_EXPORTED(28); // Отчет отправлен на экспорт

    private int id;

    private NotifificationsData(int id) {
        this.id = id;
    }

    public int value() {
        return id;
    }
}
