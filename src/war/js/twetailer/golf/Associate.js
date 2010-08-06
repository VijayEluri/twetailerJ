(function() { // To limit the scope of the private variables

    var module = dojo.provide("twetailer.golf.Associate");

    dojo.require("twetailer.golf.Common");

    /* Set of local variables */
    var _common = twetailer.golf.Common,
        _getLabel,
        _grid,
        _gridCellNode,
        _gridRowIndex,
        _queryPointOfView = _common.POINT_OF_VIEWS.SALE_ASSOCIATE;

    /**
     * Initializer
     *
     * @param {String} locale Identifier of the chosen locale
     */
    module.init = function(locale) {
        _getLabel = _common.init(locale);

        // Attach the contextual menu to the DataGrid instance
        // Note: initialization code grabbed in the dojo test file: test_grid_tooltip_menu.html
        _grid = dijit.byId("demandList");
        dijit.byId("demandListCellMenu").bindDomNode(_grid.domNode);
        _grid.onCellContextMenu = function(e) {
            _gridCellNode = e.cellNode;
            _gridRowIndex = e.rowIndex;
        };
        // _grid.setSortIndex(9, false); // 9 == position of the column 'modificationDate'

        // Fetch
        var dfd = _common.loadRemoteDemands(null, _queryPointOfView); // No modificationDate means "load all active Demands"
        dfd.addCallback(function(response) { _common.processDemandList(response.resources, _grid); });
    };

    var _demandViewDecoration = "<span class='dijitReset dijitInline silkIcon silkIconDemandView'></span>${0}";

    var _proposalCreateDecoration = "<a href='#' onclick='twetailer.golf.Associate.displayProposalForm(${0},null);return false;' title='${1}'><span class='dijitReset dijitInline silkIcon silkIconProposalAdd'></span>${1}</a>";
    var _proposalUpdateDecoration = "<a href='#' onclick='twetailer.golf.Associate.displayProposalForm(${1},${0});return false;' title='${2}'><span class='dijitReset dijitInline silkIcon silkIconProposalUpdate'></span>${0}</a>";
    var _proposalViewDecoration = "<a href='#' onclick='twetailer.golf.Associate.displayProposalForm(${1},${0});return false;' title='${2}'><span class='dijitReset dijitInline silkIcon silkIconProposalView'></span>${0}</a>";

    /**
     * Override of the formatter to be able to place the Demand icon before demand key
     *
     * @param {Number[]} demandKey identifier of the demand
     * @param {Number} rowIndex index of the data in the grid, used by the trigger launching the Proposal properties pane
     * @return {String} Formatter with the Demand icon before the demand key
     */
    module.displayDemandKey = function(demandKey, rowIndex) {
        // TODO: check the demand state in order to use the classname silkIconDemandConfirmed
        try {
            return dojo.string.substitute(_demandViewDecoration, [demandKey]);
        }
        catch(ex) { alert(ex);}
        return demandKey;
    };

    /**
     * Formatter to be able to place the "Create Proposal" link in front of the proposal key list
     *
     * @param {Number[]} proposalKeys List of proposal keys
     * @param {Number} rowIndex index of the data in the grid, used by the trigger launching the Proposal properties pane
     * @return {String} Formatter list of one link per proposal key, a link opening a dialog with the proposal detail
     */
    module.displayProposalKeys = function(proposalKeys, rowIndex) {
        // TODO: check the demand state in order to use the classname silkIconProposalConfirmed
        var item = _grid.getItem(rowIndex);
        if (item === null) {
            return;
        }
        var cellContent = "";
        var modifiableDemand = item.state == _common.STATES.PUBLISHED;
        if (modifiableDemand) {
            var createLabel = dojo.string.substitute(
                _proposalCreateDecoration,
                [rowIndex, _getLabel("console", "ga_cmenu_createProposal")]
            );
            cellContent = createLabel;
        }
        if (proposalKeys == null || proposalKeys.length == 0) {
            return cellContent;
        }
        var updateLabel = dojo.string.substitute(
            modifiableDemand ? _proposalUpdateDecoration : _proposalViewDecoration,
            ["${0}", "${1}", _getLabel("console", modifiableDemand ? "ga_cmenu_updateProposal" : "ga_cmenu_viewProposal")]
        );
        if (modifiableDemand) {
            cellContent += "<br/>";
        }
        return cellContent + _common.displayProposalKeys(proposalKeys, rowIndex, updateLabel);
    };

    /**
     * Open a dialog box with the attributes of the identified proposal. If there's no
     * proposalKey, the variable set by the contextual menu handler for the Demand grid
     * is used to identified a selected grid row and to propose a dialog for a new
     * proposal creation.
     *
     * @param {Number} proposedRowIndex (Optional) index given when a link on the proposal key is activated
     * @param {Number} proposalKey (Optional) index given when a link on the proposal key is activated
     */
    module.displayProposalForm = function(proposedRowIndex, proposalKey) {
        // rowIndex bind to the handler
        if (proposedRowIndex == null) {
            if (_gridRowIndex === null) {
                return;
            }
            proposedRowIndex = _gridRowIndex;
        }

        var item = _grid.getItem(proposedRowIndex);
        if (item === null) {
            return;
        }

        var proposalForm = dijit.byId("proposalForm");
        proposalForm.reset();

        dijit.byId("demand.key").attr("value", item.key[0]);
        dijit.byId("demand.state").attr("value", _getLabel("master", "cl_state_" + item.state[0]));
        var dueDate = dojo.date.stamp.fromISOString(item.dueDate[0]);
        dijit.byId("proposal.date").attr("value", dueDate);
        dijit.byId("proposal.date").constraints.min = new Date();
        dijit.byId("proposal.time").attr("value", dueDate);
        if (dojo.isArray(item.criteria)) {
            dijit.byId("demand.criteria").attr("value", item.criteria.join(" "));
        }
        dijit.byId("demand.quantity").attr("value", item.quantity[0]);

        if (proposalKey == null) {
            proposalForm.attr("title", _getLabel("console", "ga_cmenu_createProposal"));
            dijit.byId("proposalFormSubmitButton").attr("label", _getLabel("console", "ga_cmenu_createProposal"));

            dojo.query(".updateButton").style("display", "");
            dojo.query(".existingAttribute").style("display", "none");
            dojo.query(".closeButton").style("display", "none");
        }
        else {
            proposalForm.attr("title", _getLabel("console", "ga_cmenu_viewProposal", [proposalKey]));
            dijit.byId("proposalFormSubmitButton").attr("label", _getLabel("console", "ga_cmenu_updateProposal", [proposalKey]));
            dijit.byId("proposalFormCancelButton").attr("label", _getLabel("console", "ga_cmenu_cancelProposal", [proposalKey]));
            dijit.byId("proposalFormCloseButton").attr("label", _getLabel("console", "ga_cmenu_closeProposal", [proposalKey]));
            dojo.query(".existingAttribute").style("display", "");

            _loadProposal(proposalKey);
        }
        proposalForm.show();
        dijit.byId('proposal.price').focus();
    };

    /**
     * Load the identified proposal by its key from a local cache or from the remote back-end.
     * The control is passed to the <code>_fetchProposal()</code> for the update of dialog box
     * with the Proposal attributes.
     *
     * @param {String} proposalKey Identifier of the proposal to load
     */
    var _loadProposal = function(proposalKey) {
        if (_common.isProposalCached(proposalKey)) {
            _fetchProposal(_common.getCachedProposal(proposalKey));
        }
        else {
            var dfd = _common.loadRemoteProposal(proposalKey, _queryPointOfView);
            dfd.addCallback(function(response) { _fetchProposal(_common.getCachedProposal(proposalKey)); });
        }
    };

    /**
     * Use the given Proposal to fetch the corresponding dialog box
     *
     * @param {Proposal} proposal Object to represent
     */
    var _fetchProposal = function(proposal) {
        dijit.byId("proposal.key").attr("value", proposal.key);
        dijit.byId("proposal.state").attr("value", _getLabel("master", "cl_state_" + proposal.state));
        dijit.byId("proposal.price").attr("value", proposal.price);
        dijit.byId("proposal.total").attr("value", proposal.total);
        var dateObject = dojo.date.stamp.fromISOString(proposal.dueDate);
        dijit.byId("proposal.date").attr("value", dateObject);
        dijit.byId("proposal.time").attr("value", dateObject);
        if (dojo.isArray(proposal.criteria)) {
            dijit.byId("proposal.criteria").attr("value", proposal.criteria.join(" "));
        }
        dijit.byId("proposal.modificationDate").attr("value", _common.displayDateTime(proposal.modificationDate));

        var closeableState = proposal.state == _common.STATES.CONFIRMED;
        if (closeableState) {
            dojo.query(".updateButton").style("display", "none");
            dojo.query(".closeButton").style("display", "");
        }
        else {
            dojo.query(".updateButton").style("display", "");
            dojo.query(".closeButton").style("display", "none");
        }
        dijit.byId("proposalFormSubmitButton").attr("disabled", proposal.state == _common.STATES.DECLINED);
    };

    /**
     * Call the back-end to create or update a Proposal with the given attribute
     *
     * @param {Object} data Set of attributes built from the <code>form</code> embedded in the dialog box
     */
    module.updateProposal = function(data) {
        if (isNaN(data.key)) {
            delete data.key;
        }
        if (isNaN(data.total)) {
            delete data.total;
        }
        data.criteria = data.criteria.split(/(?:\s|\n|,|;)+/);
        data.dueDate = _common.toISOString(data.date, data.time);
        data.hashTags = ["golf"]; // TODO: offer a checkbox to allow the #demo mode

        var dfd = _common.updateRemoteProposal(data, data.key);
        dfd.addCallback(function(response) { setTimeout(function() { module.loadNewDemands(); }, 7000); });
    };

    /**
     * Call the back-end to create or update a Proposal with the given attribute
     *
     * @param {Object} data Set of attributes built from the <code>form</code> embedded in the dialog box
     */
    module.cancelProposal = function() {
        dijit.byId("proposalForm").hide();

        var proposalKey = dijit.byId("proposal.key").attr("value");
        var proposal = _common.getCachedProposal(proposalKey);

        var messageId = proposal.state == _common.STATES.CONFIRMED ?
              "ga_alert_cancelConfirmedProposal" :
              "ga_alert_cancelPublishedProposal";

        var demandKey = proposal.demandKey;
        if (!confirm(_getLabel("console", messageId, [proposalKey, demandKey]))) {
            return;
        }

        var data = { state: _common.STATES.CANCELLED };

        var dfd = _common.updateRemoteProposal(data, proposalKey);
        dfd.addCallback(function(response) { module.loadNewDemands() });
    };

    /**
     * Call the back-end to close the proposal displayed in the property pane
     */
    module.closeProposal = function() {
        dijit.byId("proposalForm").hide();

        var proposalKey = dijit.byId("proposal.key").attr("value");
        var data = { state: _common.STATES.CLOSED };

        var dfd = _common.updateRemoteProposal(data, proposalKey);
        dfd.addCallback(function(response) { module.loadNewDemands() });
    }

    /**
     * Call the back-end to get the new Demands
     */
    module.loadNewDemands = function() {
        var lastDemand = _common.getLastDemand();
        var lastModificationDate = lastDemand == null ? null : lastDemand.modificationDate;
        var dfd = _common.loadRemoteDemands(lastModificationDate, _queryPointOfView);
        dfd.addCallback(function(response) { dijit.byId("refreshButton").resetTimeout(); _common.processDemandList(response.resources, _grid); });
    };
})(); // End of the function limiting the scope of the private variables