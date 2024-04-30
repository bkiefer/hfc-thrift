#!/bin/bash
scriptdir=`dirname $0`

findnewest() {
    unset -v latest
    for file in `find "$@" -type f`; do
        test -z "$latest" -o "$file" -nt "$latest" && latest="$file"
    done
    echo "$latest"
}

cd "$scriptdir"
newestgen=`findnewest src/main/gen*`
newestthrift=`findnewest src/main/thrift`
[[ "$newestthrift" -nt "$newestgen" ]] && ./run-thrift

mvn -U clean install
mvn -f apps.xml install
