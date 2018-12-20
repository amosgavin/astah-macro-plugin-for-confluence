/**
 * java のオブジェクト
 */
var app = {
	/**
	 * dummy function
	 */
	commandExecute : function(commandName) {
		console.log('commandExecute : ' + commandName);
	},
	openBrowser : function(href) {
		console.log('openBrowser : ' + href);
		window.open(href, '_blank');
	}	
};

/**
 * リンクをクリックしたときに呼ばれる<br>
 * "app" には、java の Javascript2Java の instance が<br>
 * bind されているので、その method を呼び出している
 */
function doClick(href) {
	app.openBrowser(href);
	return false;
}

function doExecute(commandName) {
	app.commandExecute(commandName);
	return false;
}