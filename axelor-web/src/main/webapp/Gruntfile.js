module.exports = function(grunt) {

var fs = require('fs');

function jsList(input) {
	
	var lines = fs.readFileSync(input).toString().split('\n'),
		files = [];

	lines.forEach(function (line) {
        var match = /\s*(\'|\")(.*?.js)('|\")\s*(,)?/.exec(line);
        if (match) {
        	var file = match[2];
        	files.push(file);
        }
    });

	return files;
}

grunt.initConfig({
	pkg: grunt.file.readJSON('package.json'),
	uglify: {
		options: {
			compress: false
		},
		desktop: {
			files: {
				'js/application.min.js': jsList('js/application.js')
			}
		}
	},
	cssmin: {
		options: {
			keepSpecialComments: 0
		},
		desktop: {
			files: {
				'css/application.min.css': ['css/application.css']
			}
		}
	},
	clean: {
		desktop: ['js/application.min.js',
	 		      'css/application.min.css']
	}
});
grunt.loadNpmTasks('grunt-contrib-uglify');
grunt.loadNpmTasks('grunt-contrib-cssmin');
grunt.loadNpmTasks('grunt-contrib-clean');

//Default task(s).
grunt.registerTask('default', ['uglify', 'cssmin']);

};
