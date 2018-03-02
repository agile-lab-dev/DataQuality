####DataQuality - UI
The UI to create configuration for Data Quality framework 


##Quick Start

###Prerequisites

DataQuality UI has some requirements:

- Java 8 (other versions are not tested)
- sbt
- Postgress v9.3

###Build

- Inside the project folder compile the project and create the distribution package

  `sbt "project ui" dist`


###Setup and Run

- Move into `target` folder

  `cd dq-ui/target/universal/`

- Unzip the distribution package where do you want

  `unzip ui-1.0.zip -d <destination folder>`

- Go into dist folder
  
  `cd <destination folder>/ui-1.0`
  
-  Edit `conf/application.conf`:
    - update database connection with your own.
    - for security, update `play.crypto.secret` with new one. 
    Check Play framework [doc](https://playframework.com/documentation/2.5.x/ApplicationSecret) for generate it
     
- Generate a Secret Key for application

- Run the application server

  `bin/ui`

- Go to `<your ip>:9000/dataquality` to show the UI. If you run it locally the ip is `localhost`.
