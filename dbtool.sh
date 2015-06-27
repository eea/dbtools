#!/usr/bin/sh

DBDRIVERS="h2.jar"
CP="$DBDRIVERS:target/dbtools-jar-with-dependencies.jar"
java -cp $CP DBTool $@
