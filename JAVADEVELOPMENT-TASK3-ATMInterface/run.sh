#!/bin/bash
# Script to compile and run the Java Console ATM
set -e

mkdir -p out data receipts
javac -d out src/*.java
java -cp out Main
