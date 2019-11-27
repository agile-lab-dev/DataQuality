DROP SCHEMA IF EXISTS dev;
CREATE SCHEMA dev;

DROP TABLE IF EXISTS dev.results_metric_columnar;
CREATE TABLE dev.results_metric_columnar (
  "metric_id"	TEXT NOT NULL,
  "source_date"	TEXT NOT NULL,
  "name"	TEXT NOT NULL,
  "source_id"	TEXT NOT NULL,
  "column_names"	TEXT[] NOT NULL,
  "params"	TEXT,
  "result"	TEXT NOT NULL,
  "additional_result" TEXT,
  UNIQUE(metric_id, source_date)
);

DROP TABLE IF EXISTS dev.results_metric_file;
CREATE TABLE dev.results_metric_file (
  "metric_id"	TEXT NOT NULL,
  "source_date"	TEXT NOT NULL,
  "name"	TEXT NOT NULL,
  "source_id"	TEXT NOT NULL,
  "result"	TEXT NOT NULL,
  "additional_result" TEXT,
  UNIQUE(metric_id, source_date)
);

DROP TABLE IF EXISTS dev."results_metric_composed";
CREATE TABLE dev."results_metric_composed" (
  "metric_id"	TEXT NOT NULL,
  "source_date"	TEXT NOT NULL,
  "name"	TEXT NOT NULL,
  "source_id"	TEXT NOT NULL,
  "formula"	TEXT NOT NULL,
  "result"	TEXT NOT NULL,
  "additional_result" TEXT,
  UNIQUE(metric_id, source_date)
);

DROP TABLE IF EXISTS dev."results_check";
CREATE TABLE dev."results_check" (
  "check_id" TEXT NOT NULL,
  "check_name" TEXT NOT NULL,
  "description" TEXT,
  "checked_file" TEXT NOT NULL,
  "base_metric" TEXT NOT NULL,
  "compared_metric" TEXT,
  "compared_threshold" TEXT,
  "status" TEXT NOT NULL,
  "message" TEXT,
  "exec_date" TEXT NOT NULL,
  UNIQUE(check_id, exec_date, check_name)
);



CREATE OR REPLACE FUNCTION dev_upsert_colmet()
  RETURNS trigger AS
$dev_upsert_colmet$
declare
existing record;
begin
  if (select EXISTS (SELECT 1 FROM dev.results_metric_columnar WHERE
    metric_id = NEW.metric_id AND
    source_date = NEW.source_date
  )) then

  UPDATE dev.results_metric_columnar SET
    name = NEW.name,
    column_names = NEW.column_names,
    params = NEW.params,
    result = NEW.result,
    source_id = NEW.source_id,
    additional_result = NEW.additional_result
  WHERE metric_id = NEW.metric_id AND source_date = NEW.source_date;

  return null;
  end if;

  return new;
end
$dev_upsert_colmet$
LANGUAGE plpgsql;

create trigger dev_column_metrics_insert
before insert
  on dev.results_metric_columnar
for each row
  execute procedure dev_upsert_colmet();



CREATE OR REPLACE FUNCTION dev_upsert_filemet()
RETURNS trigger AS
$dev_upsert_filemet$
declare
existing record;
begin
if (select EXISTS (SELECT 1 FROM dev.results_metric_file WHERE metric_id = NEW.metric_id AND source_date = NEW.source_date)) then

UPDATE dev.results_metric_file SET
  name = NEW.name,
  source_id = NEW.source_id,
  result = NEW.result,
  additional_result = NEW.additional_result
WHERE metric_id = NEW.metric_id AND source_date = NEW.source_date;

return null;
end if;

return new;
end
$dev_upsert_filemet$
LANGUAGE plpgsql;

create trigger dev_file_metrics_insert
before insert
  on dev.results_metric_file
for each row
  execute procedure dev_upsert_filemet();



CREATE OR REPLACE FUNCTION dev_upsert_compmet()
RETURNS trigger AS
$dev_upsert_compmet$
declare
existing record;
begin
if (select EXISTS (SELECT 1 FROM results_metric_composed WHERE metric_id = NEW.metric_id AND source_date = NEW.source_date)) then

UPDATE results_metric_composed SET
  name = NEW.name,
  source_id = NEW.source_id,
  formula = NEW.formula,
  result = NEW.result,
  additional_result = NEW.additional_result
WHERE metric_id = NEW.metric_id AND source_date = NEW.source_date;

return null;
end if;

return new;
end
$dev_upsert_compmet$
LANGUAGE plpgsql;

create trigger dev_composed_metrics_insert
before insert
  on dev.results_metric_composed
for each row
  execute procedure dev_upsert_compmet();



CREATE OR REPLACE FUNCTION dev_upsert_check()
RETURNS trigger AS
$dev_upsert_check$
declare
existing record;
begin
if (select EXISTS (SELECT 1 FROM dev.results_check
WHERE check_id = NEW.check_id AND exec_date = NEW.exec_date AND check_name = NEW.check_name)) then

UPDATE dev.results_check SET
  description = NEW.description,
  checked_file = NEW.checked_file,
  base_metric = NEW.base_metric,
  compared_metric = NEW.compared_metric,
  compared_threshold = NEW.compared_threshold,
  status = NEW.status,
  message = NEW.message
WHERE check_id = NEW.check_id AND
      exec_date = NEW.exec_date AND
      check_name = NEW.check_name;

return null;
end if;

return new;
end
$dev_upsert_check$
LANGUAGE plpgsql;

create trigger dev_checks_insert
before insert
  on dev.results_check
for each row
  execute procedure dev_upsert_check();