#!/bin/bash
UGLIFY=$(which uglifyjs)
OUTPUT="js/application-all.js"

if [ ! -f "$UGLIFY" ] ; then
	echo "Install NodeJS + UglifyJS to combine all javascript resources."
	exit 0
fi

files=$(cat js/application.js | grep -P "\s*(\'|\")(.*?.js)('|\")\s*(,)?" | sed s/"'\|\""/''/g | sed s/",$"/' '/g)

echo "Compressing all javascript sources..."
$UGLIFY $files -o $OUTPUT $@
echo "Generated '$OUTPUT'"

