package com.dias.services.reports.mocks;

import com.dias.services.notifications.interfaces.INotificationsService;

import java.util.List;

/**
 * Мок сервис уведомлений
 */
public class TestNotificationService implements INotificationsService {

    private ITestNotificationListener listener;

    public void setNotificationListener(ITestNotificationListener listener) {
        this.listener = listener;
    }

    @Override
    public void sendNotification(int typeId, String[] objects, List<Integer> receivers, String targetId, Integer initiatorId) {
        if (listener != null) {
            listener.sendNotification(typeId, objects, receivers, targetId,  initiatorId);
        }
    }

    @Override
    public void stopNotifications(int typeId, List<Integer> receivers, String targetId) {

    }
}
