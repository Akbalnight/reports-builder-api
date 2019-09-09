package com.dias.services.core;

import java.util.Optional;

/**
 * Details.java
 * Date: 8 окт. 2018 г.
 * Users: vmeshkov
 * Description: Содержит данные о пользователе и идентификаторе сессии из шины
 */
public class Details {
    private static final String UNKNOWN = "unknown";
    private static final ThreadLocal<Details> detailsHolder = new InheritableThreadLocal<>();

    /**
     * Идентификатор сессии из шины
     */
    private String sessionId;
    /**
     * Идентификатор пользователя
     */
    private String userId;

    public Details(String sessionId, String userId) {
        super();
        this.sessionId = Optional.ofNullable(sessionId).orElse(UNKNOWN);
        this.userId = Optional.ofNullable(userId).orElse(UNKNOWN);
    }

    public String getSessionId() {
        return sessionId;
    }

    // TODO безопасность
    public Long getUserId() {
        if (userId != null && !userId.equalsIgnoreCase(UNKNOWN)) {
            try {
                return Long.parseLong(userId);
            } catch (NumberFormatException e) {
                // throw new IllegalArgumentException("Идентификатор пользователя имеет неправильный формат.");
            }
        }
        return null;
    }

    /**
     * Заполнить данные по запросу
     *
     * @param sessionId - идентификатор сессии из шины
     * @param userId    - идентификатор пользователя
     */
    public static void setDetails(String sessionId, String userId) {
        detailsHolder.set(new Details(sessionId, userId));
    }

    /**
     * Получить данные из потока
     *
     * @return данные
     */
    public static Details getDetails() {
        return Optional.ofNullable(detailsHolder.get()).orElse(new Details(UNKNOWN, UNKNOWN));
    }
}


