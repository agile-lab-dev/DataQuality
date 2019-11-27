# Load Checks

In addition to regular checks, which are relying on some metric results, the Data Quality Framework has Load Checks.
  Similarly to metric checks, they are working on top of Source metadata and curtain boolean expression.

You should use Load Checks for scenarion in which you don't need to iterate thru rows of the Source's Data Frame.
  For example, when you need to check the number of columns.

Load checks can be Pre or Post load. They are being performed at the beggining of the pipeline, before
  any other metric or check.
  
At the moment the following checks are present:
##### Pre
* __EXIST__: Check if the source is present in the defined path
* __ENCODING__: Checks if the source is loadable with the following encoding
* __FILE_TYPE__: Checks if the source is loadable in the desired format
##### Post
* __EXACT_COLUMN_NUM__: Checks if #columns of the source is the same as desired number
* __MIN_COLUMN_NUM__: Checks if #columns of the source is more or equal to the desired number

### Example
```hocon
LoadChecks: [
  {
    id = "encoding_check"
    type = "ENCODING"
    source = "sample_A"
    option = "UTF-8" // String: Encoding name (please, use encoding names defined in Spark)
  },
  {
    id = "min_column"
    type = "MIN_COLUMN_NUM"
    source = "sample_A"
    option = 10 // Integer: Num of columns
  },
  {
    id = "file_type"
    type = "FILE_TYPE"
    source = "sample_A"
    option = "avro" // String: File formate (csv, avro)
  },
  {
    id = "file_existence"
    type = "EXIST"
    source = "sample_A"
    option = true // Boolean: Expected result
  }
]
```
