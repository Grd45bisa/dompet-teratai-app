#!/usr/bin/env sh

# Minimal Gradle wrapper script
DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"

if [ ! -f "$DIR/gradle/wrapper/gradle-wrapper.jar" ]; then
  echo "Missing gradle-wrapper.jar" >&2
  exit 1
fi

exec java -jar "$DIR/gradle/wrapper/gradle-wrapper.jar" "$@"
