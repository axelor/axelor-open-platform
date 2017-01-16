/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2016 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
var gulp = require('gulp');
var uglify = require('gulp-uglify');
var cleanCss = require('gulp-clean-css');
var autoprefixer = require('gulp-autoprefixer');
var concat = require('gulp-concat');
var rename = require('gulp-rename');
var base64 =  require('gulp-base64');
var gzip = require('gulp-gzip');

var fs = require('fs');
var del = require('del');

function jsList(input) {

	var lines = fs.readFileSync(input).toString().split('\n'),
		files = [];

	lines.forEach(function (line) {
        var match = /^\s*\/\/=\s+(.*?\.js)/.exec(line);
        if (match) {
        	var file = match[1];
        	files.push(file);
        }
    });

	return files;
}

function minifyCss(name) {
	return function () {
		return gulp.src('css/' + name + '.css')
		  	.pipe(autoprefixer())
		    .pipe(cleanCss())
		    .pipe(base64({extensions: ['svg', 'png', 'jpg', 'gif'] }))
		    .pipe(rename(name + '.min.css'))
		    .pipe(gulp.dest('css'))
		  	.pipe(gzip())
		  	.pipe(rename(name + '.gzip.css'))
		  	.pipe(gulp.dest('css'));
	};
}

function minifyJs(name) {
	return function () {
		return gulp.src(jsList('js/' + name + '.js'))
		  	.pipe(concat(name + '.min.js'))
		    .pipe(uglify({ compress: false }))
		    .pipe(gulp.dest('js'))
		    .pipe(gzip())
		    .pipe(rename(name + '.gzip.js'))
		    .pipe(gulp.dest('js'));
	};
}

gulp.task("clean", function() {
	return del.sync([
		'css/application.min.*',
		'css/application.gzip.*',
		'css/application.login.min.*',
		'css/application.login.gzip.*',
		'js/application.min.*',
		'js/application.gzip.*',
		'js/application.login.min.*',
		'js/application.login.gzip.*'
	]);
});

gulp.task('minify-css:app', minifyCss('application'));
gulp.task('minify-css:login', minifyCss('application.login'));

gulp.task('minify-js:app', minifyJs('application'));
gulp.task('minify-js:login', minifyJs('application.login'));

gulp.task('minify-css', ['minify-css:app', 'minify-css:login']);
gulp.task('minify-js', ['minify-js:app', 'minify-js:login']);

gulp.task('default', ['minify-css', 'minify-js']);
