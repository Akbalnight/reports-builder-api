create table history_values_by_day (
    id bigint primary key,
    abnormal_context character varying(255),
    abnormal_worktime numeric(19,2),
    abnormal_worktime_all_time numeric(19,2),
    baro_press numeric(19,2),
    coldwater_temp numeric(19,2),
    date_from timestamp without time zone,
    date_to timestamp without time zone,
    enthalpy_coldwater numeric(19,2),
    enthalpy_cond_return numeric(19,2),
    enthalpy_return numeric(19,2),
    enthalpy_return_awerage numeric(19,2),
    enthalpy_steam numeric(19,2),
    enthalpy_supply numeric(19,2),
    heat_cold_water numeric(19,2),
    heat_cond_pipe_return_koef numeric(19,2),
    heat_cond_return numeric(19,2),
    heat_cond_return_koef numeric(19,2),
    heat_energy_cond_return_all_time numeric(19,2),
    heat_energy_feed_all_time numeric(19,2),
    heat_energy_pipe_all_time numeric(19,2),
    heat_energy_return_all_time numeric(19,2),
    heat_energy_steam_all_time numeric(19,2),
    heat_energy_steam_pipe_all_time numeric(19,2),
    heat_energy_supply_all_time numeric(19,2),
    heat_feed numeric(19,2),
    heat_hot_steam numeric(19,2),
    heat_pipe numeric(19,2),
    heat_return numeric(19,2),
    heat_steam numeric(19,2),
    heat_steam_pipe numeric(19,2),
    heat_supply numeric(19,2),
    outdoor_temp numeric(19,2),
    pipeline_negative numeric(19,2),
    pipeline_positive numeric(19,2),
    pipeline_temp_diff numeric(19,2),
    press_cond_return numeric(19,2),
    press_feed numeric(19,2),
    press_return numeric(19,2),
    press_steam numeric(19,2),
    press_supply numeric(19,2),
    regular_worktime numeric(19,2),
    regular_worktime_all_time numeric(19,2),
    return_rate numeric(19,2),
    temp_cond_return numeric(19,2),
    temp_feed numeric(19,2),
    temp_return numeric(19,2),
    temp_steam numeric(19,2),
    temp_supply numeric(19,2),
    time_get_data timestamp without time zone,
    time_read_data timestamp without time zone,
    time_write_data timestamp without time zone,
    vol_cond_return numeric(19,2),
    vol_cond_return_all_time numeric(19,2),
    vol_cond_return_incl_koef numeric(19,2),
    vol_feed numeric(19,2),
    vol_feed_all_time numeric(19,2),
    vol_return numeric(19,2),
    vol_return_all_time numeric(19,2),
    vol_steam numeric(19,2),
    vol_steam_all_time numeric(19,2),
    vol_supply numeric(19,2),
    vol_supply_all_time numeric(19,2),
    weight_cond_return numeric(19,2),
    weight_cond_return_all_time numeric(19,2),
    weight_cond_return_incl_koef numeric(19,2),
    weight_feed numeric(19,2),
    weight_feed_all_time numeric(19,2),
    weight_return numeric(19,2),
    weight_return_all_time numeric(19,2),
    weight_steam numeric(19,2),
    weight_steam_all_time numeric(19,2),
    weight_supply numeric(19,2),
    weight_supply_all_time numeric(19,2),
    metering_device_id bigint,
    pipeline_id bigint,
    statement_id bigint
);
INSERT INTO history_values_by_day (id, abnormal_context, abnormal_worktime, abnormal_worktime_all_time, baro_press, coldwater_temp, date_from, date_to, enthalpy_coldwater, enthalpy_cond_return, enthalpy_return, enthalpy_return_awerage, enthalpy_steam, enthalpy_supply, heat_cold_water, heat_cond_pipe_return_koef, heat_cond_return, heat_cond_return_koef, heat_energy_cond_return_all_time, heat_energy_feed_all_time, heat_energy_pipe_all_time, heat_energy_return_all_time, heat_energy_steam_all_time, heat_energy_steam_pipe_all_time, heat_energy_supply_all_time, heat_feed, heat_hot_steam, heat_pipe, heat_return, heat_steam, heat_steam_pipe, heat_supply, outdoor_temp, pipeline_negative, pipeline_positive, pipeline_temp_diff, press_cond_return, press_feed, press_return, press_steam, press_supply, regular_worktime, regular_worktime_all_time, return_rate, temp_cond_return, temp_feed, temp_return, temp_steam, temp_supply, time_get_data, time_read_data, time_write_data, vol_cond_return, vol_cond_return_all_time, vol_cond_return_incl_koef, vol_feed, vol_feed_all_time, vol_return, vol_return_all_time, vol_steam, vol_steam_all_time, vol_supply, vol_supply_all_time, weight_cond_return, weight_cond_return_all_time, weight_cond_return_incl_koef, weight_feed, weight_feed_all_time, weight_return, weight_return_all_time, weight_steam, weight_steam_all_time, weight_supply, weight_supply_all_time, metering_device_id, pipeline_id, statement_id) VALUES (16331, 'Пожар', 1, 0.00, null, 2.70, '2018-03-23 00:00:00.000000', '2018-03-24 00:00:00.000000', null, null, 61.64, null, null, 108.54, null, null, null, null, null, null, 999246.37, null, null, null, null, null, null, 1794.94, null, null, null, null, null, -109.89, 2545.89, 46.54, null, null, 2.37, null, 7.65, 24.00, 3610.59, null, null, null, 61.20, null, 107.78, null, '2018-09-24 09:24:46.643158', null, null, null, null, null, null, 36462.00, 21634695.05, null, null, 40146.00, 23819824.03, null, null, null, null, null, 35814.00, 21445518.00, null, null, 38250.00, 22732220.00, 0, 1, 976);
-- Ведомость дневная
create table if not exists day_statement (
  id bigserial primary key,
  name_statement character varying(255),
  protocol_id bigint,
  create_time timestamp without time zone,
  consumer character varying(255),
  date_from timestamp without time zone,
  date_to timestamp without time zone,
  formula_calculating_heat_energy character varying(255),
  generation_object_full_name character varying(255),
  generation_object_short_name character varying(255),
  generation_object_address character varying(255),
  heat_system character varying(255),
  number_md numeric(19,2),
  pipeline_name character varying(255),
  highway_name character varying(255),
  step_save character varying(255),
  substition_variations character varying(255),
  final_footer_row_id bigint,
  final_report_row_id bigint,
  generation_object_id bigint,
  pipeline_id bigint,
  highway_id bigint,
  type_pipeline_id bigint
);
INSERT INTO day_statement (id, name_statement, GENERATION_OBJECT_ID) values (976, 'Ведомость номер 1', 1)

