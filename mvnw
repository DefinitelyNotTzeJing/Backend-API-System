#!/bin/sh
set -eu

basedir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
distribution_url=$(sed -n 's/^distributionUrl=//p' "$basedir/.mvn/wrapper/maven-wrapper.properties")
archive_name=${distribution_url##*/}
maven_version=${archive_name%-bin.zip}
maven_home="${HOME}/.m2/wrapper/dists/${maven_version}"

if [ ! -x "$maven_home/bin/mvn" ]; then
  mkdir -p "$(dirname "$maven_home")"
  temporary_directory=$(mktemp -d)
  trap 'rm -rf "$temporary_directory"' EXIT
  curl -fsSL "$distribution_url" -o "$temporary_directory/$archive_name"
  unzip -q "$temporary_directory/$archive_name" -d "$temporary_directory"
  mv "$temporary_directory/$maven_version" "$maven_home"
fi

exec "$maven_home/bin/mvn" -f "$basedir/pom.xml" "$@"
