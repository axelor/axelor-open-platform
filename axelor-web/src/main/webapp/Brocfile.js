const fs = require('fs');

const postcss = require('broccoli-postcss');
const cssnext = require('postcss-cssnext');
const cssimport = require('postcss-import');
const cssurl = require('postcss-url');

const concat = require('broccoli-concat');
const merge = require('broccoli-merge-trees');
const uglify = require('broccoli-uglify-sourcemap');
const Funnel = require('broccoli-funnel');
const Gzip = require('broccoli-gzip');

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
        module: cssnext,
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

function minifyJs(name) {
  const tree = concat(app, {
    headerFiles: jsList('js/' + name + '.js'),
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

function gzip(tree) {
  return new Funnel(new Gzip(tree, { keepUncompressed: true }), {
    getDestinationPath: function (destPath) {
      return destPath.replace(/\.min\.(js|css)\.gz$/, '.gzip.$1');
    },
  });
}

module.exports = gzip(merge([
  minifyCss('application'),
  minifyCss('application.login'),
  minifyJs('application'),
  minifyJs('application.login'),
]));

