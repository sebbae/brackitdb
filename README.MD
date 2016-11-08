# BrackitDB - XQuery-powered XML Database

BrackitDB is a transactional XML database.

## Database Administration

You can start the database through a shell script.

```
bash ./bin/server
```

Hint: On UNIX-based systems you may want to make the script executable by issuing

```
chmod u+x ./bin/server
```

### Creating an empty database

To use brackitdb the first time, you must create an empty database with the command bash

```
./bin/server install
```

When everything runs fine, you are ready to use brackitdb

### Starting/stopping the database server

You can start the database by issuing

```
bash ./bin/server start
```

and stop it again with

```
bash ./bin/server stop
```

or by sending a SIGQUIT signal (Ctrl+D) to the server process.

## The Command Line Processor

BrackitDB ships with a command line processor to issue queries against the database:

```
bash ./bin/server clp
```

### Client programs

The database driver jar file `brackitdb-driver-X.Y.Z.jar` resides in the subdirectory `./brackitdb-driver/target/`.

The following code fragment demonstrates how to connect to run queries against the database:

```Java
// open a new connection to a locally running brackitdb server
BrackitConnection con = new BrackitConnection("localhost", 11011);

try {
  // run a simple query
  // and write the result
  // to standard out
  con.query("1+1", System.out);
} finally {
  // close the connection again
  con.close();
}
```
