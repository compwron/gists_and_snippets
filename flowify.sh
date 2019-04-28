#!/usr/bin/env bash

files_with_flow=`grep -r @flow web-src/src | wc -l | sed 's#^ *##g'` # sed removes leading whitespace
total_js_files=`find web-src/src -name *.js -type f | wc -l | sed 's#^ *##g'`

echo "Files with flow: "$files_with_flow
echo "Total js files: "$total_js_files

source previous_flowify_counts.sh
echo "Previous percentage flowified: "$PREVIOUS_FLOWIFY_PERCENTAGE

percentage_flowified=`bc <<< "scale=2; 100*$files_with_flow/$total_js_files"`
echo "Current percentage flowified: "$percentage_flowified

comparison_result=`echo "$percentage_flowified < $PREVIOUS_FLOWIFY_PERCENTAGE" | bc -l`
if (($comparison_result == 1))
    then
        echo "ERROR: flow percentage has gone down from "$PREVIOUS_FLOWIFY_PERCENTAGE" to "$percentage_flowified
        echo "Please add @flow to more files"
        exit -1 # error
    else
        echo "Flowify percentage has not decreased. Yay!"
fi;

echo "export PREVIOUS_FILES_WITH_FLOW="$files_with_flow > previous_flowify_counts.sh
echo "export PREVIOUS_TOTAL_JS_FILES="$total_js_files >> previous_flowify_counts.sh
echo "export PREVIOUS_FLOWIFY_PERCENTAGE="$percentage_flowified >> previous_flowify_counts.sh



# in build.gradle:
# task flowify(type: Exec) {
#     commandLine "bash", "flowify_status.sh"
# }
# test.dependsOn([':flowify'])
