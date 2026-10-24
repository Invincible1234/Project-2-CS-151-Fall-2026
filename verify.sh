#!/bin/sh
set -eu
cd "$(dirname "$0")"
mkdir -p out
find src/main/java src/test/java test -name '*.java' | sort > out/sources.txt
javac --release 17 -Xlint:all -Werror -d out @out/sources.txt
find src/test/java test -name '*Verification.java' | sort > out/checks.txt
while IFS= read -r source; do
    package=$(sed -n 's/^package \([^;]*\);/\1/p' "$source")
    class=$(basename "$source" .java)
    java -cp out "$package.$class"
done < out/checks.txt
printf 'All verification programs passed.\n'
