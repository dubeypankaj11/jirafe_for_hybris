<%@ page language="Java" session="true" import="de.hybris.platform.util.localization.Localization" %><html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
    <link rel="stylesheet"  type="text/css" media="screen" href="https://www.jirafe.com/dashboard/css/hybris_ui.css" />
    <script type="text/javascript" src="https://www.jirafe.com/dashboard/js/hybris_ui.js"></script>
  </head>
  <body>
    <div id="container" style="background:white;margin:auto;width:1170px;"> 
      <div id="jirafe"></div>
      <script type="text/javascript">
        var jirafeToken = "${param.t}";
	if (typeof jQuery != 'undefined') { 
		(function($) {
			 $('#jirafe').jirafe({
			    api_url: "${param.u}",
			    api_token: jirafeToken,
                            app_id: "${param.a}",
                            locale: "${param.l}",
			    version: "${param.v}"
			 });

		})(jQuery);
	}
	setTimeout(function() {
                var m = $(".jSysMsgTxt");

                if (m.length == 1) {
                  var msg = (jirafeToken == "") ? "<%=Localization.getLocalizedString("jirafereportcockpit.no-auth-token", new String[]{request.getParameter("user")})%>" : "<%=Localization.getLocalizedString("jirafereportcockpit.unexpected-error")%>";

                  m.text(msg);
                } else if ($('mod-jirafe') == undefined) {
		  $('messages').insert ("<ul class=\"messages\"><li class=\"error-msg\"></li></ul>");
		} 
	}, 2000);
      </script>
    </div>
  </body>
</html>
