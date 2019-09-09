package com.dias.services.notifications;

/**
 * NotifificationsData.java
 * Date: 29 июл. 2019 г.
 * Users: vmeshkov
 * Description: Список уведомлений
 */
public enum NotifificationsData {
    FILTER_CREATED(5), // Сформирован фильтр
    FILTER_UPDATED(6), // Изменены параметры фильтра
    FILTER_RENAMED(7), // Фильтр переименован
    DIRECTORY_CREATED(16), // Справочник сформирован
    DIRECTORY_UPDATED(17), // Справочник изменен
    DIRECTORY_DELETED(18), // Справочник удален
    DIRECTORY_IMPORTED(19), // Справочник импортирован
    DIRECTORY_EXPORTED(20), // Справочник экспортирован
    METERING_REPORT_CREATED(30), // Сформирована ведомость
    METERING_REPORT_DELETED(31), // Удалена ведомость
    METERING_REPORT_UPDATED(32), // Сброс данных ведомости
    SUBST_AUTO_GENERATED(34),  // Расчет замещающих значений автоматически сформирован
    SUBST_ALG_GENERATED(35), // Расчет замещающих значений автоматически сформирован с помощью алгоритма
    SUBST_CHANGED(36), // Изменен расчет замещающих значений
    SUBST_DELETED(37), // Удален расчет замещающих значений
    SUBST_REQ_MANUAL_INPUT(38), //Требуется ввод значений вручную для расчета замещающих значений
    ACT_CREATED(39), // Сформирован акт
    ACT_UPDATED(40), // Изменен акт
    ACT_DELETED(41), // Удален акт
    ACT_MANUAL_INPUT_REQUIRED(42), // Требуется ввод значений вручную для формирования акта
    ACT_TIME_REMAINING_TO_FINALIZE(43), // Осталось времени до финализации акта
    SUBST_APPROVMENT_CREATED(44), // Запущен процесс согласования расчета замещающих значений
    SUBST_NEED_APPROVE(45), // Требуется согласование расчета замещающего значения текущего пользователя
    SUBST_STATUS(46), // Изменен статус согласования расчета замещающего значения
    SUBST_APPROVED(47), // Расчет замещающего значения согласован согласующим
    ACT_APPROVMENT_CREATED(48), // Запущен процесс согласования акта
    ACT_NEED_APPROVE(49), // Требуется согласование текущего пользователя
    ACT_STATUS(50), // Изменен статус согласования акта или акт финализирован
    ACT_APPROVED(51), // Акт согласован согласующим
    APPROVERS_ADDED(52), // Добавлен новый список согласующих
    APPROVERS_UPDATED(53), // Отредактирован список согласующих
    APPROVERS_RENAMED(54), // Переименован список согласующих
    APPROVERS_REMOVED(55); // Удален список согласующих

    private int id;

    private NotifificationsData(int id) {
        this.id = id;
    }

    public int value() {
        return id;
    }
}
