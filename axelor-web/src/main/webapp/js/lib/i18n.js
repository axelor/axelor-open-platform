(function() {

var bundle = {};

function gettext(key) {
	var message = bundle[key] || key;
	if (message && arguments.length > 1) {
		for(var i = 1 ; i < arguments.length ; i++) {
			var placeholder = new RegExp('\\{' + (i-1) + '\\}', 'g');
			var value = arguments[i];
			message = message.replace(placeholder, value);
		}
	}
	return message;
}

gettext.put = function(messages) {
	message = messages || {};
	for(var key in messages) {
		bundle[key] = messages[key];
	}
};

this._t = gettext;

}).call(this);