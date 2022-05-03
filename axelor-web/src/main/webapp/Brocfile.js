/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
/*jshint esversion: 6 */
'use strict';

const fs = require('fs');

const postcss = require('broccoli-postcss');
const presetenv = require('postcss-preset-env');
const cssimport = require('postcss-import');
const cssurl = require('postcss-url');
const cssnano = require('cssnano');

const concat = require('broccoli-concat');
const merge = require('broccoli-merge-trees');
const Terser = require('broccoli-terser-sourcemap');
const Funnel = require('broccoli-funnel');
const Gzip = require('broccoli-gzip');
const glob = require('glob');

const app = '.';

function jsList(input) {
  const lines = fs.readFileSync(input).toString().split('\n');
  const files = [];
  lines.forEach(function (line) {
    const match = /^\s*\/\/=\s+(.*?\.js)\s*$/.exec(line);
    if (match) {
      files.push(match[1]);
    }
  });
  return files;
}

function minifyCss(name) {
  const tree = new postcss(app, {
    map: false,
    include: ['css/' + name + '.css'],
    plugins: [
      {
        module: cssimport,
        options: {
          plugins: [],
        },
      },
      {
        module: cssurl,
        options: [
          { url: 'rebase' },
          { filter: /\.(jpg|png|gif)$/, url: 'inline' },
        ],
      },
      {
        module: presetenv,
        options: {
          browsers: ['last 3 version', 'ie 11'],
        },
      },
      {
        module: cssnano,
        options: {
          preset: ['default', { "discardComments": { "removeAll": true } }]
        }
      }
    ],
  });
  return new Funnel(tree, {
    getDestinationPath: function () {
      return name + '.min.css';
    },
  });
}

function minifyJs(name) {
  const tree = concat(app, {
    headerFiles: jsList('js/' + name + '.js'),
    allowNone: true,
    outputFile: name + '.min.js',
  });
  return new Terser(tree, {
    terser: {
      compress: false,
      sourceMap: false,
    },
    hiddenSourceMap: false
  });
}

function gzip(tree) {
  return new Funnel(new Gzip(tree, { keepUncompressed: true }), {
    getDestinationPath: function (destPath) {
      return destPath.replace(/\.min\.(js|css)\.gz$/, '.gzip.$1');
    },
  });
}

const minifyNames = {"css": new Set(), "js": new Set()};
const minifyList = [];
const styleRe = /<x:style.*?\bsrc=['"][\.\/]*css\/(.*)\.css['"]/g;
const scriptRe = /<x:script.*?\bsrc=['"][\.\/]*js\/(.*)\.js['"]/g;

glob.sync("**/*.jsp").forEach(input => {
  const contents = fs.readFileSync(input).toString();
  let match;
  while ((match = styleRe.exec(contents)) !== null) {
    minifyNames.css.add(match[1]);
  }
  while ((match = scriptRe.exec(contents)) !== null) {
    minifyNames.js.add(match[1]);
  }
});

minifyNames.css.forEach(name => minifyList.push(minifyCss(name)));
minifyNames.js.forEach(name => minifyList.push(minifyJs(name)));

module.exports = gzip(merge(minifyList));
