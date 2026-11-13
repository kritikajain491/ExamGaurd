#!/bin/sh
# Compile and run ExamGuard (Linux / macOS)
cd "$(dirname "$0")"
mkdir -p out
find src -name "*.java" > sources.txt
javac -d out @sources.txt && java -cp out examguard.Main
