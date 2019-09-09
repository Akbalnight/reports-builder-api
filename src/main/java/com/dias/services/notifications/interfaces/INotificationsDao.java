package com.dias.services.notifications.interfaces;

import java.util.List;

/**
 * INotificationsDao.java
 * Date: 25 июл. 2019 г.
 * Users: vmeshkov
 * Description: Интерфасе для работы с данными уведомлений - добавление уведомлений и рассылок
 */
public interface INotificationsDao {
    /**
     * Создать уведомления
     *
     * @param typeId      тип уведомления
     * @param objects     обьекты для форматирования сообщения
     * @param receivers   список получателей
     * @param targetId    идентификатор обьекта уведомления
     * @param initiatorId идентификатор пользователя, инициатора события
     */
    void createNotifications(int typeId, String[] objects, List<Integer> receivers, String targetId, Integer initiatorId);

    /**
     * Создать рассылку
     *
     * @param typeId      тип уведомления
     * @param objects     обьекты для форматирования сообщения
     * @param targetId    идентификатор обьекта уведомления
     * @param initiatorId идентификатор пользователя, инициатора события
     */
    void createSendOut(int typeId, String[] objects, String targetId, Integer initiatorId);

    /**
     * Остановить уведомление
     *
     * @param typeId    тип уведомления
     * @param receivers список получателей уведомления
     * @param id        идентификатор обьекта уведомления
     */
    void stopNotifications(int typeId, List<Integer> receivers, String targetId);

    /**
     * Остановить уведомление
     *
     * @param typeId тип уведомления
     * @param id     идентификатор обьекта уведомления
     */
    void stopSendOut(int typeId, String targetId);
}
