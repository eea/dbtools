#!/usr/bin/sh

DBDRIVERS="sqljdbc41.jar"
CP="$DBDRIVERS:target/dbtools-jar-with-dependencies.jar"
java -cp $CP DBTool flatxml
