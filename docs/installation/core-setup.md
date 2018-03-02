Using DQ
------------

DQ is written in Scala, and the build is managed with SBT.

Before starting:
- Install JDK
- Install SBT
- Install Git

The steps to getting DQ up and running for development are pretty simple:

- Clone this repository:

    `git clone https://github.com/agile-lab-dev/DataQuality.git`

- Start DQ. You can either run DQ in local or cluster mode:

    - local: default setting
    - cluster: set isLocal = false calling makeSparkContext() in `DQ/utils/DQMainClass`

- Run DQ. You can either run DQ via scheduled or provided mode (shell):

    - `run.sh`, takes parameters from command line:
        **-n**, Spark job name
        **-c**, Path to configuration file
        **-r**, Indicates the date at which the DataQuality checks will be performed
        **-d**, Specifies whether the application is operating under debugging conditions
        **-h**, Path to hadoop configuration
---