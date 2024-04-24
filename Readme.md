# HFC server and example client based on communication via thrift

## Installation

1. Make sure you have an internet connection
2. Install maven on your computer
3. go to the `hfc-thrift` directory and enter

   `mvn install`

4. If you also want to use the python bridge, install version 0.19.0 of
   [Apache Thrift](https://thrift.apache.org/) on your computer. You will need
   the thrift python libraries installed to use it.

## Start server

`./bin/startServer src/test/data/test.yml`

This will start the server listening to port number 9090. The port number can
be changed using the `-p` option.

## Start example client (not to be derived from, only an example!)

`./bin/startClient`

The client currently only is very rudimentary example code, and will
be extended soon. For test purposes, you can provide
`src/test/data/rifca.yml` as additional argument, which will insert
the triples in `src/test/data/rifca.nt` into the database and perform some
queries.

Alternatively, you can use the `-i` or `-c` option (with or without the config
file) to get graphical or console interactive version where you can enter
queries and look at the results (very raw output, sorry)
