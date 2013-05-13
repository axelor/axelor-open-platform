#!/bin/bash
UGLIFY=$(which uglifyjs)
OUTPUT="js/application-all.min.js"

ARGS=""
while test "$1" ; do
	case $1 in
		-o) OUTPUT=$2 ; shift ;;
		*) ARGS="$ARGS $1" ;;
	esac
	shift
done

if [ ! -f "$UGLIFY" ] ; then
	echo "Install NodeJS + UglifyJS to combine all javascript resources."
	exit 0
fi

files=$(cat js/application.js | grep -P "\s*(\'|\")(.*?.js)('|\")\s*(,)?" | sed s/"'\|\""/''/g | sed s/",$"/' '/g)

OUTDIR=$(dirname $OUTPUT)
mkdir -p $OUTDIR

echo "Compressing all javascript sources..."
$UGLIFY $files -o $OUTPUT $ARGS
echo "Generated '$OUTPUT'"

