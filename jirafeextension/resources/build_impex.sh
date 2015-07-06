#!/bin/bash

impex=impex/projectdata-datamaps.impex

(echo '# Generated file DO NOT EDIT' && echo) > $impex || exit 1

echo "INSERT_UPDATE JirafeMappingDefinitions;type;definition;filter;endPointName[unique=true]" >>$impex
cd datamaps || exit 1

quote(){
    echo -n '"'
    sed 's/"/""/g' "$@"
    echo -n '"'
}

for i in *.json
do
        r=${i%.json}
        e=$(echo "$r"|tr '[:upper:]' '[:lower:]')
        case $r in *-*)
                e=${r#*-}
                r=${r%-*}
        ;;esac
        echo ";$r;$(quote $i);$(quote $r-filter.groovy);$e" >>../$impex
done
