/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
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
const fs = require('fs');

const postcss = require('broccoli-postcss');
const presetenv = require('postcss-preset-env');
const cssimport = require('postcss-import');
const cssurl = require('postcss-url');

const concat = require('broccoli-concat');
const merge = require('broccoli-merge-trees');
const uglify = require('broccoli-uglify-sourcemap');
const Funnel = require('broccoli-funnel');
const Gzip = require('broccoli-gzip');
const YAML = require('yaml');

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
  const tree = postcss(app, {
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
    ],
  });
  return new Funnel(tree, {
    getDestinationPath: function () {
      return name + '.min.css';
    },
  });
}

function minifyJs(name, headerFiles) {
  if (headerFiles === undefined) {
    headerFiles = jsList('js/' + name + '.js');
  }
  const tree = concat(app, {
    headerFiles: headerFiles,
    allowNone: true,
    outputFile: name + '.min.js',
  });
  return uglify(tree, {
    compress: false,
    sourceMapConfig: {
      enabled: false,
    },
  });
}

function minifyJsFile(name) {
  return minifyJs(name, ['js/' + name + '.js']);
}

function gzip(tree) {
  return new Funnel(new Gzip(tree, { keepUncompressed: true }), {
    getDestinationPath: function (destPath) {
      return destPath.replace(/\.min\.(js|css)\.gz$/, '.gzip.$1');
    },
  });
}

const minifyList = [
  minifyCss('application'),
  minifyCss('application.login'),
  minifyJs('application'),
  minifyJs('application.login'),
];

try {
  const minifyFile = fs.readFileSync('minify.yml', 'utf-8');
  const minifyFiles = YAML.parse(minifyFile);
  if (minifyFiles.css) {
    minifyFiles.css.forEach(name => {
      minifyList.push(minifyCss(name));
      console.log(`Minified CSS: ${name}`);
    });
  }
  if (minifyFiles.js) {
    minifyFiles.js.forEach(name => {
      minifyList.push(minifyJsFile(name));
      console.log(`Minified JS: ${name}`);
    });
  }
  if (minifyFiles.jsList) {
    minifyFiles.jsList.forEach(name => {
      minifyList.push(minifyJs(name));
      console.log(`Minified JS list: ${name}`);
    });
  }
} catch (err) {
  if (err.code === 'ENOENT') {
    console.log('No extra files to minify');
  } else {
    console.error(err);
  }
}

module.exports = gzip(merge(minifyList));
