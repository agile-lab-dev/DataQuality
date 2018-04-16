DROP TABLE IF EXISTS meta_metric CASCADE;
CREATE TABLE meta_metric (
  name TEXT NOT NULL,
  type TEXT NOT NULL,
  description TEXT,
  is_statusable BOOL DEFAULT FALSE,
  is_multicolumn BOOL DEFAULT FALSE,
  PRIMARY KEY (name)
);

insert into meta_metric (name, type, is_statusable, is_multicolumn, description) values
  ('ROW_COUNT','FILE',FALSE,FALSE,
    'Calculates amount of rows in the source.'),
  ('COLUMN_EQ','COLUMN',TRUE,TRUE,
    'Calculates amount of rows with the equal values in the specified column'),
  ('DAY_DISTANCE','COLUMN',TRUE,TRUE,
    'Calculates amount of date rows with cross-column distance within specified threshold'),
  ('LEVENSHTEIN_DISTANCE','COLUMN',TRUE,TRUE,
    'Calculates amount of rows with cross-column Levenshtein distance within specified threshold'),
  ('DISTINCT_VALUES','COLUMN',TRUE,FALSE,
    'Returns amount of distinct values inside of the column (for small variance of data) '),
  ('APPROXIMATE_DISTINCT_VALUES','COLUMN',TRUE,FALSE,
    'Returns approx amount of distinct values inside of the column (for small variance of data) '),
  ('NULL_VALUES','COLUMN',TRUE,FALSE,'Returns amount of null values'),
  ('EMPTY_VALUES','COLUMN',TRUE,FALSE,'Returns amount of empty values'),
  ('MIN_NUMBER','COLUMN',TRUE,FALSE,'Returns minimum of the column'),
  ('MAX_NUMBER','COLUMN',TRUE,FALSE,'Returns maximum of the column'),
  ('SUM_NUMBER','COLUMN',TRUE,FALSE,'Returns sum of all numerical values in the column'),
  ('AVG_NUMBER','COLUMN',TRUE,FALSE,'Returns average of all numerical value inside of the column'),
  ('STD_NUMBER','COLUMN',TRUE,FALSE,'Returns standard deviation of all numerical value inside of the column'),
  ('MIN_STRING','COLUMN',TRUE,FALSE,'Return string minimum of the column (mostly for comaparing dates)'),
  ('MAX_STRING','COLUMN',TRUE,FALSE,'Returns average string length'),
  ('FORMATTED_DATE','COLUMN',TRUE,FALSE,'Returns amount of values in priovided format'),
  ('FORMATTED_NUMBER','COLUMN',TRUE,FALSE,'Returns amount of values in priovided format'),
  ('FORMATTED_STRING','COLUMN',TRUE,FALSE,'Returns amount of values in priovided format'),
  ('CASTED_NUMBER','COLUMN',TRUE,FALSE,'Returns amount of custable values inside column'),
  ('NUMBER_IN_DOMAIN','COLUMN',TRUE,FALSE,'Returns amount of numerical values in provided domain '),
  ('NUMBER_OUT_DOMAIN','COLUMN',TRUE,FALSE,'Returns amount of numerical values outside of provided domain '),
  ('STRING_IN_DOMAIN','COLUMN',TRUE,FALSE,'Returns amount of values in provided domain '),
  ('STRING_OUT_DOMAIN','COLUMN',TRUE,FALSE,'Returns amount of values outside of provided domain'),
  ('STRING_VALUES','COLUMN',TRUE,FALSE,'Return how many times string is present in the column'),
  ('NUMBER_VALUES','COLUMN',TRUE,FALSE,'Return how many times number is present in the column'),
  ('MEDIAN_VALUE','COLUMN',TRUE,FALSE,'Returns median (second quantile) value of numerical column'),
  ('FIRST_QUANTILE','COLUMN',TRUE,FALSE,'Returns first quantile of numerical column'),
  ('THIRD_QUANTILE','COLUMN',TRUE,FALSE,'Returns third quantile of numerical column'),
  ('GET_QUANTILE','COLUMN',TRUE,FALSE,'Returns custom quantile'),
  ('GET_PERCENTILE','COLUMN',TRUE,FALSE,'Return custom percentile '),
  ('TOP_N','COLUMN',TRUE,FALSE,'Returns approximate TOP N frequent values inside of the column  ');


DROP TABLE IF EXISTS meta_metric_parameters CASCADE;
CREATE TABLE meta_metric_parameters (
  metric TEXT NOT NULL,
  name TEXT NOT NULL,
  type TEXT NOT NULL,
  description TEXT,
  is_optional BOOL DEFAULT FALSE,
  UNIQUE(metric, name),
  FOREIGN KEY (metric) REFERENCES meta_metric (name) ON UPDATE CASCADE ON DELETE CASCADE
);

-- todo FORMATS as enum: DOUBLE, INTEGER, DATE_FORMAT, STRING, METRIC
insert into meta_metric_parameters (metric, name, type, is_optional, description) values
  ('LEVENSHTEIN_DISTANCE','threshold', 'DOUBLE', FALSE, 'required threshold (from 0 to 1).'),
  ('DAY_DISTANCE','threshold','INTEGER',FALSE, 'required threshold (amount of days).'),
  ('DAY_DISTANCE','dateFormat','DATE_FORMAT', FALSE, 'date format (YYYYmmDD)'),
  ('APPROXIMATE_DISTINCT_VALUES','accuracyError','DOUBLE', TRUE, 'Less is better '),
  ('FORMATTED_DATE','dateFormat','DATE_FORMAT', FALSE, 'Format for date'),
  ('FORMATTED_NUMBER','precision','DOUBLE', FALSE, 'Provided precision '),
  ('FORMATTED_NUMBER','scale','DOUBLE', FALSE, 'Provided scale'),
  ('FORMATTED_STRING','length','STRING', FALSE, 'Length for strings'),
  ('NUMBER_IN_DOMAIN','domain','STRING', FALSE, 'Domain for checking'),
  ('NUMBER_OUT_DOMAIN','domain','STRING', FALSE, 'Domain for checking'),
  ('STRING_IN_DOMAIN','domain','STRING', FALSE, 'Domain for checking (example {domain: "NST:VST:BTH:ANY"})'),
  ('STRING_OUT_DOMAIN','domain','STRING', FALSE, 'Returns amount of values outside of provided domain'),
  ('STRING_VALUES','compareValue','STRING', FALSE, 'String for search'),
  ('NUMBER_VALUES','compareValue','STRING', FALSE, 'Number for search '),
  ('MEDIAN_VALUE','accuracyError','DOUBLE', TRUE, 'Less is more '),
  ('FIRST_QUANTILE','accuracyError','DOUBLE', TRUE, 'Less is more '),
  ('THIRD_QUANTILE','accuracyError','DOUBLE', TRUE, 'Less is more '),
  ('GET_QUANTILE','accuracyError','DOUBLE', TRUE, 'Less is more '),
  ('GET_QUANTILE','targetSideNumber','DOUBLE', FALSE, 'Provided value for quantile (should be in [0,1]) '),
  ('GET_PERCENTILE','accuracyError','DOUBLE', TRUE, 'Less is more '),
  ('GET_PERCENTILE','targetSideNumber','DOUBLE', FALSE, 'Provided value for quantile (should be in [0,1]) '),
  ('TOP_N','maxCapacity','INTEGER', TRUE, 'Size of aggregation collection '),
  ('TOP_N','targetNumber','INTEGER', FALSE, 'Requested N');