<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
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
    import="domderrien.i18n.LocaleController"
    import="domderrien.i18n.LabelExtractor.ResourceFileId"
    import="twetailer.validator.ApplicationSettings"
    import="twetailer.validator.CommandSettings.State"
    import="twetailer.dto.Consumer"
    import="twetailer.dto.Demand"
    import="twetailer.dto.Location"
    import="twetailer.dto.HashTag"
    import="twetailer.dto.Seed"
    import="twetailer.dto.Store"
    import="twetailer.dto.SaleAssociate"
    import="twetailer.j2ee.BaseRestlet"
    import="twetailer.j2ee.LoginServlet"
    import="twetailer.connector.BaseConnector.Source"
%><%
    // Application settings
    ApplicationSettings appSettings = ApplicationSettings.get();
    boolean useCDN = appSettings.isUseCDN();
    String cdnBaseURL = appSettings.getCdnBaseURL();

    // Locale detection
    Locale locale = LocaleController.getLocale(request);
    String localeId = LocaleController.getLocaleId(request);

    // Get the logged user record
    OpenIdUser loggedUser = BaseRestlet.getLoggedUser(request);
    Consumer consumer = LoginServlet.getConsumer(loggedUser);
    Long saleAssociateKey = consumer.getSaleAssociateKey();

    // Redirects non sale associates
    if (saleAssociateKey == null) {
        response.sendRedirect("./");
    }

    // Detects the vertical context
    boolean useVertical = false;
    String verticalId = null;
    String forwardedUriAttribute = (String) request.getAttribute("javax.servlet.forward.servlet_path");
    if (forwardedUriAttribute != null) {
        String[] hashtags = HashTag.getHashTagsArray();
        for (int idx=0; !useVertical && idx<hashtags.length; idx++) {
            verticalId = hashtags[idx];
            useVertical = forwardedUriAttribute.startsWith("/console/" + verticalId);
        }
    }
%><html xmlns="http://www.w3.org/1999/xhtml" dir="ltr" lang="<%= localeId %>">
<head>
    <title><%= LabelExtractor.get(ResourceFileId.third, "ga_localized_page_name", locale) %></title>
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8">
    <link rel="shortcut icon" href="/favicon.ico" />
    <link rel="icon" href="/favicon.ico" type="image/x-icon"/>
    <style type="text/css"><%
        if (useCDN) {
        %>
        @import "<%= cdnBaseURL %>/dojo/resources/dojo.css";
        @import "<%= cdnBaseURL %>/dijit/themes/tundra/tundra.css";
        @import "<%= cdnBaseURL %>/dojox/grid/resources/Grid.css";
        @import "<%= cdnBaseURL %>/dojox/grid/resources/tundraGrid.css";
        @import "<%= cdnBaseURL %>/dojox/grid/enhanced/resources/tundraEnhancedGrid.css";
        @import "<%= cdnBaseURL %>/dojox/layout/resources/FloatingPane.css";
        @import "<%= cdnBaseURL %>/dojox/layout/resources/ExpandoPane.css";<%
        }
        else { // elif (!useCDN)
        %>
        @import "/js/dojo/dojo/resources/dojo.css";
        @import "/js/dojo/dijit/themes/tundra/tundra.css";
        @import "/js/dojo/dojox/grid/resources/Grid.css";
        @import "/js/dojo/dojox/grid/resources/tundraGrid.css";
        @import "/js/dojo/dojox/grid/enhanced/resources/tundraEnhancedGrid.css";
        @import "/js/dojo/dojox/layout/resources/FloatingPane.css";
        @import "/js/dojo/dojox/layout/resources/ExpandoPane.css";<%
        } // endif (useCDN)
        %>
        @import "/css/console.css";<%
        if (useVertical) {
        %>
        @import "/css/<%= verticalId %>/console.css";<%
        } // endif (useVertical)
        %>
    </style>
</head>
<body class="tundra">

    <div id="topBar"></div>

    <div id="introFlash">
        <div><span><%= LabelExtractor.get(ResourceFileId.third, "ui_splash_screen_message", locale) %></span></div>
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
            <jsp:param name="verticalId" value="golf" />
            <jsp:param name="localeId" value="<%= localeId %>" />
            <jsp:param name="pageForAssociate" value="<%= Boolean.TRUE.toString() %>" />
            <jsp:param name="isLoggedUserAssociate" value="<%= Boolean.toString(saleAssociateKey != null) %>" />
            <jsp:param name="consumerName" value="<%= consumer.getName() %>" />
        </jsp:include>
        <div dojoType="dijit.layout.BorderContainer" gutters="false" id="centerZone" region="center">
            <div dojoType="dijit.layout.ContentPane" region="top" style="margin:10px 10px 0 10px;">
                <div style="float:right;">
                    <select dojoType="dijit.form.FilteringSelect" onchange="dijit.byId('demandList').filter({<%= Demand.STATE %>:this.value});" style="">
                        <option value="*" selected="true"><%= LabelExtractor.get(ResourceFileId.third, "ga_stateSelector_anyState", locale) %></option>
                        <option value="<%= State.opened %>"><%= LabelExtractor.get(ResourceFileId.master, "cl_state_opened", locale) %></option>
                        <option value="<%= State.invalid %>"><%= LabelExtractor.get(ResourceFileId.master, "cl_state_invalid", locale) %></option>
                        <option value="<%= State.published %>"><%= LabelExtractor.get(ResourceFileId.master, "cl_state_published", locale) %></option>
                        <option value="<%= State.confirmed %>"><%= LabelExtractor.get(ResourceFileId.master, "cl_state_confirmed", locale) %></option>
                    </select>
                    <button
                        busyLabel="<%= LabelExtractor.get(ResourceFileId.third, "refreshing_button_state", locale) %>"
                        dojoType="dojox.form.BusyButton"
                        iconClass="silkIcon silkIconRefresh"
                        id="refreshButton"
                        onclick="twetailer.GolfAssociate.loadNewDemands();"
                    ><%= LabelExtractor.get(ResourceFileId.third, "refresh_button", locale) %></button>
                </div>
            </div>
            <div dojoType="dijit.Menu" id="demandListCellMenu" style="display: none;">
                <div dojoType="dijit.MenuItem" iconClass="silkIcon silkIconProposalAdd" onClick="twetailer.GolfAssociate.displayProposalForm();"><%= LabelExtractor.get(ResourceFileId.third, "ga_cmenu_createProposal", new String[] { "" }, locale) %></div>
                <div disabled="true" dojoType="dijit.MenuItem" iconClass="silkIcon silkIconProposalRemove"><%= LabelExtractor.get(ResourceFileId.third, "ga_cmenu_declineDemand", new String[] { "" }, locale) %></div>
            </div>
            <table
                dojoType="dojox.grid.EnhancedGrid"
                errorMessage="&lt;span class='dojoxGridError'&gt;<%= LabelExtractor.get(ResourceFileId.third, "ga_dataGrid_loadingError", locale) %>&lt;/span&gt;"
                id="demandList"
                region="center"
                rowMenu="cellMenu"
                rowsPerPage="20"
                sortFields="[{attribute: '<%= Demand.MODIFICATION_DATE %>',descending:true}]"
                style="font-size:larger;margin:10px;border:1px solid lightgrey;background:transparent"
            >
                <thead>
                    <tr>
                           <!-- IMPORTANT: update the call to _grid.setSortIndex() in GolfConsumer.js if you change the place of the column 'modificationDate' -->
                           <th field="<%= Demand.KEY %>" formatter="twetailer.GolfAssociate.displayDemandKey" styles="padding:2px 5px;"><%= LabelExtractor.get(ResourceFileId.third, "ga_theader_demandKey", locale) %></th>
                           <th field="<%= Demand.DUE_DATE %>" formatter="twetailer.GolfCommon.displayDateTime" styles="padding:2px 5px;text-align:right;" width="140px"><%= LabelExtractor.get(ResourceFileId.third, "ga_theader_dueDate", locale) %></th>
                           <th field="<%= Demand.EXPIRATION_DATE %>" formatter="twetailer.GolfCommon.displayDateTime" hidden="true" styles="padding:2px 5px;text-align:right;" width="140px"><%= LabelExtractor.get(ResourceFileId.third, "ga_theader_expirationDate", locale) %></th>
                           <th field="<%= Demand.LOCATION_KEY %>" formatter="twetailer.GolfCommon.displayLocale" styles="padding:2px 5px;"><%= LabelExtractor.get(ResourceFileId.third, "ga_theader_locale", locale) %></th>
                           <th field="<%= Demand.QUANTITY %>" styles="padding:2px 5px;text-align:right;"><%= LabelExtractor.get(ResourceFileId.third, "ga_theader_quantity", locale) %></th>
                           <th fields="<%= Demand.PROPOSAL_KEYS %>" formatter="twetailer.GolfAssociate.displayProposalKeys" styles="padding:2px 5px;" width="200px"><%= LabelExtractor.get(ResourceFileId.third, "ga_theader_proposalKeys", locale) %></th>
                           <th fields="<%= Demand.CRITERIA %>" formatter="twetailer.GolfCommon.displayCriteria" styles="padding:2px 5px;" width="40%"><%= LabelExtractor.get(ResourceFileId.third, "ga_theader_criteria", locale) %></th>
                           <th field="<%= Demand.STATE %>" styles="padding:2px 5px;"><%= LabelExtractor.get(ResourceFileId.third, "ga_theader_state", locale) %></th>
                           <th field="<%= Demand.MODIFICATION_DATE %>" formatter="twetailer.GolfCommon.displayDateTime" styles="padding:2px 5px;text-align:right;" width="140px"><%= LabelExtractor.get(ResourceFileId.third, "ga_theader_modificationDate", locale) %></th>
                           <th field="<%= Demand.CREATION_DATE %>" formatter="twetailer.GolfCommon.displayDateTime" hidden="true" styles="padding:2px 5px;text-align:right;" width="140px"><%= LabelExtractor.get(ResourceFileId.third, "ga_theader_creationDate", locale) %></th>
                           <!--th
                               cellType="dojox.grid.cells.Select"
                               editable="true"
                               field="status"
                               formatter="cpwr.ConsoleLogic.displayParameterStatus"
                               options="Not Set,Excluded,Always Included,Cache Bypasser"
                               values="NotSet,Excluded,Included,CacheBypasser"
                               width="20%"
                           >Status</th-->
                    </tr>
                </thead>
            </table>
        </div>
        <div dojoType="dijit.layout.ContentPane" id="footerZone" region="bottom">
            <%= LabelExtractor.get("product_copyright", locale) %>
        </div>
    </div>

    <div
        dojoType="dijit.Dialog"
        execute="twetailer.GolfAssociate.updateProposal"
        id="proposalForm"
        title="<%= LabelExtractor.get(ResourceFileId.third, "ga_proposalForm_formTitle_creation", locale) %>"
    >
        <fieldset class="entityInformation">
            <legend><%= LabelExtractor.get(ResourceFileId.third, "ga_demandInfo", locale) %></legend>
            <table class="demandForm" width="100%">
                <tr>
                    <td align="right"><%= LabelExtractor.get(ResourceFileId.third, "ga_demandForm_demandKey", locale) %></td>
                    <td><input dojoType="dijit.form.NumberTextBox" id="demand.key" name="demandKey" readonly="true" style="width:6em;" type="text" /> </td>
                </tr>
                <tr>
                    <td align="right"><%= LabelExtractor.get(ResourceFileId.third, "ga_demandForm_demandState", locale) %></td>
                    <td><input dojoType="dijit.form.TextBox" id="demand.state" readonly="true" type="text" /> </td>
                </tr>
                <tr>
                    <td align="right"><label for="demand.quantity"><%= LabelExtractor.get(ResourceFileId.third, "ga_demandForm_demandQuantity", locale) %></label></td>
                    <td><input dojoType="dijit.form.NumberTextBox" id="demand.quantity" readonly="true" style="width:3em;" type="text" /> </td>
                </tr>
                <tr>
                    <td align="right"><label for="demand.criteria"><%= LabelExtractor.get(ResourceFileId.third, "ga_demandForm_demandCriteria", locale) %></label></td>
                    <td><input dojoType="dijit.form.TextBox" id="demand.criteria" readonly="true" style="width:25em;" type="text" /></td>
                </tr>
            </table>
        </fieldset>
        <fieldset class="entityInformation">
            <legend><%= LabelExtractor.get(ResourceFileId.third, "ga_proposalInfo", locale) %></legend>
            <table class="demandForm" width="100%">
                <tr class="existingAttribute">
                    <td align="right"><%= LabelExtractor.get(ResourceFileId.third, "ga_proposalForm_proposalKey", locale) %></td>
                    <td><input dojoType="dijit.form.NumberTextBox" id="proposal.key" name="key" readonly="true" style="width:6em;" type="text" /> </td>
                </tr>
                <tr class="existingAttribute">
                    <td align="right"><%= LabelExtractor.get(ResourceFileId.third, "ga_proposalForm_proposalState", locale) %></td>
                    <td><input dojoType="dijit.form.TextBox" id="proposal.state" readonly="true" type="text" /> </td>
                </tr>
                <tr>
                    <td align="right"><label for="proposal.time"><%= LabelExtractor.get(ResourceFileId.third, "ga_proposalForm_proposalTime", locale) %></label></td>
                    <td>
                        <input dojoType="dijit.form.DateTextBox" id="proposal.date" name="date" required="true" type="text" />
                        <input constraints="{visibleIncrement:'T00:30:00',visibleRange:'T02:00:00'}" dojoType="dijit.form.TimeTextBox" id="proposal.time" name="time" required="true" type="text" value="T07:00:00" />
                    </td>
                </tr>
                <tr>
                    <td align="right"><label for="proposal.price"><%= LabelExtractor.get(ResourceFileId.third, "ga_proposalForm_proposalPrice", locale) %></label></td>
                    <td>$<input constraints="{min:0,places:2}" dojoType="dijit.form.NumberSpinner" id="proposal.price" name="price" required="true" style="width:7em;" type="text" /></td>
                </tr>
                <tr>
                    <td align="right"><label for="proposal.total"><%= LabelExtractor.get(ResourceFileId.third, "ga_proposalForm_proposalTotal", locale) %></label></td>
                    <td>$<input constraints="{min:0,places:2}" dojoType="dijit.form.NumberSpinner" id="proposal.total" name="total" style="width:7em;" type="text" /></td>
                </tr>
                <tr>
                    <td align="right"><label for="proposal.criteria"><%= LabelExtractor.get(ResourceFileId.third, "ga_proposalForm_proposalCriteria", locale) %></label></td>
                    <td><input dojoType="dijit.form.TextBox" id="proposal.criteria" name="criteria" style="width:25em;" type="text" /></td>
                </tr>
                <tr class="existingAttribute">
                    <td align="right"><%= LabelExtractor.get(ResourceFileId.third, "ga_proposalForm_proposalModificationDate", locale) %></td>
                    <td><input dojoType="dijit.form.TextBox" id="proposal.modificationDate" readonly="true" style="width:10em;" type="text" /> </td>
                </tr>
                <tr>
                    <td colspan="2" align="center">
                        <button class="updateButton" dojoType="dijit.form.Button" iconClass="silkIcon silkIconProposalAccept" id="proposalFormSubmitButton" onclick="return dijit.byId('proposalForm').isValid();" type="submit"></button>
                        <button class="existingAttribute" dojoType="dijit.form.Button" iconClass="silkIcon silkIconProposalCancel" id="proposalFormCancelButton" onclick="twetailer.GolfAssociate.cancelProposal();"></button>
                        <button class="existingAttribute closeButton" dojoType="dijit.form.Button" iconClass="silkIcon silkIconProposalAccept" id="proposalFormCloseButton" onclick="twetailer.GolfAssociate.closeProposal();"></button>
                    </td>
                </tr>
            </table>
        </fieldset>
        <div style="text-align:center;">
            <button dojoType="dijit.form.Button" iconClass="silkIcon silkIconClose" onclick="dijit.byId('proposalForm').hide();" ><%= LabelExtractor.get(ResourceFileId.third, "ga_closeDialog_button", locale) %></button>
        </div>
    </div>

    <div
        dojoType="dijit.Dialog"
        id="aboutPopup"
        title="<%= LabelExtractor.get(ResourceFileId.third, "about_dialog_title", locale) %>"
        href="/_includes/about.jsp"
    >
    </div>

    <div
       color="darkgreen"
       dojoType="dojox.widget.Standby"
       id="demandListOverlay"
       target="demandList"
    ></div>

    <div
       color="darkgreen"
       dojoType="dojox.widget.Standby"
       id="proposalFormOverlay"
       target="proposalForm"
    ></div>

    <script type="text/javascript">
    dojo.addOnLoad(function(){
        dojo.require("dojo.data.ItemFileWriteStore");
        dojo.require("dojo.date.locale");
        dojo.require("dojo.number");
        dojo.require("dojo.parser");
        dojo.require("dijit.Dialog");
        dojo.require("dijit.layout.BorderContainer");
        dojo.require("dijit.layout.ContentPane");
        dojo.require("dijit.layout.TabContainer");
        // dojo.require("dijit.form.CheckBox");
        // dojo.require("dijit.form.ComboBox");
        dojo.require("dijit.form.DateTextBox");
        dojo.require("dijit.form.FilteringSelect");
        dojo.require("dijit.form.NumberSpinner");
        dojo.require("dijit.form.NumberTextBox");
        // dojo.require("dijit.form.Textarea");
        dojo.require("dijit.form.TextBox");
        dojo.require("dijit.form.TimeTextBox");
        dojo.require("dojox.form.BusyButton");
        dojo.require("dojox.form.Rating");
        dojo.require("dojox.grid.EnhancedGrid");
        // dojo.require("dojox.layout.ExpandoPane");
        // dojo.require("dojox.secure");
        // dojo.require("dojox.widget.Portlet");
        dojo.require("dojox.widget.Standby");
        dojo.require("twetailer.GolfAssociate");
        dojo.addOnLoad(function(){
            dojo.parser.parse();
            twetailer.GolfAssociate.init("<%= localeId %>");
            dojo.fadeOut({
                node: "introFlash",
                delay: 50,
                onEnd: function() {
                    dojo.style("introFlash", "display", "none");
                }
            }).play();
        });
        // http://archive.dojotoolkit.org/nightly/dojotoolkit/dojox/image/tests/test_SlideShow.html
    });
    </script>

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
