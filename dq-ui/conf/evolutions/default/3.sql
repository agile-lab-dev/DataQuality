DROP TABLE IF EXISTS meta_check CASCADE;
CREATE TABLE meta_check (
  name TEXT NOT NULL,
  type TEXT NOT NULL,
  description TEXT,
  with_metric BOOLEAN DEFAULT FALSE,
  PRIMARY KEY (name, with_metric)
);

insert into meta_check (name, type, with_metric, description) values
  ('GREATER_THAN','snapshot', FALSE,
    'Checks if the metric result is greater than required threshold.'),
  ('GREATER_THAN','snapshot', TRUE,
    'Checks if the metric result is greater than other metric result.'),
  ('LESS_THAN','snapshot', FALSE,
    'Checks if the metric result is less than required threshold.'),
  ('LESS_THAN','snapshot', TRUE,
    'Checks if the metric result is less than other metric result.'),
  ('EQUAL_TO','snapshot', FALSE,
    'Checks if the metric result is equal to required threshold.'),
  ('EQUAL_TO','snapshot', TRUE,
    'Checks if the metric result is equal to other metric result.'),
  ('COUNT_EQ_ZERO','sql', FALSE,
    'Checks if query returns value equal to 0.'),
  ('AVERAGE_BOUND_FULL_CHECK','trend', FALSE,
    'Checks if the result is in [avg*(1-threshold),avg(1+threshold].'),
  ('AVERAGE_BOUND_LOWER_CHECK','trend', FALSE,
    'Checks if the result is bigger than avg*(1-threshold).'),
  ('AVERAGE_BOUND_UPPER_CHECK','trend', FALSE,
    'Checks if the result is lesser than avg*(1-threshold).'),
  ('TOP_N_RANK_CHECK','trend', FALSE,
    'Checks if Jaccard distance between current and selected result is less than threshold.');

DROP TABLE IF EXISTS meta_check_parameters CASCADE;
CREATE TABLE meta_check_parameters (
  check_name TEXT NOT NULL,
  with_metric BOOLEAN NOT NULL,
  name TEXT NOT NULL,
  type TEXT NOT NULL,
  description TEXT,
  UNIQUE(check_name, name, with_metric),
  FOREIGN KEY (check_name, with_metric) REFERENCES meta_check (name, with_metric) ON UPDATE CASCADE ON DELETE CASCADE
);

insert into meta_check_parameters (check_name, with_metric, name, type, description) values
  ('GREATER_THAN', FALSE, 'threshold', 'DOUBLE', 'required threshold'),
  ('GREATER_THAN', TRUE, 'compareMetric','METRIC', 'result of another metric to compare with'),
  ('LESS_THAN', FALSE, 'threshold', 'DOUBLE', 'required threshold'),
  ('LESS_THAN', TRUE, 'compareMetric','METRIC', 'result of another metric to compare with'),
  ('EQUAL_TO', FALSE, 'threshold', 'DOUBLE', 'required threshold'),
  ('EQUAL_TO', TRUE, 'compareMetric','METRIC', 'result of another metric to compare with'),
  ('AVERAGE_BOUND_FULL_CHECK', FALSE, 'threshold', 'PROPORTION', 'required threshold'),
  ('AVERAGE_BOUND_FULL_CHECK', FALSE, 'timewindow', 'INTEGER', 'required time window'),
  ('AVERAGE_BOUND_UPPER_CHECK', FALSE, 'threshold', 'PROPORTION', 'required threshold'),
  ('AVERAGE_BOUND_UPPER_CHECK', FALSE, 'timewindow', 'INTEGER', 'required time window'),
  ('AVERAGE_BOUND_LOWER_CHECK', FALSE, 'threshold', 'PROPORTION', 'required threshold'),
  ('AVERAGE_BOUND_LOWER_CHECK', FALSE, 'timewindow', 'INTEGER', 'required time window'),
  ('TOP_N_RANK_CHECK', FALSE, 'threshold', 'PROPORTION', 'required threshold'),
  ('TOP_N_RANK_CHECK', FALSE, 'targetNumber', 'INTEGER', 'required threshold'),
  ('TOP_N_RANK_CHECK', FALSE, 'timewindow', 'INTEGER', 'required time window');

DROP TABLE IF EXISTS meta_trend_check_rules CASCADE;
CREATE TABLE meta_trend_check_rules (
  name TEXT NOT NULL,
  description TEXT
);

INSERT INTO meta_trend_check_rules values
  ('record', 'Select N metric results from the current date. Time window param is amount of records'),
  ('date', 'Select all metric results within window [(curr.date - timewindow), curr.date]. Time window param is amount of days.');