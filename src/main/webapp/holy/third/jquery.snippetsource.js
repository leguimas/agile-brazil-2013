(function($) {

	$.fn.appendSnippetSourceURL = function(url, type) {
		$(this).append('<div />');
		var panel = $(this).children('div:last');
		var u = url.split('/');
		var name = u[u.length - 1];
		panel.append('<h3>' + name + '</h3>');
		panel.append('<div><pre class="snippet"></pre></div>');
		panel.find('div').hide();
		panel.togglePanel();
		var opts = {
			url : url,
			dataType : 'text',
			target : panel,
			snippetType : type,
			success : function(text) {
				var pre = $(this.target).find('pre');
				var type = this.snippetType;
				pre.text(text);
				pre.snippet(type, {
					style : "acid",
					showNum : false
				});
			}
		};
		$.ajax(opts);
	}

}(jQuery));
