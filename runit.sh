#!/usr/bin/sh

CP="lib/sqljdbc41.jar:target/dbunittools-jar-with-dependencies.jar"
#CP="lib/*:target/dbunittools.jar"
java -cp $CP DBTool flatxml
#CP="sqljdbc41.jar"
#java -cp $CP -jar target/dbunittools-jar-with-dependencies.jar
