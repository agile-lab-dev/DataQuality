# Postprocessors

Postprocessors are the final blocks of DataQuality workflow. They are made to enrich and transform
 particular DataFrames (made out of Sources or Virtual Sources) in order to obtain desired form, which 
 may be used in future applications (for reporting as example).
 
Currently there are 4 types of postprocessing:
 - Enrich: attaching values or constants to already defined/new DataFrame
 - Transpose by key: transposing all, but key ones
 - Transpose by column: transposing each defined column separately
 - Arrange: selecting, rearranging DataFrame columns and doing type casting
 
Result of each postprocessor is a DataFrame stored as file (csv, avro), which can be used as an input to next
 postprocessor. All postprocessors are executed in the order they were defined.
 
> **Tip:** You can save each step of postprocessing in the same file, framework will override it each time,
 but be sure that you will not use previous obtained result.
 
## Details
##### General information
Every postprocessor is using the following structure:
```hocon
{
    mode: "#POSTPROCESSOR_MODE"
    config: {
      // PP SPECIFIC
      #PARAM1: "VALUE1"
      // COMMON
      source: "#INPUT_SOURCE_ID"
      saveTo: {
        fileName: "#OUTPUT_SOURCE_ID"
        fileFormat: "csv"
        delimiter: ","
        path: "#PATH_TO_FILE" // no need to specify file itself
      }
    }
}
```
##### Enrich

Enrich postprocessor is used to connect source, metrics, checks and all additional together to create "body"
 of the future report. All the values will be attached as columns to the DataFrame. In this postprocessor source is
 optional.
 
 > **Tip:** All references to metrics,checks and columns in the DataFrame are case sensitive.

*Mode: "enrich"*

| Parameter     | Is optional | Type            | Description                   |
| ------------- | :---------: | --------------- | ----------------------------- |
| source        | +           | String          | Name of input source/vs       |
| metrics       |             | Array[String]   | Received metric results       |
| checks        |             | Array[String]   | Received checks results       |
| extra         |             | Object[HOCON]   | Extra parameters              |
| saveTo        |             | Object[HOCON]   | Output source configuration   |

##### Transpose by key

Transpose by key is transposing the input DataFrame, but keeping key columns untouched. It's creates extra rows.
Header of transposed part: "KEY","VALUE"

*Mode: "transpose_by_key"*

| Parameter     | Is optional | Type            | Description                   |
| ------------- | :---------: | --------------- | ----------------------------- |
| source        |             | String          | Name of input source/vs       |
| keyColumns    |             | Array[String]   | Columns to keep untouched     |
| saveTo        |             | Object[HOCON]   | Output source configuration   |

##### Transpose by column

Transpose by column on the other hand, transposing each column individually (detaching header and putting it as
 a column). In particular, in takes key/all columns in the order as they are present, adding column with header
 to the left (new header will be "KEY_#","VALUE_#") and adding/trimming extra columns to fit required structure.

> **Tip:** In this postprocessor key columns are the ones to transform!

*Mode: "transpose_by_column"*

| Parameter       | Is optional | Type            | Description                   |
| --------------- | :---------: | --------------- | ----------------------------- |
| source          |             | String          | Name of input source/vs       |
| keyColumns      | +           | Array[String]   | Columns to transform          |
| numberOfColumns |             | Int             | Format of transposing         |
| saveTo          |             | Object[HOCON]   | Output source configuration   |
   
##### Arrange

Arrange in the postprocessor made to rearrange columns in the DataFrame and cast them to a specific type.

> **Tip:** Be sure that selected column is castable.

*Mode: "arrange"*

| Parameter       | Is optional | Type            | Description                   |
| --------------- | :---------: | --------------- | ----------------------------- |
| source          |             | String          | Name of input source/vs       |
| columnOrder     |             | Array[*]        | Desired column order          |
| saveTo          |             | Object[HOCON]   | Output source configuration   |

"Star" type can be:
- String
- Tuple object (column_name, type). Example: *{"battle_number":"double"}*

Supported types:
- Double
- Int
- Long

It could be able to format a string or a number. 

### Declare number precision

In the following example the **amount** column has the number precision equals to 5 (e.g. 1.00441 or 1.00000)

```hocon
{
    mode: "arrange"
    config: {
      source: "tera_enriched"
      columnOrder: [{"battle_number":"double"}, "name", {"amount": {"double": 5}}]
      saveTo: {
        fileName: "tera_arranged"
        fileFormat: "csv"
        path: "./tmp/postproc"
        delimiter: ","
      }
    }
  }
```


### Format a string

In the following example the **surname** column has a prefix _Hello_ (e.g. Hello Carl)

```hocon
{
    mode: "arrange"
    config: {
      source: "tera_enriched"
      columnOrder: [{"battle_number":"double"}, "name", {"surname": {"STRING": "Hello %s"}}, {"y_avg":"int"}]
      saveTo: {
        fileName: "tera_arranged"
        fileFormat: "csv"
        path: "./tmp/postproc"
        delimiter: ","
      }
    }
  }
``` 

## Example

```hocon
Postprocessors:[
  {
    mode: "enrich"
    config: {
      source: "BTL_FILTERED"
      metrics: ["y_avg","1011"]
      checks: ["teracheck"]
      extra: {
        department: "Westeros"
        agent: "Barese guy"
      }
      saveTo: {
        fileName: "tera_enriched"
        fileFormat: "csv"
        path: "./side-code/dump/postproc"
        delimiter: ","
      }
    }
  },
  {
    mode: "transpose_by_key"
    config: {
      keyColumns: ["name"]
      source: "tera_enriched"
      saveTo: {
        fileName: "tera_transposed"
        fileFormat: "csv"
        path: "./side-code/dump/postproc"
        delimiter: ","
        quoted: true
      }
    }
  },
  {
    mode: "transpose_by_column"
    config: {
      source: "tera_enriched"
      numberOfColumns: 5
      saveTo: {
        fileName: "tera_headless"
        fileFormat: "csv"
        path: "./side-code/dump/postproc"
        delimiter: ","
      }
    }
  },
  {
    mode: "arrange"
    config: {
      source: "tera_enriched"
      columnOrder: [{"battle_number":"double"}, "name", {"y_avg":"int"}]
      saveTo: {
        fileName: "tera_arranged"
        fileFormat: "csv"
        path: "./side-code/dump/postproc"
        delimiter: ","
      }
    }
  }
]
```