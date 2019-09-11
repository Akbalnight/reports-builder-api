package com.dias.services.notifications;

import com.dias.services.notifications.interfaces.INotificationsDao;
import com.dias.services.notifications.interfaces.INotificationsService;

import java.util.List;

/**
 * NotificationsServiceImpl.java
 * Date: 25 июл. 2019 г.
 * Users: vmeshkov
 * Description: Добавить уведомления и рассылки
 */
public class NotificationsServiceImpl implements INotificationsService {

    public NotificationsServiceImpl(INotificationsDao notificationsDao) {
        this.notificationsDao = notificationsDao;
    }

    private INotificationsDao notificationsDao;

    @Override
    public void sendNotification(int typeId, String[] objects,
                                 List<Integer> receivers, String targetId, Integer initiatorId) {
        if (receivers != null && receivers.size() > 0 && receivers.get(0) != -1) {
            notificationsDao.createNotifications(typeId, objects, receivers, targetId, initiatorId);
        }

        notificationsDao.createSendOut(typeId, objects, targetId, initiatorId);
    }

    @Override
    public void stopNotifications(int typeId, List<Integer> receivers, String targetId) {
        notificationsDao.stopNotifications(typeId, receivers, targetId);
        notificationsDao.stopSendOut(typeId, targetId);
    }
}
