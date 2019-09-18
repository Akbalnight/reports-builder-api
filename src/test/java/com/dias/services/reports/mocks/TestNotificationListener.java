package com.dias.services.reports.mocks;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class TestNotificationListener implements ITestNotificationListener {

    private List<TestNotificationMessage> messages = new ArrayList<>();

    @Override
    public void sendNotification(int typeId, String[] objects, List<Integer> receivers, String targetId, Integer initiatorId) {
        messages.add(TestNotificationMessage.builder().typeId(typeId).objects(objects).receivers(receivers).targetId(targetId).initiatorId(initiatorId).build());
    }
}
