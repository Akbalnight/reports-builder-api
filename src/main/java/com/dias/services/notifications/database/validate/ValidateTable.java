package com.dias.services.notifications.database.validate;

import com.dias.services.notifications.database.validate.resource.IResource;
import com.dias.services.notifications.database.validate.validatedata.IValidateDataInTable;

/**
 * ValidateTable.java
 * Date: 19 февр. 2019 г.
 * Users: vmeshkov
 * Description: TODO
 */
public class ValidateTable {
    /**
     * Ресурс, проверяющий целостность таблицы
     */
    private IResource validateResource;
    /**
     * Валидация данных в таблице
     */
    private IValidateDataInTable validateRows;
    /**
     * Ресурс для создания таблицы
     */
    private IResource createResource;

    public ValidateTable(IResource validateResource, IValidateDataInTable validateRows,
                         IResource createResource) {
        this.validateResource = validateResource;
        this.validateRows = validateRows;
        this.createResource = createResource;
    }

    public IResource getValidateResource() {
        return validateResource;
    }

    public void setValidateResource(IResource validateResource) {
        this.validateResource = validateResource;
    }

    public IValidateDataInTable getValidateRows() {
        return validateRows;
    }

    public void setValidateRows(IValidateDataInTable validateRows) {
        this.validateRows = validateRows;
    }

    public IResource getCreateResource() {
        return createResource;
    }

    public void setCreateResource(IResource createResource) {
        this.createResource = createResource;
    }
}
