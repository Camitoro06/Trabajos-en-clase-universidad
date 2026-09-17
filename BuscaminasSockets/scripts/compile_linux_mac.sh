#!/usr/bin/env bash
set -e
cd "$(dirname "$0")/.."
rm -rf out
mkdir -p out
find src/main/java -name "*.java" > sources.txt
javac -encoding UTF-8 -d out @sources.txt
rm sources.txt
echo "Compilacion terminada. Clases en out/"
