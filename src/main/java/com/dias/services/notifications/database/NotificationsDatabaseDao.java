package com.dias.services.notifications.database;

import com.dias.services.notifications.database.validate.ValidateDao;
import com.dias.services.notifications.database.validate.ValidateTable;
import com.dias.services.notifications.database.validate.resource.SQLScript;
import com.dias.services.notifications.database.validate.resource.SQLText;
import com.dias.services.notifications.database.validate.validatedata.SQLValidateDataInTable;
import com.dias.services.notifications.database.validate.validatedata.strategy.RefillValidateStrategy;
import com.dias.services.notifications.interfaces.INotificationsDao;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * NotificationsDatabaseDao.java
 * Date: 25 июл. 2019 г.
 * Users: vmeshkov
 * Description: Имплементация работы с уведомлениями с базой данных
 */
public class NotificationsDatabaseDao extends ValidateDao implements INotificationsDao {

    private static Logger LOG = Logger.getLogger(NotificationsDatabaseDao.class.getName());

    private final class NotificationData {
        public String description;
        public String emailTitle;
        public String emailBody;
    }

    /**
     * SQL проверки целостности таблицы sendout
     */
    private static final String SQL_VALIDATE_SENDOUT = "select "
                + "         \"time\","
                + "         id,"
                + "         idinitiator,"
                + "         typeId,"
                + "         objectId,"
                + "         objects,"
                + "         sended "
                + "from {scheme.name}.sendout";

    /**
     * SQL проверки целостности таблицы notifications_data
     */
    private static final String SQL_VALIDATE_NOTIFICATIONS_DATA = "select "
                + "        id,"
                + "        commonTypeId,"
                + "        description,"
                + "        email_title,"
                + "        email_body,"
                + "        props "
                + "from {scheme.name}.notifications_data";

    /**
     * SQL проверки заполнения таблицы notifications_data
     */
    private static final String SQL_VALIDATE_CONTENT_NOTIFICATIONS_DATA = "select id "
                + "from {scheme.name}.notifications_data";

    /**
     * SQL проверки целостности таблицы notification_category
     */
    private static final String SQL_VALIDATE_NOTIFICATIONS_CATEGORIES = "select "
                + "        id,"
                + "        value "
                + "from {scheme.name}.notification_category";

    /**
     * SQL проверки заполнения таблицы notification_category
     */
    private static final String SQL_VALIDATE_CONTENT_NOTIFICATIONS_CATEGORIES = "select id "
                + "from {scheme.name}.notification_category";

    /**
     * SQL проверки целостности таблицы notifications
     */
    private static final String SQL_VALIDATE_NOTIFICATIONS = "select "
                + "         \"time\","
                + "         id,"
                + "         idinitiator,"
                + "         idUser,"
                + "         idObject,"
                + "         idType,"
                + "         idParent,"
                + "         status,"
                + "         description "
                + "from {scheme.name}.notifications";

    /**
     * SQL проверки целостности таблицы emails
     */
    private static final String SQL_VALIDATE_EMAILS = "select "
                + "         id,"
                + "         \"time\","
                + "         title,"
                + "         body,"
                + "         receivers,"
                + "         sended " + "from {scheme.name}.emails";

    /**
     * SQL добавления уведломления
     */
    private static final String SQL_INSERT_NOTIFICATION = "INSERT INTO {scheme.name}.notifications("
                + "         idUser,"
                + "         idObject,"
                + "         idType,"
                + "         idParent,"
                + "         description,"
                + "         idinitiator) "
                + "VALUES("
                + "         ?,"
                + "         ?,"
                + "         ?,"
                + "         ?,"
                + "         ?,"
                + "         ?)";

    /**
     * SQL добавления рассылки
     */
    private static final String SQL_INSERT_SENDOUT = "INSERT INTO {scheme.name}.sendout("
                + "         typeId,"
                + "         objectId,"
                + "         objects,"
                + "         idinitiator) "
                + "VALUES(:typeId,"
                + "       :objectId,"
                + "       cast(:objects AS JSON),"
                + "       :idinitiator)";

    /**
     * Удалить уведомления
     */
    private static final String SQL_DELETE_NOTIFICATIONS = "DELETE "
                + "FROM {scheme.name}.notifications "
                + "WHERE idType = :typeId AND "
                + "idObject = :objectId AND "
                + "(idUser = ANY(:receivers::INT[]) OR "
                + "-1 = ANY(:receivers::INT[]))";

    /**
     * Удалить уведомления
     */
    private static final String SQL_DELETE_SENDOUTS = "DELETE "
                + "FROM {scheme.name}.sendout "
                + "WHERE typeid = :typeId AND "
                + "objectid = :objectId";

    /**
     * SQL запрос добавление письма
     */
    private static final String SQL_ADD_EMAIL = "INSERT "
                + "INTO {scheme.name}.emails "
                + "(title,"
                + "body,"
                + "receivers) "
                + "VALUES("
                + ":title,"
                + ":body,"
                + "cast(:receivers AS JSON))";

    /**
     * Получить список данных: текст нотификации, заголовок и текст письма
     */
    private static final String SQL_NOTIFICATIONS_DATA = "SELECT "
                + "id,"
                + "description,"
                + "email_title,"
                + "email_body "
                + "FROM {scheme.name}.notifications_data";

    private ObjectMapper objectMapper;

    private NamedParameterJdbcTemplate jdbcTemplate;

    private String schemeName;


    public NotificationsDatabaseDao(ObjectMapper objectMapper,
                                    String schemeName,
                                    NamedParameterJdbcTemplate jdbcTemplate) {
        this.objectMapper = objectMapper;
        this.schemeName = schemeName;
        this.jdbcTemplate = jdbcTemplate;
        init();
    }

    private Map<Integer, NotificationData> mapData = new HashMap<Integer, NotificationData>();

    /**
     * Подключение шаблона для выполнения SQL запросов
     */
    public void init() {

        try {
            validate(new ValidateTable[]
                    {
                            new ValidateTable(new SQLText(SQL_VALIDATE_NOTIFICATIONS, schemeName), null,
                                    new SQLScript(
                                            "/notice_db/db_notifications_create_tables.sql", schemeName)),
                            new ValidateTable(new SQLText(SQL_VALIDATE_NOTIFICATIONS_DATA, schemeName), null,
                                    new SQLScript(
                                            "/notice_db/db_notifications_data_create_tables.sql", schemeName)),
                            new ValidateTable(new SQLText(SQL_VALIDATE_NOTIFICATIONS_CATEGORIES, schemeName), null,
                                    new SQLScript(
                                            "/notice_db/db_notifications_categories_create_tables.sql", schemeName)),
                            new ValidateTable(new SQLText(SQL_VALIDATE_SENDOUT, schemeName), null,
                                    new SQLScript("/notice_db/db_sendout_create_tables.sql", schemeName)),
                            new ValidateTable(new SQLText(SQL_VALIDATE_EMAILS, schemeName), null,
                                    new SQLScript("/notice_db/db_email_create_tables.sql", schemeName))
                    });
        } catch (IOException e) {
            LOG.severe("Ошибка инициализации таблиц для уведомлений: " + e.getMessage());
        }
    }

    @Override
    public void createNotifications(int typeId, String[] objects,
                                    List<Integer> receivers, String targetId, Integer initiatorId) {
        String description = String
                .format(selectNotificationDescription(typeId), (Object[]) objects);
        getJDBCTemplate().getJdbcTemplate().batchUpdate(getSqlFromTemplate(SQL_INSERT_NOTIFICATION, schemeName),
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i)
                            throws SQLException {
                        ps.setLong(1, receivers.get(i));
                        ps.setString(2, targetId);
                        ps.setInt(3, typeId);
                        ps.setObject(4, null);
                        ps.setString(5, description);
                        ps.setObject(6, initiatorId);
                    }

                    @Override
                    public int getBatchSize() {
                        return receivers.size();
                    }
                });

        email(typeId, objects, receivers, initiatorId);
    }

    private String getSqlFromTemplate(String sqlTemplate, String schemeName) {
        return sqlTemplate.replaceAll("\\{scheme.name\\}", schemeName);
    }

    private String selectNotificationDescription(int typeId) {
        return getMapData().get(typeId).description;
    }

    private Map<Integer, NotificationData> getMapData() {
        if (mapData.isEmpty()) {
            createNotificationData();
        }

        return mapData;
    }

    private synchronized void createNotificationData() {
        if (mapData.isEmpty()) {
            jdbcTemplate.query(SQL_NOTIFICATIONS_DATA, (ResultSet rs) -> {
                if (rs.isBeforeFirst()) {
                    return;
                }
                do {
                    NotificationData data = new NotificationData();
                    data.description = rs.getString(2);
                    data.emailTitle = rs.getString(3);
                    data.emailBody = rs.getString(4);
                    mapData.put(rs.getInt(1), data);
                }
                while (rs.next());
            });
        }
    }

    @Override
    public void createSendOut(int typeId, String[] objects, String targetId,
                              Integer initiatorId) {
        MapSqlParameterSource paramSource = new MapSqlParameterSource("typeId", typeId);
        paramSource.addValue("objectId", targetId);
        try {
            paramSource.addValue("objects",
                    objectMapper.writeValueAsString(objects));
        } catch (JsonProcessingException e) {
            // Nothing todo
        }

        paramSource.addValue("idinitiator", initiatorId);
        getJDBCTemplate().update(getSqlFromTemplate(SQL_INSERT_SENDOUT, schemeName), paramSource);
    }

    @Override
    protected NamedParameterJdbcTemplate getJDBCTemplate() {
        return jdbcTemplate;
    }

    @Override
    public void stopNotifications(int typeId, List<Integer> receivers,
                                  String targetId) {
        MapSqlParameterSource paramSource = new MapSqlParameterSource("typeId",
                typeId);
        paramSource.addValue("objectId", targetId);
        paramSource.addValue("receivers",
                receivers.stream().mapToInt(Integer::intValue).toArray());
        getJDBCTemplate().update(getSqlFromTemplate(SQL_DELETE_NOTIFICATIONS, schemeName), paramSource);
    }

    @Override
    public void stopSendOut(int typeId, String targetId) {
        MapSqlParameterSource paramSource = new MapSqlParameterSource("typeId",
                typeId);
        paramSource.addValue("objectId", targetId);
        getJDBCTemplate().update(getSqlFromTemplate(SQL_DELETE_SENDOUTS, schemeName), paramSource);
    }

    private void email(int id, String[] objects, List<Integer> receivers,
                       Integer initiatorId) {
        NotificationData data = getMapData().get(id);
        if (data != null && data.emailBody != null && data.emailTitle != null) {
            MapSqlParameterSource params = new MapSqlParameterSource("title",
                    data.emailTitle);
            String body = String.format(data.emailBody, (Object[]) objects);
            params.addValue("body", initiatorId == null ? body
                    : String.format("%s (<user:%d>)", body, initiatorId));
            try {
                params.addValue("receivers",
                        String.format("{\"receivers\":{\"receivers\":%s}}",
                                objectMapper.writeValueAsString(receivers)));
            } catch (JsonProcessingException e) {
                // Nothing to do
            }

            jdbcTemplate.update(SQL_ADD_EMAIL, params);
        }
    }
}
