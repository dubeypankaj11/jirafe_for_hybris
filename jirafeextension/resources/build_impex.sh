#!/bin/bash

impex=impex/essentialdata-jirafeextension.impex

(echo '# Generated file DO NOT EDIT' && echo && cat $impex-template) > $impex || exit 1

echo "INSERT_UPDATE JirafeMappingDefinitions;type;definition;filter;endPointName[unique=true]" >>$impex
cd test_json || exit 1

quote(){
    echo -n '"'
    sed 's/"/""/g' "$@"
    echo -n '"'
}

for i in *.json
do
        r=${i%.json}
        echo ";$r;$(quote $i);$(quote $r-filter.groovy);$(echo "$r"|tr '[:upper:]' '[:lower:]')" >>../$impex
done
