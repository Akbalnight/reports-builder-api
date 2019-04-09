SELECT databasechangelog.filename as "Имя файла" FROM public.databasechangelog databasechangelog WHERE databasechangelog.filename not in ('1','2')
