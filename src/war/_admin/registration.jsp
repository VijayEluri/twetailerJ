<!doctype html>
<%@page
    language="java"
    contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"
    import="java.util.Enumeration"
    import="java.util.Locale"
    import="java.util.Map"
    import="java.util.ResourceBundle"
    import="com.dyuproject.openid.OpenIdUser"
    import="com.dyuproject.openid.RelyingParty"
    import="domderrien.i18n.LabelExtractor"
    import="domderrien.i18n.LabelExtractor.ResourceFileId"
    import="domderrien.i18n.LocaleController"
    import="domderrien.i18n.StringUtils"
    import="twetailer.connector.BaseConnector.Source"
    import="twetailer.dto.Consumer"
    import="twetailer.dto.Location"
    import="twetailer.dto.SaleAssociate"
    import="twetailer.dto.Store"
    import="twetailer.j2ee.BaseRestlet"
    import="twetailer.j2ee.LoginServlet"
    import="twetailer.validator.ApplicationSettings"
%><%
    // Application settings
    ApplicationSettings appSettings = ApplicationSettings.get();
    boolean useCDN = appSettings.isUseCDN();
    String cdnBaseURL = appSettings.getCdnBaseURL();

    // Locale detection
    Locale locale = LocaleController.getLocale(request);
    String localeId = LocaleController.getLocaleId(request);
%><html dir="ltr" lang="<%= localeId %>">
<head>
    <title>Sale Associate Registration Page</title>
    <meta http-equiv="content-type" content="text/html;charset=<%= StringUtils.HTML_UTF8_CHARSET %>" />
    <meta http-equiv="content-language" content="<%= localeId %>" />
    <meta name="copyright" content="<%= LabelExtractor.get(ResourceFileId.master, "product_copyright", locale) %>" />
    <link rel="shortcut icon" href="/favicon.ico" />
    <link rel="icon" href="/favicon.ico" type="image/x-icon"/>
    <%
    if (useCDN) {
    %><style type="text/css">
        @import "<%= cdnBaseURL %>/dojo/resources/dojo.css";
        @import "<%= cdnBaseURL %>/dijit/themes/tundra/tundra.css";
        @import "<%= cdnBaseURL %>/dojox/grid/resources/Grid.css";
        @import "<%= cdnBaseURL %>/dojox/grid/resources/tundraGrid.css";
        @import "/css/console.css";
    </style><%
    }
    else { // elif (!useCDN)
    %><style type="text/css">
        @import "/js/dojo/dojo/resources/dojo.css";
        @import "/js/dojo/dijit/themes/tundra/tundra.css";
        @import "/js/dojo/dojox/grid/resources/Grid.css";
        @import "/js/dojo/dojox/grid/resources/tundraGrid.css";
        @import "/css/console.css";
    </style><%
    } // endif (useCDN)
    %>
</head>
<body class="tundra">

    <div id="introFlash">
        <div id="introFlashWait"><span><%= LabelExtractor.get(ResourceFileId.third, "console_splash_screen_message", locale) %></span></div>
    </div>

    <%
    if (useCDN) {
    %><script
        djConfig="parseOnLoad: false, isDebug: true, useXDomain: true, baseUrl: './', modulePaths: { twetailer: '/js/twetailer', domderrien: '/js/domderrien' }, dojoBlankHtmlUrl: '/html/blank.html'"
        src="<%= cdnBaseURL %>/dojo/dojo.xd.js"
        type="text/javascript"
    ></script><%
    }
    else { // elif (!useCDN)
    %><script
        djConfig="parseOnLoad: false, isDebug: false, baseUrl: '/js/dojo/dojo/', modulePaths: { twetailer: '/js/twetailer', domderrien: '/js/domderrien' }, dojoBlankHtmlUrl: '/html/blank.html'"
        src="/js/dojo/dojo/dojo.js"
        type="text/javascript"
    ></script><%
    } // endif (useCDN)
    %>

    <div id="topContainer" dojoType="dijit.layout.BorderContainer" gutters="false" style="height: 100%;">
        <jsp:include page="/_includes/banner_protected.jsp">
            <jsp:param name="pageForAssociate" value="<%= Boolean.FALSE.toString() %>" />
            <jsp:param name="isLoggedUserAssociate" value="<%= Boolean.FALSE.toString() %>" />
            <jsp:param name="consumerName" value="Administrator" />
        </jsp:include>
        <div
            dojoType="dijit.layout.ContentPane"
            id="centerZone"
            region="center"
        >
            <div dojoType="dijit.layout.StackContainer" id="wizard" jsId="wizard" style="margin-left:25%;margin-right:25%;width:50%;">
                <div dojoType="dijit.layout.ContentPane" jsId="step0">
                    <fieldset class="entityInformation">
                        <legend>Action Selection</legend>
                        <p style="font-weight:bold;">Direct accesses:</p>
                        <p>
                            <button disabled="true" dojoType="dijit.form.Button"><< Previous</button>
                            <button dojoType="dijit.form.Button" onclick="wizard.selectChild(step1);dijit.byId('<%= Location.POSTAL_CODE %>').focus();">New Location >></button>
                            <button dojoType="dijit.form.Button" onclick="wizard.selectChild(step2);dijit.byId('<%= Store.LOCATION_KEY %>').focus();">New Store >></button>
                            <button dojoType="dijit.form.Button" onclick="wizard.selectChild(step3);dijit.byId('<%= SaleAssociate.STORE_KEY %>').focus();">New Sale Associate >></button>
                        </p>
                        <p style="font-weight:bold;">Other tools:</p>
                        <ul>
                            <li>Administrative console:
                            <a href="https://appengine.google.com/dashboard?app_id=anothersocialeconomy">hosted</a> --
                            <a href="http://127.0.0.1:9999/_ah/admin">local</a>;</li>
                            <li>Registration console:
                            <a href="https://anothersocialeconomy.appspot.com/_admin/registration.jsp">hosted</a> --
                            <a href="http://127.0.0.1:9999/_admin/registration.jsp">local</a>;</li>
                            <li>Monitoring console:
                            <a href="https://anothersocialeconomy.appspot.com/_admin/monitoring.jsp">hosted</a> --
                            <a href="http://127.0.0.1:9999/_admin/monitoring.jsp">local</a>.</li>
                        </ul>
                    </fieldset>
                </div>
                <div dojoType="dijit.layout.ContentPane" jsId="step1" style="display:hidden;">
                    <fieldset class="entityInformation" id="innerStep1">
                        <legend>Location Creation/Retrieval</legend>
                        <form id="locationInformation">
                            <div>
                                <label for="<%= Location.POSTAL_CODE %>">Postal Code</label><br/>
                                <input dojoType="dijit.form.TextBox" id="<%= Location.POSTAL_CODE %>" name="<%= Location.POSTAL_CODE %>" style="width:10em;" type="text" value="" />
                            </div>
                            <div>
                                <label for="<%= Location.COUNTRY_CODE %>">Country Code</label><br/>
                                <select dojoType="dijit.form.Select" name="countryCode">
                                    <option value="CA" selected="true">Canada</option>
                                    <option value="US">United States of America</option>
                                </select>
                            </div>
                        </form>
                        <p>
                            <button dojoType="dijit.form.Button" onclick="wizard.back();"><< Previous</button>
                            <button dojoType="dijit.form.Button" onclick="localModule.createLocation();">Next >></button>
                        </p>
                    </fieldset>
                </div>
                <div dojoType="dijit.layout.ContentPane" jsId="step2" style="display:hidden;">
                    <fieldset class="entityInformation" id="innerStep2">
                        <legend>Store Creation</legend>
                        <form id="storeInformation">
                            <div>
                                <label for="<%= Store.LOCATION_KEY %>">Location Key</label><br/>
                                <input dojoType="dijit.form.TextBox" id="<%= Store.LOCATION_KEY %>" name="<%= Store.LOCATION_KEY %>" style="width:10em;" type="text" value="" />
                            </div>
                            <div>
                                <label for="<%= Store.NAME %>">Store Name</label><br/>
                                <input dojoType="dijit.form.TextBox" name="<%= Store.NAME %>" style="width:20em;" type="text" value="" />
                            </div>
                            <div>
                                <label for="<%= Store.ADDRESS %>">Address</label><br/>
                                <input dojoType="dijit.form.TextBox" name="<%= Store.ADDRESS %>" style="width:30em;" type="text" value="" />
                            </div>
                            <div>
                                <label for="<%= Store.EMAIL %>">Email</label><br/>
                                <input dojoType="dijit.form.TextBox" name="<%= Store.EMAIL %>" style="width:10em;" type="text" value="" />
                            </div>
                            <div>
                                <label for="<%= Store.PHONE_NUMBER %>">Phone Number</label><br/>
                                <input dojoType="dijit.form.TextBox" name="<%= Store.PHONE_NUMBER %>" style="width:10em;" type="text" value="" />
                            </div>
                            <div>
                                <label for="<%= Store.REGISTRAR_KEY %>">Registrar Key</label><br/>
                                <input dojoType="dijit.form.TextBox" name="<%= Store.REGISTRAR_KEY %>" style="width:10em;" type="text" value="" />
                            </div>
                            <div>
                                <label for="<%= Store.REVIEW_SYSTEM_KEY %>">Review System Key</label><br/>
                                <input dojoType="dijit.form.TextBox" name="<%= Store.REVIEW_SYSTEM_KEY %>" style="width:10em;" type="text" value="" />
                            </div>
                            <div>
                                <label for="<%= Store.URL %>">Website URL</label><br/>
                                <input dojoType="dijit.form.TextBox" name="<%= Store.URL %>" style="width:10em;" type="text" value="" />
                            </div>
                        </form>
                        <p>
                            <button dojoType="dijit.form.Button" onclick="wizard.back();dijit.byId('<%= Location.POSTAL_CODE %>').focus();"><< Previous</button>
                            <button dojoType="dijit.form.Button" onclick="localModule.createStore();dijit.byId('<%= SaleAssociate.STORE_KEY %>').focus();">Next >></button>
                        </p>
                    </fieldset>
                    <fieldset class="entityInformation">
                        <legend>Store Retrieval</legend>
                        <ul id="storeList">
                        </ul>
                        <p>
                            <button dojoType="dijit.form.Button" onclick="localModule.getStores();">Get Stores</button>
                        </p>
                    </fieldset>
                </div>
                <div dojoType="dijit.layout.ContentPane" jsId="step3" style="display:hidden;">
                    <fieldset class="entityInformation" id="innerStep3">
                        <legend>Sale Associate Creation</legend>
                        <form id="saleAssociateInformation">
                            <div>
                                <label for="<%= SaleAssociate.STORE_KEY %>">Store Key</label><br/>
                                <input dojoType="dijit.form.TextBox" id="<%= SaleAssociate.STORE_KEY %>" name="<%= SaleAssociate.STORE_KEY %>" style="width:10em;" type="text" value="" />
                            </div>
                            <div>
                                <label for="<%= SaleAssociate.CONSUMER_KEY %>">Consumer Key</label><br/>
                                <input dojoType="dijit.form.TextBox" id="<%= SaleAssociate.CONSUMER_KEY %>" name="<%= SaleAssociate.CONSUMER_KEY %>" style="width:20em;" type="text" value="" />
                            </div>
                        </form>
                        <p>
                            <button dojoType="dijit.form.Button" onclick="wizard.back();dijit.byId('<%= Store.LOCATION_KEY %>').focus();"><< Previous</button>
                            <button dojoType="dijit.form.Button" onclick="localModule.createSaleAssociate();">Next >></button>
                        </p>
                    </fieldset>
                    <fieldset class="entityInformation">
                        <legend>Sale Associate Retrieval (for the specified Store Key)</legend>
                        <ul id="saleAssociateList">
                        </ul>
                        <p>
                            <button dojoType="dijit.form.Button" onclick="localModule.getSaleAssociates();">Get Sale Associates</button>
                        </p>
                    </fieldset>
                    <fieldset class="entityInformation">
                        <legend>Consumer Retrieval (for the E-mail Address, the Jabber Id, or the Twitter Name--in this order)</legend>
                        <ul id="consumerList">
                        </ul>
                        <p>
                            <button dojoType="dijit.form.Button" onclick="localModule.getConsumer();">Get Consumer</button>
                        </p>
                    </fieldset>
                </div>
                <div dojoType="dijit.layout.ContentPane" jsId="step5" style="display:hidden;">
                    <fieldset class="entityInformation" id="innerStep5">
                        <legend>Repeat Again ;)</legend>
                        <p>The Sale Associate can now tweet to @twetailer to supply his/her own tags.</p>
                        <p>
                            <button dojoType="dijit.form.Button" onclick="wizard.selectChild(step1);dijit.byId('<%= Location.POSTAL_CODE %>').focus();"><< Another Location</button>
                            <button dojoType="dijit.form.Button" onclick="wizard.selectChild(step2);dijit.byId('<%= Store.LOCATION_KEY %>').focus();"><< Another Store</button>
                            <button dojoType="dijit.form.Button" onclick="wizard.selectChild(step3);dijit.byId('<%= SaleAssociate.STORE_KEY %>').focus();"><< Another Sale Associate</button>
                            <button disabled="true" dojoType="dijit.form.Button">Next >></button>
                        </p>
                    </fieldset>
                </div>
            </div>
        </div>
        <div dojoType="dijit.layout.ContentPane" id="footerZone" region="bottom">
            <%= LabelExtractor.get("product_rich_copyright", locale) %>
        </div>
    </div>

    <script type="text/javascript">
    dojo.addOnLoad(function(){
        dojo.require('dojo.parser');
        dojo.require('dijit.Dialog');
        dojo.require('dijit.layout.BorderContainer');
        dojo.require('dijit.layout.ContentPane');
        dojo.require('dijit.layout.StackContainer');
        dojo.require('dijit.form.Button');
        dojo.require('dijit.form.Form');
        dojo.require('dijit.form.Select');
        dojo.require('dijit.form.TextBox');
        dojo.require('dojox.analytics.Urchin');
        dojo.addOnLoad(function(){
            dojo.parser.parse();
            dojo.fadeOut({
                node: 'introFlash',
                delay: 50,
                onEnd: function() {
                    dojo.style('introFlash', 'display', 'none');
                }
            }).play();<%
            if (!"localhost".equals(request.getServerName()) && !"127.0.0.1".equals(request.getServerName()) && !"10.0.2.2".equals(request.getServerName())) { %>
            new dojox.analytics.Urchin({ acct: 'UA-11910037-2' });<%
            } %>
            dojo.byId('logoutLink').href = '<%= com.google.appengine.api.users.UserServiceFactory.getUserService().createLogoutURL(request.getRequestURI()) %>';
        });
    });

    var localModule = new Object();
    localModule.createLocation = function() {
        dojo.animateProperty({
            node: 'innerStep1',
            properties: { backgroundColor: { end: 'yellow' } }
        }).play();
        dojo.xhrPost({
            headers: { 'content-type': 'application/json; charset=<%= StringUtils.HTML_UTF8_CHARSET %>' },
            putData: dojo.formToJson('locationInformation'),
            handleAs: 'json',
            load: function(response, ioArgs) {
                if (response !== null && response.success) {
                    dijit.byId('<%= Store.LOCATION_KEY %>').focus();
                    dijit.byId('<%= Store.LOCATION_KEY %>').set('value', response.resource.<%= Location.KEY %>);
                    wizard.forward();
                }
                else {
                    alert(response.message+'\nurl: '+ioArgs.url);
                }
                dojo.animateProperty({
                    node: 'innerStep1',
                    properties: { backgroundColor: { end: 'transparent' } }
                }).play();
            },
            error: function(message, ioArgs) { alert(message+'\nurl: '+ioArgs.url); },
            url: '/API/Location/'
        });
    };
    localModule.createStore = function() {
        dojo.animateProperty({
            node: 'innerStep2',
            properties: { backgroundColor: { end: 'yellow' } }
        }).play();
        var data = dojo.formToObject('storeInformation');
        data.locationKey = parseInt(data.locationKey); // Otherwise it's passed as a String
        dojo.xhrPost({
            headers: { 'content-type': 'application/json; charset-<%= StringUtils.HTML_UTF8_CHARSET %>' },
            putData: dojo.toJson(data),
            handleAs: 'json',
            load: function(response, ioArgs) {
                if (response !== null && response.success) {
                    dijit.byId('<%= SaleAssociate.STORE_KEY %>').focus();
                    dijit.byId('<%= SaleAssociate.STORE_KEY %>').set('value', response.resource.<%= Store.KEY %>);
                    wizard.forward();
                }
                else {
                    alert(response.message+'\nurl: '+ioArgs.url);
                }
                dojo.animateProperty({
                    node: 'innerStep2',
                    properties: { backgroundColor: { end: 'transparent' } }
                }).play();
            },
            error: function(message, ioArgs) { alert(message+'\nurl: '+ioArgs.url); },
            url: '/API/Store/'
        });
    };
    localModule.getStores = function() {
        var locationKey = parseInt(dijit.byId('<%= Store.LOCATION_KEY %>').get('value'));
        if (locationKey.length == 0 || isNaN(locationKey)) {
            alert('You need to specify a valid Location key');
            dijit.byId('<%= Store.LOCATION_KEY %>').focus();
            return;
        }
        dojo.xhrGet({
            headers: { 'content-type': 'application/x-www-form-urlencoded; charset=<%= StringUtils.HTML_UTF8_CHARSET %>' },
            content: { <%= Store.LOCATION_KEY %>: locationKey },
            handleAs: 'json',
            load: function(response, ioArgs) {
                if (response !== null && response.success) {
                    var placeHolder = dojo.byId('storeList');
                    placeHolder.innerHTML = '';
                    dojo.forEach(response.resources, function(store, i) {
                        var listItem = dojo.doc.createElement('li');
                        var onclickHandler =
                            'var saKeyField = dijit.byId(\'<%= SaleAssociate.STORE_KEY %>\');' +
                            'saKeyField.set(\'value\', \'' + store.<%= Store.KEY %> + '\');' +
                            'saKeyField.focus();' +
                            'wizard.forward();' +
                            'return false;';
                        listItem.innerHTML =
                            'Name: <a href="#" onclick="' + onclickHandler + '">' + store.<%= Store.NAME %> + '</a>, ' +
                            'Address: ' + store.<%= Store.ADDRESS %> + ', ' +
                            'Phone Number: ' + store.<%= Store.PHONE_NUMBER %>;
                        placeHolder.appendChild(listItem);
                    });
                }
                else {
                    alert(response.message+'\nurl: '+ioArgs.url);
                }
            },
            error: function(message, ioArgs) { alert(message+'\nurl: '+ioArgs.url); },
            url: '/API/Store/'
        });
    };
    localModule.createSaleAssociate = function() {
        dojo.animateProperty({
            node: 'innerStep3',
            properties: { backgroundColor: { end: 'yellow' } }
        }).play();
        var data = dojo.formToObject('saleAssociateInformation');
        data.storeKey = parseInt(data.storeKey); // Otherwise it's passed as a String
        if (data.consumerKey == null || isNaN(data.consumerKey) || data.consumerKey.length == 0) {
            delete data.consumerKey;
        }
        else {
            data.consumerKey = parseInt(data.consumerKey); // Otherwise it's passed as a String
        }
        dojo.xhrPost({
            headers: { 'content-type': 'application/json; charset=<%= StringUtils.HTML_UTF8_CHARSET %>' },
            putData: dojo.toJson(data),
            handleAs: 'json',
            load: function(response, ioArgs) {
                if (response !== null && response.success) {
                    // No visual feedback
                    wizard.forward();
                }
                else {
                    alert(response.exceptionMessage+'\nurl: '+ioArgs.url+'\n\n'+response.originalExceptionMessage);
                }
                dojo.animateProperty({
                    node: 'innerStep3',
                    properties: { backgroundColor: { end: 'transparent' } }
                }).play();
            },
            error: function(message, ioArgs) { alert(message+'\nurl: '+ioArgs.url); },
            url: '/API/SaleAssociate/'
        });
    };
    localModule.getSaleAssociates = function() {
        var storeKey = parseInt(dijit.byId('<%= SaleAssociate.STORE_KEY %>').get('value'));
        if (storeKey.length == 0 || isNaN(storeKey)) {
            alert('You need to specify a valid Store key');
            dijit.byId('<%= SaleAssociate.STORE_KEY %>').focus();
            return;
        }
        dojo.xhrGet({
            headers: { 'content-type': 'application/x-www-form-urlencoded; charset=<%= StringUtils.HTML_UTF8_CHARSET %>' },
            content: { <%= SaleAssociate.STORE_KEY %>: storeKey },
            handleAs: 'json',
            load: function(response, ioArgs) {
                if (response !== null && response.success) {
                    var placeHolder = dojo.byId('saleAssociateList');
                    placeHolder.innerHTML = '';
                    dojo.forEach(response.resources, function(saleAssociate, i) {
                        var listItem = dojo.doc.createElement('li');
                        listItem.innerHTML =
                            'Consumer key: ' + saleAssociate.<%= SaleAssociate.CONSUMER_KEY %>;
                        placeHolder.appendChild(listItem);
                    });
                }
                else {
                    alert(response.message+'\nurl: '+ioArgs.url);
                }
            },
            error: function(message, ioArgs) { alert(message+'\nurl: '+ioArgs.url); },
            url: '/API/SaleAssociate/'
        });
    };
    localModule.getConsumer = function() {
        var consumerKey = dijit.byId('<%= SaleAssociate.CONSUMER_KEY %>').get('value');
        if (consumerKet.length == 0) {
            alert('You need to specify a consumer key!');
            dijit.byId('<%= SaleAssociate.CONSUMER_KEY %>').focus();
            return;
        }
        var parameters = { <%= SaleAssociate.CONSUMER_KEY %>: consumerKey };
        dojo.xhrGet({
            headers: { 'content-type': 'application/x-www-form-urlencoded; charset=<%= StringUtils.HTML_UTF8_CHARSET %>' },
            content: parameters,
            handleAs: 'json',
            load: function(response, ioArgs) {
                if (response !== null && response.success) {
                    var placeHolder = dojo.byId('consumerList');
                    placeHolder.innerHTML = '';
                    var consumer = response.resource;
                    var listItem = dojo.doc.createElement('li');
                    listItem.innerHTML =
                        'Key: <a href="#" onclick="javascript:dijit.byId(\'<%= SaleAssociate.CONSUMER_KEY %>\').set(\'value\',' + consumer.<%= Consumer.KEY %> + ');return false;">' + consumer.<%= Consumer.KEY %> + '</a>, ' +
                        'Name: ' + consumer.<%= Consumer.NAME %> + ', ' +
                        'E-mail Address: <a href="mailto:' + consumer.<%= Consumer.EMAIL %> + '">' + consumer.<%= Consumer.EMAIL %> + '</a>, ' +
                        'Jabber Id: <a href="xmpp:' + consumer.<%= Consumer.JABBER_ID %> + '">' + consumer.<%= Consumer.JABBER_ID %> + '</a>, ' +
                        'Twitter Name: <a href="https://twitter.com/' + consumer.<%= Consumer.TWITTER_ID %> +'" target="_blank">' + consumer.<%= Consumer.TWITTER_ID %> + '</a>';
                    placeHolder.appendChild(listItem);
                }
                else {
                    alert(response.message+'\nurl: '+ioArgs.url);
                }
            },
            error: function(message, ioArgs) { alert(message+'\nurl: '+ioArgs.url); },
            url: '/API/Consumer/' + consumerKey
        });
    };
    </script>
</body>
</html>
