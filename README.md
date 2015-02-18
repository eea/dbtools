DBTools
=======

Currently you can export data as flat XML for DBUnit or CSV format.

To set up the database copy database.properties-dist to database.properties and edit the information.

The software is delivered as a JAR file that has all dependencies except the database driver.

```
CP="sqljdbc41.jar:target/dbtools-jar-with-dependencies.jar"
java -cp $CP DBTool flatxml
```
