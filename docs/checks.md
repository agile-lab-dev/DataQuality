# Checks

Checks are the main part of DataQuality framework. They are defining crucial parts of your analysis pipeline.
 With input of metric results or sql queries you will be able to understand quality of your data.
 
There are 3 base types of checks:
- Snapshot: Check metric result on the current source snapshot.
- Trend: Analise previous results of the metric in order to evaluate the behavior of your data.
- SQL: Executing classic sql checks on external database and importing result in to DataQuality framework.

> **Tip:** All checks are returning Boolean as the result.

## Snapshot checks 
Basic check between current metric result and threshold (constant)/other metric result

Supported subtypes are:
- *DIFFER_BY_LT:* Relative error between 2 metrics
- *EQUAL_TO:* Checks if current res is equal to threshold/metric result
- *GREATER_THAN:* Checks if current result is greather than threshold/metric result
- *LESS_THAN:* Checks if current result is less than threshold/metric result
	
Example:
```hocon
Checks: [
    {
        id: "123"
        type: "snapshot"
        subtype: "GREATER_THAN"
        description: "check for number rows limit with threshold on table A"
        config: {
          metrics: ["101"]
          params: {threshold: "10"}
        }
    },
    {
        type: "snapshot"
        subtype: "EQUAL_TO"
        description: "min less than max"
        config: {
          metrics: ["105", "104"]
          params: {compareMetric: "104"}
        }
    }
]
```

## Trend checks
##### Average check 
Checks if difference between current metric result and the average are inside the bound
        
Subtypes: 
- *"AVERAGE_BOUND_FULL_CHECK":* (1 - threshold) * avg_Result <= current_result <= (1 + threshold) * avg_Result
- *"AVERAGE_BOUND_UPPER_CHECK":* current_result <= (1 + threshold) * avg_Result
- *"AVERAGE_BOUND_LOWER_CHECK":* (1 - threshold) * avg_Result <= current_result

Required fields:
- *metrics:* Target metric id of TOP_N metric
- *rule:* "record" compares current metric result with previous R records and "date" compares current metric result with results made in last R days

Parameters:
- *threshold:* should be in [0,1]. Represents allowed difference level between results to pass the check.
- *timewindow:* represents time window size (amount of days/records)

Example:
```hocon
{
    type: "trend"
    subtype: "AVERAGE_BOUND_FULL_CHECK"
    name: "some basic trend"
    description: "trend date"
    config: {
        metrics: ["201"]
        rule: "date"
        params: {threshold: "0.5", timewindow: "5"}
    }
}
```
##### TOP N check
This check was developed for "TOP N" metric only. It calculates Jaccard distance between 2 TOP N rankings
 (current and the previous one).
 
> **Tip:** Currently supports only check between 2 records.
       
Subtype:
- **"TOP_N_RANK_CHECK"**
        
Fields: 
- *metrics:* Target metric id of TOP_N metric
- *rule:* "record" compares current metric result with previous R records and "date" compares current metric result with results made in last R days

Params:
- *threshold:* should be in [0,1]. Represents allowed difference level between results to pass the check.
- *timewindow:* represents time window size (amount of days/records)
- *targetNumber:* N in TOP_N. Should take N value of target metric of lesser.
            
Example:
```hocon
{
    type: "trend"
    subtype: "TOP_N_RANK_CHECK"
    name: "beepf"
    description: "some basic trend"
    config: {
        metrics: ["1011"]
        rule: "record"
        params: { threshold:"0.5", timewindow: "2", targetNumber:4}
    }
}
```
## SQL checks
Runs query on the remote database and check if result is zero or not.

Subtypes:
- "COUNT_EQ_ZERO": Returns "Success" if result is 0, else "Failure"
- "COUNT_NOT_EQ_ZERO": Returns "Success" if result is not 0, else "Failure"
	
Example:
```hocon
{
    type: "sql"
    subtype: "COUNT_EQ_ZERO"
    name: "test_sql_check"
    config: {
      source: "LOCAL_SQLITE"
      query: "select count(*) from column_metrics where name = 'Rocco'"
    }
}
``` 