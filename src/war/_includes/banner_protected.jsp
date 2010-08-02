<%@page
    language="java"
    contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"
    import="java.util.Enumeration"
    import="java.util.Locale"
    import="java.util.ResourceBundle"
    import="java.net.URL"
    import="java.net.URLEncoder"
    import="domderrien.i18n.LabelExtractor"
    import="domderrien.i18n.LocaleController"
    import="domderrien.i18n.LabelExtractor.ResourceFileId"
    import="domderrien.jsontools.JsonArray"
    import="domderrien.jsontools.JsonObject"
    import="domderrien.jsontools.JsonParser"
    import="domderrien.i18n.LocaleController"
    import="twetailer.dto.HashTag.RegisteredHashTag"
    import="twetailer.j2ee.LoginServlet"
    import="twetailer.validator.ApplicationSettings"
    import="twetailer.validator.LocaleValidator"
%><%
    // Locale detection
    String localeId = request.getParameter("localeId");
    Locale locale = LocaleController.getLocale(localeId);

    // Get the current page url
    String queryString = request.getQueryString();
    String fromPageURL = request.getRequestURI();
    int defaultPageIdx = fromPageURL.lastIndexOf("index.jsp");
    if (defaultPageIdx == fromPageURL.length() - "index.jsp".length()) {
        fromPageURL = fromPageURL.substring(0, defaultPageIdx);
    }
    if (queryString != null) {
        fromPageURL += "?" + queryString;
    }
    fromPageURL = URLEncoder.encode(fromPageURL, "UTF-8");

    // Get the parameters passed to this template
    boolean pageForAssociate = Boolean.valueOf(request.getParameter("pageForAssociate"));
    boolean isLoggedUserAssociate = Boolean.valueOf(request.getParameter("isLoggedUserAssociate"));
    String consumerName = request.getParameter("consumerName");

    String verticalId = request.getParameter("verticalId");
%>
        <div dojoType="dijit.layout.ContentPane" id="headerZone" region="top">
            <div id="brand"><%
                if (RegisteredHashTag.golf.toString().equals(verticalId)) {
                   %><jsp:include page="/_includes/brands/golf.jsp"><jsp:param name="localeId" value="<%= localeId %>" /></jsp:include><%
                }
                else { // if ("".equals(verticalId)) {
                   %><jsp:include page="/_includes/brands/default.jsp"><jsp:param name="localeId" value="<%= localeId %>" /></jsp:include><%
                }
            %></div>
            <div id="navigation">
                <ul>
                    <!--  Normal order because they are left aligned -->
                    <% if (pageForAssociate) {
                    %><li><a href="./"><%= LabelExtractor.get(ResourceFileId.third, "navigation_consumer", locale) %></a></li>
                    <li><span class="active"><%= LabelExtractor.get(ResourceFileId.third, "navigation_sale_associate", locale) %></span></li><%
                    }
                    else {
                    %><li><span class="active"><%= LabelExtractor.get(ResourceFileId.third, "navigation_consumer", locale) %></span></li>
                    <% if(isLoggedUserAssociate) {
                    %><li><a href="./associate.jsp"><%= LabelExtractor.get(ResourceFileId.third, "navigation_sale_associate", locale) %></a></li><%
                    }
                    } %>
                    <!--  Reverse order because they are right aligned -->
                    <li class="subItem"><a href="javascript:dijit.byId('aboutPopup').show();" title="<%= LabelExtractor.get(ResourceFileId.third, "navigation_about", locale) %>"><%= LabelExtractor.get(ResourceFileId.third, "navigation_about", locale) %></a></li>
                    <li class="subItem"><a href="/logout?<%= LoginServlet.FROM_PAGE_URL_KEY %>=<%= fromPageURL %>" title="<%= LabelExtractor.get(ResourceFileId.third, "navigation_sign_out", locale) %>"><%= LabelExtractor.get(ResourceFileId.third, "navigation_sign_out", locale) %></a></li>
                    <li class="subItem" style="color: orange; font-weight: bold; padding: 0 20px;">
                        <a href="/console/#profile" title="<%= LabelExtractor.get(ResourceFileId.third, "navigation_username_editIt", locale) %>">
                            <span style="color:orange;"><%= LabelExtractor.get(ResourceFileId.third, "navigation_welcome_user", new Object[] { consumerName}, locale) %></span>
                        </a>
                    </li>
                </ul>
            </div>
        </div>
