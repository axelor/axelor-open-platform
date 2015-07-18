#!/bin/bash

./bin/compress --local `which uglifyjs` codemirror \
	clike \
	css \
	groovy \
	htmlmixed \
	javascript \
	markdown \
	properties \
	sql \
	xml \
	comment \
	continuecomment \
	foldcode \
	foldgutter \
	brace-fold \
	xml-fold \
	comment-fold \
	markdown-fold \
	closebrackets \
	closetag \
	matchbrackets \
	matchtags \
	fullscreen \
	placeholder \
	show-hint \
	xml-hint \
	css-hint \
	html-hint \
	javascript-hint \
	anyword-hint \
	sql-hint \
	runmode \
	search \
	active-line > codemirror.min.js

cat lib/codemirror.css \
	addon/hint/show-hint.css \
	addon/fold/foldgutter.css \
	addon/display/fullscreen.css \
	theme/ambiance.css \
	theme/monokai.css \
	theme/solarized.css \
	theme/eclipse.css \
	theme/base16-dark.css \
	theme/base16-light.css | cssmin > codemirror.min.css
