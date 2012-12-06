#!/bin/bash

NODE=$(which node)
OUTPUT="js/application-all.min.js"

if [ ! -f "$NODE" ] ; then
	echo "Install NodeJS + UglifyJS to combine all javascript resources."
	exit 0
fi

[ -z "$1" ] || OUTPUT=$1

mkdir -p `dirname $OUTPUT`

cat << EOF | $NODE

var fs = require('fs')

var jsp = require("uglify-js").parser;
var pro = require("uglify-js").uglify;

// uglify non-lib code
function uglify(source) {
	var ast = jsp.parse(source);
	ast = pro.ast_mangle(ast);
	ast = pro.ast_squeeze(ast);
	return pro.gen_code(ast);
}

function minimizeJS() {
	var code = "";
	
	console.log("Combine: js/application.js");
	fs.readFileSync('js/application.js').toString()
	.split('\n').forEach(function (line) {
		var mat = /\s*(\'|\")(.*?.js)('|\")\s*(,)?/.exec(line)
		if (mat) {
			var file = mat[2]
			console.log('Processing:', file);
			var source = fs.readFileSync(file).toString();
			source = source.replace("\"use strict;\"", "")
			if (/([-_.])min\.js/.test(file)) {
				return code += source + ";\n"
			}
			code += uglify(source) + ";\n"
		}
	})
	fs.writeFileSync("$OUTPUT", code)
	console.log("Created: $OUTPUT");
}

minimizeJS();

EOF
