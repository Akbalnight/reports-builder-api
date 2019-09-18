package com.dias.services.reports.mocks;

import java.util.List;

/**
 * Слушателеь нотификаций
 * Позволяет отслеживать события отправки уведомлений
 */
public interface ITestNotificationListener {
    void sendNotification(int typeId, String[] objects, List<Integer> receivers, String targetId, Integer initiatorId);
}
