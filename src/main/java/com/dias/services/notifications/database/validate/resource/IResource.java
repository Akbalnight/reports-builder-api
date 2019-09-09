package com.dias.services.notifications.database.validate.resource;

import java.io.IOException;

/**
 * IResource.java
 * Date: 23 янв. 2019 г.
 * Users: vmeshkov
 * Description: Интерфейс, который описывает SQL ресурс
 */
public interface IResource {
    public String getSQL() throws IOException;
}
