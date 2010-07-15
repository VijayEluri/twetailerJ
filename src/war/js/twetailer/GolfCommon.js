(function() { // To limit the scope of the private variables

    var module = dojo.provide("twetailer.GolfCommon");

    dojo.require("twetailer.Common");

    /* Set of local variables */
    var _common = twetailer.Common,
        _getLabel,
        _locations = {},
        _demands = {},
        _proposals = {},
        _lastDemand,
        _supportGeolocation;

    module.STATES = _common.STATES;
    module.POINT_OF_VIEWS = _common.POINT_OF_VIEWS;

    /**
     * Initializer
     *
     * @param {String} locale Identifier of the chosen locale
     * @return {Function} Shortcut on the local function getting localized labels
     */
    module.init = function(locale) {
        _getLabel = _common.init(locale, "detectLocationButton");

        // Return the shortcut on domderrien.i18n.LabelExtractor.getFrom()
        return _getLabel;
    };

    /**
     * Date formatter
     *
     * @param {String} ISO representation of a Date, as generated by the back-end
     * @return {String} Simply formatted date
     */
    module.displayDate = function(serializedDate) {
        try {
            var dateObject = dojo.date.stamp.fromISOString(serializedDate);
            return dojo.date.locale.format(dateObject, {selector: "date"});
        }
        catch(ex) {
            console.log("displayDate('" + serializedDate + "') -- ex: " + ex.message);
            return "<span class='invalidData' title='" + _getLabel("console", "error_invalid_date", [serializedDate]) + "'>" + _getLabel("console", "error_invalid_data") + "</span>";
        }
    };

    /**
     * Date & Time formatter
     *
     * @param {String} ISO representation of a Date, as generated by the back-end
     * @return {String} Simply formatted date
     */
    module.displayDateTime = function(serializedDate) {
        try {
            var dateObject = dojo.date.stamp.fromISOString(serializedDate);
            return dojo.date.locale.format(dateObject, {selector: "dateTime"});
        }
        catch(ex) {
            console.log("displayDateTime('" + serializedDate + "') -- ex: " + ex.message);
            return "<span class='invalidData' title='" + _getLabel("console", "error_invalid_date", [serializedDate]) + "'>" + _getLabel("console", "error_invalid_data") + "</span>";
        }
    };

    /**
     * Criteria formatter
     *
     * @param {String[]} criteria List of criteria, as generated by the back-end
     * @return {String} Serialized criteria list
     */
    module.displayCriteria = function(criteria) {
        if (criteria == null) {
            return "";
        }
        if (dojo.isArray(criteria)) {
            return criteria.join(" ");
        }
        console.log("displayCriteria(" + criteria + ") is not an Array");
        return "<span class='invalidData' title='" + _getLabel("console", "error_invalid_array") + "'>" + _getLabel("console", "error_invalid_data") + "</span>";
    };

    var _ccTwitterDecoration = ["<a href='http://twitter.com/", "' target='twTwitter'>", "</a> "];
    var _ccEmailDecoration = ["<a href='mailto:", "'>", "</a> "];

    /**
     * CC formatter
     *
     * @param {String[]} ccList List of CC-ed users' identifier, as generated by the back-end
     * @return {String} Serialized identifier list
     */
    module.displayCCList = function(ccList) {
        if (ccList == null) {
            return "";
        }
        if (dojo.isArray(ccList)) {
            var value = [], cc, ccLinked;
            var limit = ccList.length;
            for (var idx = 0; idx < limit; idx ++) {
                cc = ccList[idx];
                ccLinked = cc;
                var decorationRef = _ccEmailDecoration;
                if (cc.charAt(0) == '@') {
                    ccLinked = cc.substring(1);
                    decorationRef = _ccTwitterDecoration;
                }
                else if (cc.indexOf('@') == -1) {
                    decorationRef = _ccTwitterDecoration;
                }
                value.push(decorationRef[0]);
                value.push(ccLinked);
                value.push(decorationRef[1]);
                value.push(cc);
                value.push(decorationRef[2]);
            }
            return value.join("");
        }
        console.log("displayCC(" + ccList + ") is not an Array");
        return "<span class='invalidData' title='" + _getLabel("console", "error_invalid_array") + "'>" + _getLabel("console", "error_invalid_data") + "</span>";
    };

    /**
     * Formatter for the list of attached proposal keys
     *
     * @param {Number[]} proposalKeys List of proposal keys
     * @param {Number} rowIndex index of the data in the grid, used by the trigger launching the Proposal properties pane
     * @param {Array} decoration Link definition wrapping a proposal key with:
     *                   ${0}: place holder for the proposalKey
     *                   ${1}: place holder for the rowIndex
     * @return {String} Formatter list of one link per proposal key, a link opening a dialog with the proposal detail
     */
    module.displayProposalKeys = function(proposalKeys, rowIndex, decoration) {
        if (proposalKeys == null) {
            return "";
        }
        if (dojo.isArray(proposalKeys)) {
            var value = [], pK;
            var limit = proposalKeys.length;
            for (var idx = 0; idx < limit; idx ++) {
                pK = proposalKeys[idx];
                value.push(dojo.string.substitute(decoration || "${0}", [pK, rowIndex]));
            }
            return value.join(" ");
        }
        return "<span class='invalidData' title='" + _getLabel("console", "error_invalid_array") + "'>" + _getLabel("console", "error_invalid_data") + "</span>";
    };

    var _previouslySelectedCountryCode = null;

    /**
     * Helper modifying on the fly the constraints for the postal code field
     *
     * @param {Object} countryCode New country code
     * @param {Object} postalCodeFieldId Identifier of the postal code field
     */
    module.updatePostalCodeFieldConstraints = function(countryCode, postalCodeFieldId) {
        if (_previouslySelectedCountryCode != countryCode) {
            var pcField = dijit.byId(postalCodeFieldId);
            if (pcField != null) {
                pcField.attr("regExp", _getLabel("console", "location_postalCode_regExp_" + countryCode));
                pcField.attr("invalidMessage", _getLabel("console", "location_postalCode_invalid_" + countryCode));
                pcField.focus();
            }
            _previouslySelectedCountryCode = countryCode;
        }
    }

    /**
     * Locale formatter with a lookup in the Location list
     *
     * @param {String[]} locationKey Identifier of the Location to represente
     * @return {String} Composition of the postal and the country codes in a link to Google Maps
     */
    module.displayLocale = function(locationKey) {
        if (locationKey == null) {
            return "";
        }
        var location = _locations[locationKey];
        if (location != null) {
            return location.postalCode + " " + location.countryCode;
        }
        return "<span class='invalidData' title='" + _getLabel("console", "error_invalid_locale") + "'>" + _getLabel("console", "error_invalid_data") + "</span>";
    };

    /**
     * Get the specified demand from the cache.
     *
     * @param {String} demandKey Identifier of the demand to load
     * @return {Demand} Identified demand if it exists, <code>null</code> otherwise;
     */
    module.getCachedDemand = function(demandKey) {
        return _demands[demandKey];
    };

    /**
     * Load the demands modified after the given date from the back-end
     *
     * @param {Date} lastModificationDate (Optional) Date to considered before returning the demands (ISO formatted)
     * @param {String} pointOfView (Optional) operation initiator point of view, default to CONSUMER server-side
     * @return {dojo.Deferred} Object that callers can use to attach callbacks and errbacks
     */
    module.loadRemoteDemands = function(lastModificationISODate, pointOfView) {
        dijit.byId("demandListOverlay").show();
        var dfd = dojo.xhrGet({
            content: {
                pointOfView: pointOfView || _common.POINT_OF_VIEWS.CONSUMER,
                lastModificationDate: lastModificationISODate,
                related: ["Location"]
            },
            handleAs: "json",
            load: function(response, ioArgs) {
                if (response !== null && response.success) {
                    // Deferred callback will process the list
                    _lastDemand = null;
                    var resources = response.resources;
                    var resourceNb = resources == null ? 0 : resources.length;
                    if (0 < resourceNb) {
                        _lastDemand = resources[0];
                        for (var i=0; i<resourceNb; ++i) {
                            // Add the updated demand into the cache
                            var resource = resources[i];
                            _demands[resource.key] = resource;
                            // Remove the associated and possibly updated proposals from the cache
                            var proposalKeys = resource.proposalKeys;
                            var proposalKeyNb = proposalKeys == null ? 0 : proposalKeys.length;
                            for (var j=0; j<proposalKeyNb; j++) {
                                delete _proposals[proposalKeys[j]];
                            }
                        }
                        if (0 < resourceNb) {
                            // Add the locations to the cache
                            var resource = resources[0];
                            var locations = resource.related == null ? null : resource.related.Location;
                            var locationNb = locations == null ? 0 : locations.length;
                            for (var k = 0; k < locationNb; k++) {
                                var location = locations[k];
                                _locations[location.key] = location;
                            }
                            delete resource.related;
                        }
                    }
                }
                else {
                    alert(response.message+"\nurl: "+ioArgs.url);
                }
                dijit.byId("demandListOverlay").hide();
                return response;
            },
            error: function(message, ioArgs) {
                dijit.byId("demandListOverlay").hide();
                _common.handleError(message, ioArgs);
            },
            preventCache: true,
            url: "/API/Demand/"
        });
        return dfd;
    };

    /**
     * Callback processing a list of demands that should replace the current grid content
     *
     * @param {Demand[]} List of demands to insert into the grid
     * @param {DataGrid} Reference on the grid to fetch
     */
    module.processDemandList = function(resources, grid) {
        // Prepare the data store
        var demandStore = new dojo.data.ItemFileWriteStore({
            data: { identifier: 'key', items: resources }
        });
        // Fetch the grid with the data
        demandStore.fetch({
            query : {},
            onComplete : function(items, request) {
                if (grid.selection !== null) {
                    grid.selection.clear();
                }
                grid.setStore(demandStore);
            },
            error: function(message, ioArgs) {
                _common.handleError(message, ioArgs);
            },
            sort:  [{attribute: "quantity", descending: true}]
        });
    };

    /**
     * Return the last modified demand.
     *
     * @return {Demand} Identified demand if it exists, <code>null</code> otherwise;
     */
    module.getLastDemand = function(demandKey) {
        return _lastDemand;
    };

    /**
     * Call the back-end to create or update a Demand with the given attribute
     *
     * @param {Object} data Set of attributes built from the <code>form</code> embedded in the dialog box
     * @param {Number} demandKey (Optional) demand identifier, picked-up in the given JSON bag if missing
     * @return {dojo.Deferred} Object that callers can use to attach callbacks and errbacks
     */
    module.updateRemoteDemand = function(data, demandKey) {
        dijit.byId('demandListOverlay').show();
        demandKey = demandKey || data.key;
        var dfd = (demandKey == null ? dojo.xhrPost : dojo.xhrPut)({
            headers: { "content-type": "application/json; charset=utf-8" },
            postData: dojo.toJson(data),
            putData: dojo.toJson(data),
            handleAs: "json",
            load: function(response, ioArgs) {
                if (response !== null && response.success) {
                    var demand = response.resource;
                    _demands[demand.key] = demand;
                }
                else {
                    alert(response.message+"\nurl: "+ioArgs.url);
                }
                dijit.byId('demandListOverlay').hide();
            },
            error: function(message, ioArgs) {
                dijit.byId("demandListOverlay").hide();
                _common.handleError(message, ioArgs);
            },
            url: "/API/Demand/" + (demandKey == null ? "" : demandKey)
        });
        return dfd;
    };

    /**
     * Tell of the specified proposal has been cached.
     *
     * @param {String} proposalKey Identifier of the proposal to load
     * @return {Boolean} <code>true</code> if the proposal is cached, <code>false</code> otherwise
     */
    module.isProposalCached = function(proposalKey) {
        return _proposals[proposalKey] != null;
    };

    /**
     * Get the specified proposal from the cache.
     *
     * @param {String} proposalKey Identifier of the proposal to load
     * @return {Proposal} Identified proposal if it exists, <code>null</code> otherwise;
     */
    module.getCachedProposal = function(proposalKey) {
        return _proposals[proposalKey];
    };

    /**
     * Load the identified proposal by its key from the remote back-end.
     *
     * @param {String} proposalKey Identifier of the proposal to load
     * @param {String} pointOfView (Optional) operation initiator point of view, default to SALE_ASSOCIATE
     * @return {dojo.Deferred} Object that callers can use to attach callbacks and errbacks
     */
    module.loadRemoteProposal = function(proposalKey, pointOfView) {
        dijit.byId("proposalFormOverlay").show();
        var dfd = dojo.xhrGet({
            content: {
                pointOfView: pointOfView || _common.POINT_OF_VIEWS.SALE_ASSOCIATE,
                related: ["Store"]
            },
            handleAs: "json",
            load: function(response, ioArgs) {
                if (response !== null && response.success) {
                    var resource = response.resource;
                    _proposals[proposalKey] = resource;
                }
                else {
                    alert(response.message+"\nurl: "+ioArgs.url);
                }
                dijit.byId("proposalFormOverlay").hide();
                return response;
            },
            error: function(message, ioArgs) {
                dijit.byId("proposalFormOverlay").hide();
                _common.handleError(message, ioArgs);
            },
            url: "/API/Proposal/" + proposalKey
        });
        return dfd;
    };

    /**
     * Call the back-end to create or update a Proposal with the given attribute
     *
     * @param {Object} data Set of attributes built from the <code>form</code> embedded in the dialog box
     * @param {Number} proposalKey (Optional) proposal identifier, picked-up in the given JSON bag if missing
     * @return {dojo.Deferred} Object that callers can use to attach callbacks and errbacks
     */
    module.updateRemoteProposal = function(data, proposalKey, pointOfView) {
        dijit.byId('demandListOverlay').show();
        proposalKey = proposalKey || data.key;
        var dfd = (proposalKey == null ? dojo.xhrPost : dojo.xhrPut)({
            headers: { "content-type": "application/json; charset=utf-8" },
            postData: dojo.toJson(data),
            putData: dojo.toJson(data),
            handleAs: "json",
            load: function(response, ioArgs) {
                if (response !== null && response.success) {
                    var proposal = response.resource;
                    _proposals[proposal.key] = proposal;
                }
                else {
                    alert(response.message+"\nurl: "+ioArgs.url);
                }
                dijit.byId('demandListOverlay').hide();
            },
            error: function(message, ioArgs) {
                dijit.byId("demandListOverlay").hide();
                _common.handleError(message, ioArgs);
            },
            url: "/API/Proposal/" + (proposalKey == null ? "" : proposalKey)
        });
        return dfd;
    };

    module.setLocation = function(locationKey, postalCodeField, countryCodeField) {
        var location = _locations[locationKey];
        if (location != null) {
            postalCodeField.attr("value", location.postalCode);
            countryCodeField.attr("value", location.countryCode);
            return;
        }
    }

    module.getBrowserLocation = function(overlayId) {
        var eventName = "browserLocationCodeAvailable";
        var handle = dojo.subscribe(eventName, function(postalCode, countryCode) {
            dijit.byId("demand.postalCode").attr("value", postalCode);
            dijit.byId("demand.countryCode").attr("value", countryCode);
            dijit.byId("demand.postalCode").focus();
            dojo.unsubscribe(handle);
        })
        _common.getBrowserLocation(eventName, overlayId);
    }

    module.showDemandLocaleMap = function() {
        var postalCode = dijit.byId("demand.postalCode").attr("value");
        var countryCode = dijit.byId("demand.countryCode").attr("value");
        _common.showMap(postalCode, countryCode);
    }

    module.showStoreLocaleMap = function() {
        alert("Not yet implemented!");
//        var postalCode = dijit.byId("demand.postalCode").attr("value");
//        var countryCode = dijit.byId("demand.countryCode").attr("value");
//        _common.showMap(postalCode, countryCode);
    }
})(); // End of the function limiting the scope of the private variables
