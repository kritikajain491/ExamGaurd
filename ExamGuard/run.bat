@echo off
REM Compile and run ExamGuard (Windows)
cd /d "%~dp0"
if not exist out mkdir out
dir /s /b src\*.java > sources.txt
javac -d out @sources.txt && java -cp out examguard.Main
