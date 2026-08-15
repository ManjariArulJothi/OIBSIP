#!/bin/bash
# Script to compile and run the automated ATM test suite
set -e

mkdir -p out
javac -d out src/*.java
java -cp out TestRunner
