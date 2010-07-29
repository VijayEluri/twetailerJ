<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
<%@page
    language="java"
    contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"
    import="twetailer.validator.ApplicationSettings"
    import="twetailer.validator.LocaleValidator"
%><%
    // Application settings
    ApplicationSettings appSettings = ApplicationSettings.get();
    boolean useCDN = appSettings.isUseCDN();
    String cdnBaseURL = appSettings.getCdnBaseURL();

    // Getting parameters
    String localeId = request.getParameter("lg");
    String width = request.getParameter("w");
    String height = request.getParameter("h");
    if (localeId == null) { localeId = "en"; }
    if (width == null) { width = "200"; }
    if (height == null) { height = "400"; }
%><html xmlns="http://www.w3.org/1999/xhtml" dir="ltr" lang="<%= localeId %>">
<head>
    <style type="text/css"><%
        if (useCDN) {
        %>
        @import "<%= cdnBaseURL %>/dojo/resources/dojo.css";
        @import "<%= cdnBaseURL %>/dijit/themes/tundra/tundra.css";<%
        }
        else { // elif (!useCDN)
        %>
        @import "/js/dojo/dojo/resources/dojo.css";
        @import "/js/dojo/dijit/themes/tundra/tundra.css";<%
        } // endif (useCDN)
        %>
        @import "/css/golf/widget.css";

        body { margin: 10px; }
        .name, .n, code { color: green; }
        .value, .v { color: blue; }
   </style>
</head>
<body class="tundra">
    <iframe
        src="/widget/golf.jsp?lg=<%= localeId %>&postalCode=H3C2N6&countryCode=CA&referralId=000-000"
        style="width:<%= width %>px;height:<%= height %>px;float:right;border:0 none;"
        frameborder="0"
        type="text/html"
    ></iframe>
    <h1>ezToff widget context</h1>
    <p><a href="http://eztoff.com/"><b>ezToff</b></a> is a golf specific implementation of the <a href="http://anothersocialeconomy.com/">Another Social Economy</a> (ASE) engine.</p>
    <p>
        If the ASE engine is multi-channels (e-mail, Twitter, Facebook, SMS, Android, iPhone, Web, etc.), <b>ezToff</b> offers its widget to help players initiating their requests.
        The widget can be embedded on any webpage of the participating golf courses or associations, being given the right referral identifier is used.
        When golfers have successfully created their requests with the <b>ezToff</b> widget, all communications continue trough e-mail.
        No golfer registration is required as the system is widely open.
        And no system requirement on the golf course-side other than inserting the code snippet below into the golf course website.
    </p>
    <p>Contact the <b>ezToff</b> staff at <a href="mailto:support@eztoff.com?subject=Question(s) related to the ezToff widget">support@eztoff.com</a> for more information.</p>
    <h1>Widget parameters</h1>
    <p>
        Among the parameters listed below, only two are mandatory: <code>referralId</code> and <code>postalCode</code>.
        Other parameters have a generic default value.
    </p>
    <p>
        The values for the parameters should be defined as part of the URL used to get the <b>ezToff</b> widget on your site.
        Each value should be specified with the sequence: <code>&lt;parameter-name&gt;&lt;value&gt;</code>.
        Note that some values should be <a href="http://en.wikipedia.org/wiki/Percent-encoding">encoded for URLs</a> to avoid collisions with others.
    </p>
    <ul>
        <li><code>referralId</code>: golf course identifier (mandatory).</li>
        <li><code>postalCode</code>: postal code from the golf course address (mandatory).</li>
        <li><code>countryCode</code>:  ISO code of the golf course location (optional, default: <code><%= LocaleValidator.DEFAULT_COUNTRY_CODE %></code>).</li>
        <li><code>languageId</code>:  ISO code of the interface language (optional, default: <code><%= LocaleValidator.DEFAULT_LANGUAGE %></code>).</li>
        <li><code>color</code>: text color of the input fields' label (optional, default: <code>white</code>).</li>
        <li><code>color-title</code>: text color of the wizard panes' title (optional, default: <code>green</code>).</li>
        <li><code>background-color</code>: color of the wizard panes' background (optional, default: <code>#5ddc1e</code>).</li>
        <li><code>background-image</code>: image url of the wizard panes' background (optional, default: &lt;none&gt;).</li>
        <li><code>font-size</code>: text size of the input fields' label (optional, default: <code>10px</code>).</li>
        <li><code>font-size-title</code>: text size of the wizard pane's title (optional, default: <code>12px</code>).</li>
        <li><code>font-family</code>: name of the text font (optional, default: <code>Tahoma, Verdana, arial</code>).</li>
    </ul>
    <h1>Code snippet</h1>
    <p>
        <pre
            style="border:1px solid gray; padding:15px;float:left;"
>&lt;iframe
    src="http://twetailer.appspot.com/widget/golf.jsp?<span class="n">languageId</span>=<span class="v">fr_CA</span>&<span class="n">postalCode</span>=<span class="v">H3C2N6</span>&<span class="n">countryCode</span>=<span class="v">CA</span>&<span class="n">referralId</span>=<span class="v">6456TRT435454-6878</span>"
    style="width:200px;height:400px;border:0 none;"
    frameborder="0"
    type="text/html"
&gt;&lt;/iframe&gt;</pre>
    </p>

    <% if (!"localhost".equals(request.getServerName())) { %><script type="text/javascript">
    var _gaq = _gaq || [];
    _gaq.push(['_setAccount', 'UA-11910037-2']);
    _gaq.push(['_trackPageview']);
    (function() {
        var ga = document.createElement('script');
        ga.type = 'text/javascript';
        ga.async = true;
        ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
        var s = document.getElementsByTagName('script')[0];
        s.parentNode.insertBefore(ga, s);
    })();
    </script><% } %>

</body>
</html>
