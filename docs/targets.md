# Targets

In order to control alerting and saving of the results, we introduced new entity called "Targets".

There are 2 types of targets:
 - Regular (checks, column metrics, file metrics, composed metrics): Saving result as file of predefined format.
 - System: Defining list of checks and alerting, if some of them failed.

## How to configure

> **Tip:** You should provide HDFS paths

Target configuration should follow this format:
```hocon
  {
    type: "#TARGET_TYPE"
    config: {
      fileFormat: "csv"
      path: "#SAVE_PATH"
      delimiter: ","
      savemode: "append"
    }
  }
```

To define "System" target use type: *"SYSTEM"*

System targets should also contain following fields:
```hocon
id: "#TARGET_ID",
checkList: ["#CHECK_ID"]
mailingList: ["#RECIEVER_EMAIL"],
```

## Headers

All the files that will be saved with target configuration will have one of those predefined schemas.

##### Checks

> **Tip:** This header is common for "Checks" regular target and the "System" one. 

Checks regular target type: *CHECKS*

| checkId | checkName | description | checkedFile | comparedMetric | comparedThreshold | status  | message | execDate |
| :-----: | :-------: | :---------: | :---------: |:-------------: | :---------------: | :----:  | :-----: | :------: |
| String  | String    | String      | String      | Option[String] | Option[Double]    | String* | String  | Date     |

Currently we are storing status as a string representation of a boolean value (Success/Failure).

##### File Metrics

File metrics target type: *FILE-METRICS*

| metricId | sourceDate | name    | sourceId | result | additionalResult |
| :------: | :--------: | :-----: | :------: | :----: | :--------------: |
| String   | Date       | String  | String   | Double | String           |

##### Columnar Metrics

File metrics target type: *COLUMNAR-METRICS*

| metricId | sourceDate | name    | sourceId | columnNames | params | result | additionalResult |
| :------: | :--------: | :-----: | :------: | :---------: | :----: | :----: | :--------------: |
| String   | Date       | String  | String   | Seq[String] | JSON   | Double | String           |

##### Composed Metrics

File metrics target type: *COMPOSED-METRICS*

| metricId | sourceDate | name    | sourceId | formula | result | additionalResult |
| :------: | :--------: | :-----: | :------: | :-----: | :----: | :--------------: |
| String   | Date       | String  | String   | String  | Double | String           |

## Example
```hocon
Targets: [
  {
    type: "SYSTEM"
    id: "test_systar",
    checkList: ["check_1", "check_3"]
    mailingList: ["hello@egor.com", "pippo@stat.eu"],
    config: {
      fileFormat: "csv"
      path: "./Agile.DataQuality/dq-core/output"
      delimiter: ","
      savemode: "append"
    }
  },
  {
    type: "CHECKS"
    config: {
      fileFormat: "csv"
      path: "./Agile.DataQuality/side-code/dump"
      delimiter: ","
      savemode: "append"
    }
  },
  {
    type: "COLUMNAR-METRICS"
    config: {
      fileFormat: "csv"
      path: "./Agile.DataQuality/side-code/dump"
      delimiter: ","
      savemode: "append"
    }
  },
  {
    type: "FILE-METRICS"
    config: {
      fileFormat: "csv"
      path: "./Agile.DataQuality/side-code/dump"
      delimiter: ","
      savemode: "append"
    }
  },
  {
    type: "COMPOSED-METRICS"
    config: {
      fileFormat: "csv"
      path: "./Agile.DataQuality/side-code/dump"
      delimiter: ","
      savemode: "append"
    }
  }
]
```