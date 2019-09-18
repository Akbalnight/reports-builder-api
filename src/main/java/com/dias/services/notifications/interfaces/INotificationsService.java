package com.dias.services.notifications.interfaces;

import java.util.List;

/**
 * INotificationsService.java
 * Date: 25 июл. 2019 г.
 * Users: vmeshkov
 * Description: Сервис для отправки уведомления пользователям
 */
public interface INotificationsService {
    /**
     * Отправить уведомление
     *
     * @param typeId      тип уведомления
     * @param objects     обьекты для форматирования сообщения
     * @param receivers   список получателей
     * @param targetId    идентификатор обьекта уведомления
     * @param initiatorId идентификатор пользователя, который инциировал уведомление
     */
    void sendNotification(int typeId, String[] objects, List<Integer> receivers, String targetId, Integer initiatorId);

    /**
     * Остановить уведомление
     *
     * @param typeId    тип уведомления
     * @param receivers список получателей уведомления
     * @param id        идентификатор обьекта уведомления
     */
    void stopNotifications(int typeId, List<Integer> receivers, String targetId);
}
