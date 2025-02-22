﻿# DistributedSystems-JMS-RingElection

**Author:** Vincenzo Barbato
**Date:** 04/2024

This project simulates a distributed system based on JMS. The nodes in the distributed system have two tasks:

-   The first one is to elect the master node whenever it is necessary (system start, no response received, node goes down and becomes active). The election algorithm used is the **Ring Algorithm**.
    
-   The second one is to request the master node if it is possible to execute a task (this represents a simulation of requesting limited resources to the master node).
    

The probability that the master gives resources to a node is **70%**. The probability that a node goes down is **30%**. The probability that a node becomes active is **70%**. There are **5** nodes in the distributed system, and each node is tasked with executing **100** tasks.

**N.B. The last node of the distributed system ends execution without completing its 100 tasks.**

## Documentation

Javadoc documentation can be found in the folder: **RingElection/javadoc**
To view the project documentation on the open browser click on: **index.html**

## Setup environment

To run ***.jar** files you needJDK Development Kit 21.

To manage project you need JavaIDE (we use eclipse with javaSE-17).

Download apache-activemq-6.0.1 on [ActiveMQ](https://activemq.apache.org/components/classic/documentation/download-archives)

If you use eclipse IDE :

 - Go to: project->Build Path-> Configure build path
	In classpath add external jar:
	- Go to folder: apache-activemq-6.0.1
	   import activemq-6.0.1.jar
	- Go to folder: lib/optional
	   import all file with **log4j** in the name

The project use port **61616** therefore is necessary keep free it or change it.

## Run application with *.jar files

This is a simulation of distributed system based on 1 broker and 5 nodes.

First one we need to open 6 terminal in the folder where there are ***.jar** files.

In the first terminal we run the broker:
```
java -jar Broker.jar
```

In the others 5 terminal we run the nodes:

```
java -jar Node.jar
```
