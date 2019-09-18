delete from {scheme.name}.notifications_data;
insert into {scheme.name}.notifications_data(id, commonTypeId, description, email_title, email_body, props) values(23, 3, 'Отчет %s добавлен в избранное', null, null, cast('{}' as json));
insert into {scheme.name}.notifications_data(id, commonTypeId, description, email_title, email_body, props) values(24, 3, 'Отчет %s добавлен в публичные', null, null, cast('{"receivers":{"receivers":[-1]}}' as json));
insert into {scheme.name}.notifications_data(id, commonTypeId, description, email_title, email_body, props) values(25, 3, 'Отчет %s добавлен в личные', null, null, cast('{}' as json));
insert into {scheme.name}.notifications_data(id, commonTypeId, description, email_title, email_body, props) values(26, 3, 'Отчет %s отредактирован', null, null, cast('{}' as json));
insert into {scheme.name}.notifications_data(id, commonTypeId, description, email_title, email_body, props) values(27, 3, 'Отчет %s удален', null, null, cast('{}' as json));
insert into {scheme.name}.notifications_data(id, commonTypeId, description, email_title, email_body, props) values(28, 3, 'Экспорт отчета %s завершен, отчет сохранен в %s', null, null, cast('{}' as json));
insert into {scheme.name}.notifications_data(id, commonTypeId, description, email_title, email_body, props) values(29, 3, 'Отчет %s отправлен на печать', null, null, cast('{}' as json));
