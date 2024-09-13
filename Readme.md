# HFC Server and Example Client Based on Communication via Apache Thrift

This module implements a Java based server providing end points
via [Apache Thrift](https://thrift.apache.org/) for accessing ontologies loaded
with [HFC](https://github.com/bkiefer/hfc). It also contains example clients in Java and Python
demonstrating how to interact with the server.

## Requirements

This software was tested on Ubuntu 22.04 LTS. For local deployment, the following software is
required:

- [Apache Thrift](https://thrift.apache.org/), version 0.19 or higher
- [Java 11 JDK](https://openjdk.org/projects/jdk/11/) or higher
- [Maven](https://maven.apache.org/) build tool, version 3.5 or higher

Additionally for the Python client:

- [Python](https://www.python.org/) version 3.10 or later
- optional: [Poetry](https://python-poetry.org/) packaging and dependency management system, version
  1.8.3 or
  later

## Installation

In the top-level folder, run

`./compile.sh`

This executes the following steps:

- Read the Thrift interface definitions in `src/main/thrift` and create source code for
    - Java in `src/main/gen-java` (required for both HFC server and example Java client)
    - Python in `src/main/python/src/hfc_db` (required for Python client)
- Compile the HFC server and example client and create two fat jars
    - `target/hfc-server.jar` for the server, main class `de.dfki.lt.hfc.db.server.HfcDbMain`
    - `targer/hfc-client.jar` for the client, main class `de.dfki.lt.hfc.db.client.HfcDbClient`

  These fat jars are used by the scripts in `bin`.

For the Python example client:

- In the folder `src/main/python`, install Python example client and all required dependencies in a
  Poetry virtual environment by running

  `poetry install`

  or alternatively, use a virtual environment of your choice and install the Python example client
  and its dependencies by running

  `pip install .`

## Starting the Server

Run

`./bin/startServer src/test/data/test.yml`

to start the server listening to port number 9090. The port number can be changed using the `-p`
option. The script takes as argument a config file
`test.yml` specifying the details of the ontology to load with HFC.

## Running the Java Example Client

The client currently only is very rudimentary example code, and will be extended soon.

Run

`./bin/startClient src/test/data/rifca.yml`

to insert the triples in `src/test/data/rifca.nt`
into the HFC database and perform some queries. Again, the server port number the client uses can be
changed using the `-p` option.

Alternatively, you can use the `-i` or `-c` option (with or without the config
file) to get graphical or console interactive version where you can enter
queries and look at the results (very raw output, sorry)

## Running the Python Example Client

In the folder `src/main/python`, activate the virtual environment with the Python example client.

`src/hfc_thrift/hfcclient.py` is the Python client responsible for communication with the HFC
server. It is used by `src/hfc_thrift/rdfproxy.py` that acts as proxy between Python objects and
ontology entities. For usage, see unit tests in `test/hfcdbtest.py`.

Run

`python ./test/hfcdbtest.py`

to run the unit tests. Note that this starts its own temporary HFC server used for tests only
running on port 7979.
