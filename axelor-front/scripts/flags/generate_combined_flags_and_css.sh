#!/usr/bin/env sh

IMAGE_FILE=flags.svg
CSS_FILE=flags.module.scss

WORK_DIR=${TMPDIR:-${TEMP:-${TMP:-/tmp}}}

cd "$(dirname "$0")"

./extract_country_codes.py |\
./to_twemoji_url.py |\
./download_image.py -d "$WORK_DIR/twemoji" &&\
./rename_svg.py "$WORK_DIR/twemoji" "$WORK_DIR/flags" &&\
./combine_svg.py "$WORK_DIR/flags" -o "../../src/assets/$IMAGE_FILE" &&\
./generate_css.py "$WORK_DIR/flags" -o "../../src/views/form/widgets/phone/$CSS_FILE" &&\
rm -rf $WORK_DIR/flags
