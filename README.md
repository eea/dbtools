DBTools
=======

DBTools is a command-line database client that has command history.

To set up the database copy database.properties-dist to database.properties and edit the information.

The software is delivered as a JAR file that has all dependencies except the database driver. To add the database driver add the path to the `classpath` line in database.properties.

```
java -jar target/dbtools-jar-with-dependencies.jar
```
