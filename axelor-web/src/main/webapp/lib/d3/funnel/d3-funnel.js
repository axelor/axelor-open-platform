'use strict';

var _createClass = (function () { function defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ('value' in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } } return function (Constructor, protoProps, staticProps) { if (protoProps) defineProperties(Constructor.prototype, protoProps); if (staticProps) defineProperties(Constructor, staticProps); return Constructor; }; })();

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError('Cannot call a class as a function'); } }

(function (global, d3) {

	/* global d3 */
	/* jshint bitwise: false */
	'use strict';

	var D3Funnel = (function () {

		/**
   * @param {string} selector A selector for the container element.
   *
   * @return {void}
   */

		function D3Funnel(selector) {
			_classCallCheck(this, D3Funnel);

			this.selector = selector;

			// Default configuration values
			this.defaults = {
				width: 350,
				height: 400,
				bottomWidth: 1 / 3,
				bottomPinch: 0,
				isCurved: false,
				curveHeight: 20,
				fillType: 'solid',
				isInverted: false,
				hoverEffects: false,
				dynamicArea: false,
				minHeight: false,
				animation: false,
				label: {
					fontSize: '14px',
					fill: '#fff'
				}
			};
		}

		_createClass(D3Funnel, [{
			key: 'destroy',

			/**
    * Remove the funnel and its events from the DOM.
    *
    * @return {void}
    */
			value: function destroy() {
				// D3's remove method appears to be sufficient for removing the events
				d3.select(this.selector).selectAll('svg').remove();
			}
		}, {
			key: 'draw',

			/**
    * Draw the chart inside the container with the data and configuration
    * specified. This will remove any previous SVG elements in the container
    * and draw a new funnel chart on top of it.
    *
    * @param {Array}  data    A list of rows containing a category, a count,
    *                         and optionally a color (in hex).
    * @param {Object} options An optional configuration object to override
    *                         defaults. See the docs.
    *
    * @return {void}
    */
			value: function draw(data, options) {
				// Remove any previous drawings
				this.destroy();

				// Initialize chart options
				this._initialize(data, options);

				// Add the SVG
				this.svg = d3.select(this.selector).append('svg').attr('width', this.width).attr('height', this.height);

				this.sectionPaths = this._makePaths();

				// Define color gradients
				if (this.fillType === 'gradient') {
					this._defineColorGradients(this.svg);
				}

				// Add top oval if curved
				if (this.isCurved) {
					this._drawTopOval(this.svg, this.sectionPaths);
				}

				// Add each block section
				this._drawSection(0);
			}
		}, {
			key: '_initialize',

			/**
    * Initialize and calculate important variables for drawing the chart.
    *
    * @param {Array}  data
    * @param {Object} options
    *
    * @return {void}
    */
			value: function _initialize(data, options) {
				if (!isArray(data) || data.length === 0 || !isArray(data[0]) || data[0].length < 2) {
					throw {
						name: 'D3 Funnel Data Error',
						message: 'Funnel data is not valid.'
					};
				}

				// Initialize options if not set
				options = typeof options !== 'undefined' ? options : {};

				this.data = data;

				// Counter
				var i;

				// Prepare the configuration settings based on the defaults
				// Set the default width and height based on the container
				var settings = extend({}, this.defaults);
				settings.width = parseInt(d3.select(this.selector).style('width'), 10);
				settings.height = parseInt(d3.select(this.selector).style('height'), 10);

				// Overwrite default settings with user options
				var keys = Object.keys(options);
				for (i = 0; i < keys.length; i++) {
					if (keys[i] !== 'label') {
						settings[keys[i]] = options[keys[i]];
					}
				}

				// Label settings
				if (options.hasOwnProperty('label')) {
					var validLabelOptions = /fontSize|fill/;
					var labelOption;
					for (labelOption in options.label) {
						if (labelOption.match(validLabelOptions)) {
							settings.label[labelOption] = options.label[labelOption];
						}
					}
				}
				this.label = settings.label;

				// In the case that the width or height is not valid, set
				// the width/height as its default hard-coded value
				if (settings.width <= 0) {
					settings.width = this.defaults.width;
				}
				if (settings.height <= 0) {
					settings.height = this.defaults.height;
				}

				// Initialize the colors for each block section
				var colorScale = d3.scale.category10();
				for (i = 0; i < this.data.length; i++) {
					var hexExpression = /^#([0-9a-f]{3}|[0-9a-f]{6})$/i;

					// If a color is not set for the record, add one
					if (!('2' in this.data[i]) || !hexExpression.test(this.data[i][2])) {
						this.data[i][2] = colorScale(i);
					}
				}

				// Initialize funnel chart settings
				this.width = settings.width;
				this.height = settings.height;
				this.bottomWidth = settings.width * settings.bottomWidth;
				this.bottomPinch = settings.bottomPinch;
				this.isCurved = settings.isCurved;
				this.curveHeight = settings.curveHeight;
				this.fillType = settings.fillType;
				this.isInverted = settings.isInverted;
				this.hoverEffects = settings.hoverEffects;
				this.dynamicArea = settings.dynamicArea;
				this.minHeight = settings.minHeight;
				this.animation = settings.animation;

				// Calculate the bottom left x position
				this.bottomLeftX = (this.width - this.bottomWidth) / 2;

				// Change in x direction
				// Will be sharper if there is a pinch
				this.dx = this.bottomPinch > 0 ? this.bottomLeftX / (data.length - this.bottomPinch) : this.bottomLeftX / data.length;
				// Change in y direction
				// Curved chart needs reserved pixels to account for curvature
				this.dy = this.isCurved ? (this.height - this.curveHeight) / data.length : this.height / data.length;

				// Support for events
				this.onItemClick = settings.onItemClick;
			}
		}, {
			key: '_makePaths',

			/**
    * Create the paths to be used to define the discrete funnel sections and
    * returns the results in an array.
    *
    * @return {Array}
    */
			value: function _makePaths() {
				var paths = [];

				// Initialize velocity
				var dx = this.dx;
				var dy = this.dy;

				// Initialize starting positions
				var prevLeftX = 0;
				var prevRightX = this.width;
				var prevHeight = 0;

				// Start from the bottom for inverted
				if (this.isInverted) {
					prevLeftX = this.bottomLeftX;
					prevRightX = this.width - this.bottomLeftX;
				}

				// Initialize next positions
				var nextLeftX = 0;
				var nextRightX = 0;
				var nextHeight = 0;

				var middle = this.width / 2;

				// Move down if there is an initial curve
				if (this.isCurved) {
					prevHeight = 10;
				}

				var topBase = this.width;
				var bottomBase = 0;

				var totalArea = this.height * (this.width + this.bottomWidth) / 2;
				var slope = 2 * this.height / (this.width - this.bottomWidth);

				// This is greedy in that the section will have a guranteed height and
				// the remaining is shared among the ratio, instead of being shared
				// according to the remaining minus the guranteed
				if (this.minHeight !== false) {
					var height = this.height - this.minHeight * this.data.length;
					totalArea = height * (this.width + this.bottomWidth) / 2;
				}

				var totalCount = 0;
				var count = 0;

				// Harvest total count
				for (var i = 0; i < this.data.length; i++) {
					totalCount += isArray(this.data[i][1]) ? this.data[i][1][0] : this.data[i][1];
				}

				// Create the path definition for each funnel section
				// Remember to loop back to the beginning point for a closed path
				for (i = 0; i < this.data.length; i++) {
					count = isArray(this.data[i][1]) ? this.data[i][1][0] : this.data[i][1];

					// Calculate dynamic shapes based on area
					if (this.dynamicArea) {
						var ratio = count / totalCount;
						var area = ratio * totalArea;

						if (this.minHeight !== false) {
							area += this.minHeight * (this.width + this.bottomWidth) / 2;
						}

						bottomBase = Math.sqrt((slope * topBase * topBase - 4 * area) / slope);
						dx = topBase / 2 - bottomBase / 2;
						dy = area * 2 / (topBase + bottomBase);

						if (this.isCurved) {
							dy = dy - this.curveHeight / this.data.length;
						}

						topBase = bottomBase;
					}

					// Stop velocity for pinched sections
					if (this.bottomPinch > 0) {
						// Check if we've reached the bottom of the pinch
						// If so, stop changing on x
						if (!this.isInverted) {
							if (i >= this.data.length - this.bottomPinch) {
								dx = 0;
							}
							// Pinch at the first sections relating to the bottom pinch
							// Revert back to normal velocity after pinch
						} else {
							// Revert velocity back to the intial if we are using
							// static area's (prevents zero velocity if isInverted
							// and bottomPinch are non trivial and dynamicArea is false)
							if (!this.dynamicArea) {
								dx = this.dx;
							}

							dx = i < this.bottomPinch ? 0 : dx;
						}
					}

					// Calculate the position of next section
					nextLeftX = prevLeftX + dx;
					nextRightX = prevRightX - dx;
					nextHeight = prevHeight + dy;

					// Expand outward if inverted
					if (this.isInverted) {
						nextLeftX = prevLeftX - dx;
						nextRightX = prevRightX + dx;
					}

					// Plot curved lines
					if (this.isCurved) {
						paths.push([
						// Top Bezier curve
						[prevLeftX, prevHeight, 'M'], [middle, prevHeight + (this.curveHeight - 10), 'Q'], [prevRightX, prevHeight, ''],
						// Right line
						[nextRightX, nextHeight, 'L'],
						// Bottom Bezier curve
						[nextRightX, nextHeight, 'M'], [middle, nextHeight + this.curveHeight, 'Q'], [nextLeftX, nextHeight, ''],
						// Left line
						[prevLeftX, prevHeight, 'L']]);
						// Plot straight lines
					} else {
						paths.push([
						// Start position
						[prevLeftX, prevHeight, 'M'],
						// Move to right
						[prevRightX, prevHeight, 'L'],
						// Move down
						[nextRightX, nextHeight, 'L'],
						// Move to left
						[nextLeftX, nextHeight, 'L'],
						// Wrap back to top
						[prevLeftX, prevHeight, 'L']]);
					}

					// Set the next section's previous position
					prevLeftX = nextLeftX;
					prevRightX = nextRightX;
					prevHeight = nextHeight;
				}

				return paths;
			}
		}, {
			key: '_defineColorGradients',

			/**
    * Define the linear color gradients.
    *
    * @param {Object} svg
    *
    * @return {void}
    */
			value: function _defineColorGradients(svg) {
				var defs = svg.append('defs');

				// Create a gradient for each section
				for (var i = 0; i < this.data.length; i++) {
					var color = this.data[i][2];
					var shade = shadeColor(color, -0.25);

					// Create linear gradient
					var gradient = defs.append('linearGradient').attr({
						id: 'gradient-' + i
					});

					// Define the gradient stops
					var stops = [[0, shade], [40, color], [60, color], [100, shade]];

					// Add the gradient stops
					for (var j = 0; j < stops.length; j++) {
						var stop = stops[j];
						gradient.append('stop').attr({
							offset: stop[0] + '%',
							style: 'stop-color:' + stop[1]
						});
					}
				}
			}
		}, {
			key: '_drawTopOval',

			/**
    * Draw the top oval of a curved funnel.
    *
    * @param {Object} svg
    * @param {Array}  sectionPaths
    *
    * @return {void}
    */
			value: function _drawTopOval(svg, sectionPaths) {
				var leftX = 0;
				var rightX = this.width;
				var centerX = this.width / 2;

				if (this.isInverted) {
					leftX = this.bottomLeftX;
					rightX = this.width - this.bottomLeftX;
				}

				// Create path form top-most section
				var paths = sectionPaths[0];
				var path = 'M' + leftX + ',' + paths[0][1] + ' Q' + centerX + ',' + (paths[1][1] + this.curveHeight - 10) + ' ' + rightX + ',' + paths[2][1] + ' M' + rightX + ',10' + ' Q' + centerX + ',0' + ' ' + leftX + ',10';

				// Draw top oval
				svg.append('path').attr('fill', shadeColor(this.data[0][2], -0.4)).attr('d', path);
			}
		}, {
			key: '_drawSection',

			/**
    * Draw the next section in the iteration.
    *
    * @param {int} index
    *
    * @return {void}
    */
			value: function _drawSection(index) {
				if (index === this.data.length) {
					return;
				}

				// Create a group just for this block
				var group = this.svg.append('g');

				// Fetch path element
				var path = this._getSectionPath(group, index);
				path.data(this._getSectionData(index));

				// Add animation components
				if (this.animation !== false) {
					var self = this;
					path.transition().duration(this.animation).ease('linear').attr('fill', this._getColor(index)).attr('d', this._getPathDefinition(index)).each('end', function () {
						self._drawSection(index + 1);
					});
				} else {
					path.attr('fill', this._getColor(index)).attr('d', this._getPathDefinition(index));
					this._drawSection(index + 1);
				}

				// Add the hover events
				if (this.hoverEffects) {
					path.on('mouseover', this._onMouseOver).on('mouseout', this._onMouseOut);
				}

				// ItemClick event
				if (this.onItemClick) {
					path.on('click', this.onItemClick);
				}

				this._addSectionLabel(group, index);
			}
		}, {
			key: '_getSectionPath',

			/**
    * @param {Object} group
    * @param {int}	index
    *
    * @return {Object}
    */
			value: function _getSectionPath(group, index) {
				var path = group.append('path');

				if (this.animation !== false) {
					this._addBeforeTransition(path, index);
				}

				return path;
			}
		}, {
			key: '_addBeforeTransition',

			/**
    * Set the attributes of a path element before its animation.
    *
    * @param {Object} path
    * @param {int}	index
    *
    * @return {void}
    */
			value: function _addBeforeTransition(path, index) {
				var paths = this.sectionPaths[index];

				var beforePath = '';
				var beforeFill = '';

				// Construct the top of the trapezoid and leave the other elements
				// hovering around to expand downward on animation
				if (!this.isCurved) {
					beforePath = 'M' + paths[0][0] + ',' + paths[0][1] + ' L' + paths[1][0] + ',' + paths[1][1] + ' L' + paths[1][0] + ',' + paths[1][1] + ' L' + paths[0][0] + ',' + paths[0][1];
				} else {
					beforePath = 'M' + paths[0][0] + ',' + paths[0][1] + ' Q' + paths[1][0] + ',' + paths[1][1] + ' ' + paths[2][0] + ',' + paths[2][1] + ' L' + paths[2][0] + ',' + paths[2][1] + ' M' + paths[2][0] + ',' + paths[2][1] + ' Q' + paths[1][0] + ',' + paths[1][1] + ' ' + paths[0][0] + ',' + paths[0][1];
				}

				// Use previous fill color, if available
				if (this.fillType === 'solid') {
					beforeFill = index > 0 ? this._getColor(index - 1) : this._getColor(index);
					// Use current background if gradient (gradients do not transition)
				} else {
					beforeFill = this._getColor(index);
				}

				path.attr('d', beforePath).attr('fill', beforeFill);
			}
		}, {
			key: '_getSectionData',

			/**
    * @param {int} index
    *
    * @return {Array}
    */
			value: function _getSectionData(index) {
				return [{
					index: index,
					label: this.data[index][0],
					value: isArray(this.data[index][1]) ? this.data[index][1][0] : this.data[index][1], formattedValue: isArray(this.data[index][1]) ? this.data[index][1][1] : this.data[index][1].toLocaleString(),
					baseColor: this.data[index][2],
					fill: this._getColor(index)
				}];
			}
		}, {
			key: '_getColor',

			/**
    * Return the color for the given index.
    *
    * @param {int} index
    *
    * @return {string}
    */
			value: function _getColor(index) {
				if (this.fillType === 'solid') {
					return this.data[index][2];
				} else {
					return 'url(#gradient-' + index + ')';
				}
			}
		}, {
			key: '_getPathDefinition',

			/**
    * @param {int} index
    *
    * @return {string}
    */
			value: function _getPathDefinition(index) {
				var pathStr = '';
				var point = [];
				var paths = this.sectionPaths[index];

				for (var j = 0; j < paths.length; j++) {
					point = paths[j];
					pathStr += point[2] + point[0] + ',' + point[1] + ' ';
				}

				return pathStr;
			}
		}, {
			key: '_onMouseOver',

			/**
    * @param {Object} data
    *
    * @return {void}
    */
			value: function _onMouseOver(data) {
				d3.select(this).attr('fill', shadeColor(data.baseColor, -0.2));
			}
		}, {
			key: '_onMouseOut',

			/**
    * @param {Object} data
    *
    * @return {void}
    */
			value: function _onMouseOut(data) {
				d3.select(this).attr('fill', data.fill);
			}
		}, {
			key: '_addSectionLabel',

			/**
    * @param {Object} group
    * @param {int}	index
    *
    * @return {void}
    */
			value: function _addSectionLabel(group, index) {
				var i = index;
				var paths = this.sectionPaths[index];
				var sectionData = this._getSectionData(index)[0];
				var textStr = sectionData.label + ': ' + sectionData.formattedValue;
				var textFill = this.data[i][3] || this.label.fill;

				var textX = this.width / 2; // Center the text
				var textY = !this.isCurved ? // Average height of bases
				(paths[1][1] + paths[2][1]) / 2 : (paths[2][1] + paths[3][1]) / 2 + this.curveHeight / this.data.length;

				group.append('text').text(textStr).attr({
					'x': textX,
					'y': textY,
					'text-anchor': 'middle',
					'dominant-baseline': 'middle',
					'fill': textFill,
					'pointer-events': 'none'
				}).style('font-size', this.label.fontSize);
			}
		}]);

		return D3Funnel;
	})();

	/**
  * Check if the supplied value is an array.
  *
  * @param {*} value
  *
  * @return {bool}
  */
	function isArray(value) {
		return Object.prototype.toString.call(value) === '[object Array]';
	}

	/**
  * Extends an object with the members of another.
  *
  * @param {Object} a The object to be extended.
  * @param {Object} b The object to clone from.
  *
  * @return {Object}
  */
	function extend(a, b) {
		var prop;
		for (prop in b) {
			if (b.hasOwnProperty(prop)) {
				a[prop] = b[prop];
			}
		}
		return a;
	}

	/**
  * Shade a color to the given percentage.
  *
  * @param {string} color A hex color.
  * @param {number} shade The shade adjustment. Can be positive or negative.
  *
  * @return {string}
  */
	function shadeColor(color, shade) {
		var f = parseInt(color.slice(1), 16);
		var t = shade < 0 ? 0 : 255;
		var p = shade < 0 ? shade * -1 : shade;
		var R = f >> 16,
		    G = f >> 8 & 255;
		var B = f & 255;

		var converted = 16777216 + (Math.round((t - R) * p) + R) * 65536 + (Math.round((t - G) * p) + G) * 256 + (Math.round((t - B) * p) + B);

		return '#' + converted.toString(16).slice(1);
	}

	global.D3Funnel = D3Funnel;
})(window, d3);