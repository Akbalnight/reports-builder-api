package com.dias.services.reports.mocks;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class TestNotificationMessage {
    int typeId;
    String[] objects;
    List<Integer> receivers;
    String targetId;
    Integer initiatorId;
}
