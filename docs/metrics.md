# Metrics

Metrics are main working unit of DataQuality framework. With them you'll be able to calculate multiple KPIs and get
 an analytical overview of your sources.

> **Tip:** All metrics are returning tuple of [Double, String]. In most of them second part is empty.

Configuration for all metrics is unified and following:
```hocon
{
    id: "1011"
    name: "TOP_N" // metric name from the list
    type: "COLUMN" // base metric type
    description: "median"
    config: {
        file: "USGS_2005", // source name
        columns: ["Magnitude"], // Only for column metrics
        params: {targetNumber:4, maxCapacity: 100} // check metric description for details
    }
}
```

### Composed metrics
Composed metrics are the way to combine multiple metric results. Calculation is going on after calculation of basic metrics.

There are basic math operators (+-*/), powering(^) and grouping features.

Example:
```hocon
{
    id: "0.5_tdigest_check"
    name: "Q2 error"
    description: "Determines null values percentage on column X"
    formula: "$td05 - ($108 - 0.675*$109)" //use $metric_id to pull metric result in formula
}
```

> **Tip:** If you want to use TOP_N metric result in your formula, simply use following template:
 metric_name + _ + rating_position. It will get frequency of the result.

### File metric list
##### "ROW_COUNT"
Returns amount of rows in the file.

Parameters: none


### Special column metric list	

> **Tip:** Require "column" field inside config object

##### "TOP_N"
Returns approximate TOP N frequent values inside of the column 

> **Tip:** "TOP_N" metrics is a special. They are returning multiple results of [Frequency, String] with name
 metric_id + _ + rating_position

Parameters:
- optional Int maxCapacity: Size of aggregation collection
- Int targetNumber: Requested N

##### "TOP_N_ALGEBIRD"
Returns approximate TOP N frequent values inside of the column 

> **Tip:** This metric is using Twitter's Algebird api to use probabilistic data structures. Use this metric in case
 big value variance inside the column.

Parameters:
- optional Int maxCapacity: Size of aggregation collection
- Int targetNumber: Requested N

##### Multicolumn metrics
DataQuality also supports metrics between two columns of one source. All those metrics calculates amount of tuples that
didn't passed metric specific constraint.

Such as:
- "COLUMN_EQ": Value of column A should be equal to value of column B
- "DAY_DISTANCE": Values of col.A and col.B should be in the provided Date format and distance between them should
 not exceed defined distance (in days)
- "LEVENSHTEIN_DISTANCE": Calculated Levenshtein distance between col.A and col.B should not exceed defined threshold.

Example:
```hocon
  {
    id: "DAY_DIST_id0"
    name: "DAY_DISTANCE"
    type: "COLUMN"
    description: "Test example of DAY_DISTANCE"
    config: {
      file: "SURVEY_FEEDBACK"
      columns: ["QUESTIONNAIRE_DATE","ANSWER_DATE"]
      params: {
        threshold:"2"
        dateFormat:"yyyy-MM-dd"
      }
    }
  }
```

### Column metric list

> **Tip:** Require "column" field inside config object

##### "DISTINCT_VALUES"
Returns amount of distinct values inside of the column (for small variance of data)
		
Parameters: none

##### "APPROXIMATE_DISTINCT_VALUES"
Returns approx amount of distinct values inside of the column (for small variance of data)

Parameters: 
- optional Double accuracyError: Less is better

##### "NULL_VALUES"
Returns amount of null values

Parameters: none

##### "EMPTY_VALUES"
Returns amount of empty values

Parameters: none

##### "MIN_NUMBER"
Returns minimum of the column

Parameters: none

##### "MAX_NUMBER"
Returns maximum of the column
		
Parameters: none

##### "SUM_NUMBER"
Returns sum of all numerical values in the column

Parameters: none

##### "SUM_DECIMAL_NUMBER"
Returns sum of all numerical values in the column

In financial use case, this metric is recommended over SUM_NUMBER

Parameters: none

##### "AVG_NUMBER"
Returns average of all numerical value inside of the column

Parameters: none

##### "AVG_DECIMAL_NUMBER"
Returns average of all numerical value inside of the column

In financial use case, this metric is recommended over AVG_NUMBER

Parameters: none

##### "STD_NUMBER"
Return standard deviation of all numerical values inside of the column
		
Parameters: none

##### "STD_DECIMAL_NUMBER"

Return standard deviation of all numerical values inside of the column
		
In financial use case, this metric is recommended over STD_NUMBER

Parameters: none

##### "MIN_STRING"
Return string minimum of the column (mostly for comparing dates)

Parameters: none

##### "MAX_STRING"
Return string maximum of the column (mostly for comparing dates)
		
Parameters: none

##### "AVG_STRING"
Returns average string length

Parameters: none

##### "FORMATTED_DATE"
Returns amount of values in provided format

Parameters:
- String dateFormat: Format for date

#####"FORMATTED_NUMBER"
Returns amount of values in provided format

Parameters:
- Double precision: Provided precision
- Double scale: Provided scale
    
##### "FORMATTED_STRING"
Returns amount of values in provided format

Parameters:
- String length: Length for strings

##### "CASTED_NUMBER"
Returns amount of castable values inside column

Parameters: none

##### "NUMBER_IN_DOMAIN"
Returns amount of numerical values in provided domain
		
Parameters:
- Set String domainSet: Domain for checking

##### "NUMBER_OUT_DOMAIN"
Returns amount of numerical values outside of provided domain

Parameters:
- Set String domainSet: Domain for checking

##### "STRING_IN_DOMAIN"
Returns amount of values in provided domain

Parameters:
- Set String domainSet: Domain for checking (example {domainSet: "NST:VST:BTH:ANY"})

##### "STRING_OUT_DOMAIN"
Returns amount of values outside of provided domain
		
Parameters:
- Set String domainSet: Domain for checking

##### "STRING_VALUES"
Return how many times string is present in the column

Parameters:
- String compareValue: String for search

##### "NUMBER_VALUES"
Return how many times number is present in the column

Parameters:
- Double compareValue: Number for search

##### "MEDIAN_VALUE"
Returns median (second quantile) value of numerical column

Parameters:
- optional Double accuracyError: Less is more

#### "FIRST_QUANTILE"
Returns first quantile of numerical column

Parameters:
- optional Double accuracyError: Less is more

##### "THIRD_QUANTILE"
Returns third quantile of numerical column

Parameters:
- optional Double accuracyError: Less is more

##### "GET_QUANTILE"
Returns custom quantile

Parameters:
- optional Double accuracyError: Less is more
- Double targetSideNumber: Provided value for quantile (should be in [0,1])

##### "GET_PERCENTILE"
Return custom percentile

Parameters:
- optional Double accuracyError: Less is more
- Double targetSideNumber: Provided value for percentile
