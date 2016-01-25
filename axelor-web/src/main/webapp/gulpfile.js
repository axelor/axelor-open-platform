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
var minifyCss = require('gulp-minify-css');
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

gulp.task("clean", function() {
	return del.sync([
		'css/application.min.*',
		'css/application.gzip.*',
		'js/application.min.*',
		'js/application.gzip.*'
	]);
});

gulp.task('minify-css', function() {
  return gulp.src('css/application.css')
  	.pipe(autoprefixer())
    .pipe(minifyCss())
    .pipe(base64({extensions: ['svg', 'png', 'jpg', 'gif'] }))
    .pipe(rename('application.min.css'))
    .pipe(gulp.dest('css'))
  	.pipe(gzip())
  	.pipe(rename('application.gzip.css'))
  	.pipe(gulp.dest('css'));
});

gulp.task('minify-js', function() {
  return gulp.src(jsList('js/application.js'))
  	.pipe(concat('application.min.js'))
    .pipe(uglify({ compress: false }))
    .pipe(gulp.dest('js'))
    .pipe(gzip())
    .pipe(rename('application.gzip.js'))
    .pipe(gulp.dest('js'));
});

gulp.task('default', ['minify-css', 'minify-js']);
