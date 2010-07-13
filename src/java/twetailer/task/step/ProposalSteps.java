package twetailer.task.step;

import static twetailer.connector.BaseConnector.communicateToConsumer;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.jdo.PersistenceManager;

import twetailer.ClientException;
import twetailer.CommunicationException;
import twetailer.DataSourceException;
import twetailer.InvalidIdentifierException;
import twetailer.InvalidStateException;
import twetailer.ReservedOperationException;
import twetailer.connector.BaseConnector.Source;
import twetailer.dto.Command;
import twetailer.dto.Consumer;
import twetailer.dto.Demand;
import twetailer.dto.Entity;
import twetailer.dto.Location;
import twetailer.dto.Payment;
import twetailer.dto.Proposal;
import twetailer.dto.RawCommand;
import twetailer.dto.SaleAssociate;
import twetailer.dto.Store;
import twetailer.dto.Command.QueryPointOfView;
import twetailer.j2ee.BaseRestlet;
import twetailer.j2ee.MaezelServlet;
import twetailer.payment.AmazonFPS;
import twetailer.task.CommandLineParser;
import twetailer.task.RobotResponder;
import twetailer.validator.CommandSettings.Action;
import twetailer.validator.CommandSettings.Prefix;
import twetailer.validator.CommandSettings.State;
import domderrien.i18n.DateUtils;
import domderrien.i18n.LabelExtractor;
import domderrien.jsontools.JsonObject;

public class ProposalSteps extends BaseSteps {

    protected static AmazonFPS amazonFPS = new AmazonFPS();

    public static Proposal getProposal(PersistenceManager pm, Long proposalKey, Long ownerKey, QueryPointOfView pointOfView, Long saleAssociateKey, Long storeKey) throws ReservedOperationException, InvalidIdentifierException {
        Proposal output = null;

        if (QueryPointOfView.CONSUMER.equals(pointOfView)) {
            output = getProposalOperations().getProposal(pm, proposalKey, null, null);

            try {
                // Verify the Demand owner
                Demand demand = getDemandOperations().getDemand(pm, output.getDemandKey(), ownerKey);

                // Get payment related URL
                if (State.confirmed.equals(output.getState())) {
                    //
                    // TODO: verify the store record to check if it accepts AWS FPS payment
                    // TODO: verify that the proposal has a total cost value
                    //
                    // Cook the Amazon FPS Co-Branded service URL
                    String description = LabelExtractor.get(
                            "payment_transaction_description",
                            new Object[] {
                                    output.getSerializedCriteria(),
                                    demand.getSerializedCriteria()
                            },
                            Locale.ENGLISH // FIXME: get logged user's locale
                    );
                    try {
                        String transactionReference = Payment.getReference(ownerKey, demand.getKey(), proposalKey);
                        output.setAWSCBUIURL(amazonFPS.getCoBrandedServiceUrl(transactionReference, description, output.getTotal(), "USD"));
                    }
                    catch (Exception ex) {
                        throw new DataSourceException("Cannot compute the AWS FPS Co-Branded Service URL", ex);
                    }
                }
            }
            catch (Exception ex) {
                throw new ReservedOperationException(Action.list, Proposal.class.getName());
            }
        }
        else if (QueryPointOfView.SALE_ASSOCIATE.equals(pointOfView)) {
            if (saleAssociateKey == null && storeKey == null) {
                throw new ReservedOperationException(Action.list, Proposal.class.getName());
            }
            output = getProposalOperations().getProposal(pm, proposalKey, saleAssociateKey, storeKey);
        }
        else { // if (QueryPointOfView.anonymous.equals(pointOfView)) {
            throw new ReservedOperationException(Action.list, Proposal.class.getName());
        }

        return output;
    }

    public static List<Proposal> getProposals(PersistenceManager pm, JsonObject parameters, Long ownerKey, QueryPointOfView pointOfView, Long saleAssociateKey) throws ReservedOperationException, InvalidIdentifierException, DataSourceException {

        Map<String, Object> queryParameters = prepareQueryForSelection(parameters);
        int maximumResults = (int) parameters.getLong(BaseRestlet.MAXIMUM_RESULTS_PARAMETER_KEY);

        List<Proposal> output = null;

        if (QueryPointOfView.CONSUMER.equals(pointOfView)) {
            queryParameters.put(Proposal.CONSUMER_KEY, ownerKey);

            output = getProposalOperations().getProposals(pm, queryParameters, maximumResults);

            throw new RuntimeException("Not yet implemented!");
        }
        else if (QueryPointOfView.SALE_ASSOCIATE.equals(pointOfView)) {
            if (saleAssociateKey == null) {
                throw new ReservedOperationException(Action.list, Proposal.class.getName());
            }
            queryParameters.put(Proposal.OWNER_KEY, saleAssociateKey);

            output = getProposalOperations().getProposals(pm, queryParameters, maximumResults);
        }
        else { // if (QueryPointOfView.anonymous.equals(pointOfView)) {
            List<Location> locations = LocationSteps.getLocations(pm, parameters, true);

            output = getProposalOperations().getProposals(pm, queryParameters, locations, maximumResults);
        }

        return output;
    }

    public static List<Long> getProposalKeys(PersistenceManager pm, JsonObject parameters, Long ownerKey, QueryPointOfView pointOfView, Long saleAssociateKey) throws ReservedOperationException, DataSourceException {

        Map<String, Object> queryParameters = prepareQueryForSelection(parameters);
        int maximumResults = (int) parameters.getLong(BaseRestlet.MAXIMUM_RESULTS_PARAMETER_KEY);

        List<Long> output = null;

        if (QueryPointOfView.CONSUMER.equals(pointOfView)) {
            queryParameters.put(Proposal.CONSUMER_KEY, ownerKey);

            output = getProposalOperations().getProposalKeys(pm, queryParameters, maximumResults);

            throw new RuntimeException("Not yet implemented!");
        }
        else if (QueryPointOfView.SALE_ASSOCIATE.equals(pointOfView)) {
            if (saleAssociateKey == null) {
                throw new ReservedOperationException(Action.list, Proposal.class.getName());
            }
            queryParameters.put(Proposal.OWNER_KEY, saleAssociateKey);

            output = getProposalOperations().getProposalKeys(pm, queryParameters, maximumResults);
        }
        else { // if (QueryPointOfView.anonymous.equals(pointOfView)) {
            throw new ReservedOperationException(Action.list, Proposal.class.getName());
        }

        return output;
    }

    /**
     * Retrieve the list of proposal identifier the specified sale associate has created
     *
     * @param saleAssociateKey Identifier of the concerned sale associate
     * @return List of proposal identifiers
     *
     * @throws DataSourceException If the proposal key retrieval fails
     */
    protected static List<Long> getProposalKeys(Long saleAssociateKey) throws DataSourceException {
        if (saleAssociateKey == null) {
            return null;
        }
        PersistenceManager pm = getBaseOperations().getPersistenceManager();
        try {
            return getProposalKeys(pm, saleAssociateKey);
        }
        finally {
            pm.close();
        }
    }

    /**
     * Retrieve the list of proposal identifier the specified sale associate has created
     *
     * @param pm Persistence manager instance to use - let open at the end to allow possible object updates later
     * @param saleAssociateKey Identifier of the concerned sale associate
     * @return List of proposal identifiers
     *
     * @throws DataSourceException If the proposal key retrieval fails
     */
    protected static List<Long> getProposalKeys(PersistenceManager pm, Long saleAssociateKey) throws DataSourceException {
        if (saleAssociateKey == null) {
            return null;
        }
        // Get the keys of the proposals owned by the requester
        Map<String, Object> queryProposals = new HashMap<String, Object>();
        queryProposals.put(Command.OWNER_KEY, saleAssociateKey);
        queryProposals.put(Command.STATE_COMMAND_LIST, Boolean.TRUE);
        return getProposalOperations().getProposalKeys(pm, queryProposals, 0);
    }

    /**
     * Remove nominative information from the Proposal record
     *
     * @param pointOfView Identify the anonymization point of view
     * @param proposal Entity to purge
     * @return Cleaned up Proposal instance
     */
    public static JsonObject anonymizeProposal(QueryPointOfView pointOfView, JsonObject proposal) {

        // Remove owner information
        proposal.remove(Proposal.OWNER_KEY);

        // TODO: control the cancellerKey

        return proposal;
    }

    /**
     * Helper fetching a list of parameters for a query
     *
     * @param parameters bag of parameters proposed by a connector
     * @return Prefetch list of query parameters
     */
    protected static Map<String, Object> prepareQueryForSelection(JsonObject parameters) {
        Map<String, Object> queryFilters = new HashMap<String, Object>();

        if (!parameters.containsKey(BaseRestlet.ANY_STATE_PARAMETER_KEY)) {
            queryFilters.put(Proposal.STATE_COMMAND_LIST, Boolean.TRUE);
        }

        Date lastModificationDate = null;
        if (parameters.containsKey(Entity.MODIFICATION_DATE)) {
            try {
                lastModificationDate = DateUtils.isoToDate(parameters.getString(Entity.MODIFICATION_DATE));
                queryFilters.put(">" + Entity.MODIFICATION_DATE, lastModificationDate);
            }
            catch (ParseException e) { } // Date not set, too bad.
        }

        return queryFilters;
    }

    /**
     * Utility method create a proposal with the given parameters and triggering the associated workflow steps
     *
     * @param pm Persistence manager instance to use - let open at the end to allow possible object updates later
     * @param parameters Parameters produced by the Command line parser or transmitted via the REST API
     * @param owner Sale associate who's going to own the created proposal
     * @param saConsumerRecord Consumer record attached to the sale associate
     * @return Just created proposal
     *
     * @throws DataSourceException if the retrieval of the last created proposal or of the location information fail
     * @throws ClientException if there's an issue with the given parameters
     */
    public static Proposal createProposal(PersistenceManager pm, JsonObject parameters, SaleAssociate owner, Consumer saConsumerRecord) throws DataSourceException, ClientException {

        // Data validation & propagation
        if (!parameters.containsKey(Proposal.DEMAND_KEY)) {
            throw new InvalidIdentifierException("Missing " + Proposal.DEMAND_KEY + " attribute!");
        }
        Long demandKey = parameters.getLong(Proposal.DEMAND_KEY);
        Demand demand = getDemandOperations().getDemand(pm, demandKey, null);
        if (!parameters.containsKey(Proposal.CONSUMER_KEY)) {
            parameters.put(Proposal.CONSUMER_KEY, demand.getOwnerKey());
        }
        else if (!demand.getOwnerKey().equals(parameters.getLong(Proposal.CONSUMER_KEY))) {
            throw new ClientException("Proposed " + Proposal.CONSUMER_KEY + " does not match the key of the demand owner!");
        }

        parameters.put(Proposal.LOCATION_KEY, owner.getLocationKey());

        // Entity creation
        Proposal proposal = getProposalOperations().createProposal(pm, parameters, owner);

        // Related workflow step
        MaezelServlet.triggerValidationTask(proposal);

        return proposal;
    }

    /**
     * Utility method update a proposal with the given parameters and triggering the associated workflow steps
     *
     * @param pm Persistence manager instance to use - let open at the end to allow possible object updates later
     * @param proposalKey Resource identifier
     * @param parameters Parameters produced by the Command line parser or transmitted via the REST API
     * @param owner Sale associate who owns the proposal to be updated
     * @param saConsumerRecord Consumer record attached to the sale associate
     * @return Just updated proposal
     *
     * @throws DataSourceException if the retrieval of the last created proposal or of the location information fail
     * @throws InvalidIdentifierException if there's an issue with the Proposal identifier is invalid
     * @throws InvalidStateException if the Proposal is not update-able
     */
    public static Proposal updateProposal(PersistenceManager pm, Long proposalKey, JsonObject parameters, SaleAssociate owner, Consumer saConsumerRecord) throws DataSourceException, InvalidIdentifierException, InvalidStateException {

        Proposal proposal = getProposalOperations().getProposal(pm, proposalKey, owner.getKey(), owner.getStoreKey());
        State currentState = proposal.getState();

        // Workflow state change
        if (parameters.size() == 1 && parameters.containsKey(Command.STATE)) {
            String proposedState = parameters.getString(Command.STATE);

            // Close
            if (State.confirmed.equals(currentState) && State.closed.toString().equals(proposedState)) {
                // Get the associated demand
                Demand demand = getDemandOperations().getDemand(pm, proposal.getDemandKey(), null);
                if (!State.closed.equals(demand.getState())) {
                    Consumer demandOwner = getConsumerOperations().getConsumer(pm, demand.getOwnerKey());

                    // Get the corresponding rawCommant
                    RawCommand rawCommand = null;
                    if (Source.api.equals(demand.getSource())) {
                        rawCommand = new RawCommand(demandOwner.getPreferredConnection());
                    }
                    else {
                        rawCommand = getRawCommandOperations().getRawCommand(pm, demand.getRawCommandId());
                    }

                    // Inform Proposal owner about the closing
                    Locale locale = demandOwner.getLocale();
                    String demandRef = LabelExtractor.get("cp_tweet_demand_reference_part", new Object[] { demand.getKey() }, locale);
                    String proposalRef = LabelExtractor.get("cp_tweet_proposal_reference_part", new Object[] { proposal.getKey() }, locale);
                    try {
                        communicateToConsumer(
                                rawCommand,
                                demandOwner,
                                new String[] { LabelExtractor.get("cp_command_close_proposal_closed_demand_to_close", new Object[] { proposalRef, demandRef }, locale) }
                        );
                    }
                    catch (CommunicationException ex) {} // Not a critical error, should not block the rest of the process
                }

                // No need to bother CC-ed
            }
            // Cancel
            else if (!State.closed.equals(currentState) && State.cancelled.toString().equals(proposedState)) {
                proposal.setCancelerKey(saConsumerRecord.getKey());

                // Get the associated demand
                Demand demand = getDemandOperations().getDemand(pm, proposal.getDemandKey(), null);
                demand.removeProposalKey(proposalKey);

                if (State.confirmed.equals(currentState)) {
                    Consumer demandOwner = getConsumerOperations().getConsumer(pm, demand.getOwnerKey());

                    RawCommand rawCommand = null;
                    if (Source.api.equals(proposal.getSource())) {
                        rawCommand = new RawCommand(saConsumerRecord.getPreferredConnection());
                    }
                    else {
                        rawCommand = getRawCommandOperations().getRawCommand(pm, demand.getRawCommandId());
                    }

                    // Notify SaleAssociate about the confirmed Demand cancellation
                    Locale locale = demandOwner.getLocale();
                    String demandRef = LabelExtractor.get("cp_tweet_demand_reference_part", new Object[] { demand.getKey() }, locale);
                    String proposalRef = LabelExtractor.get("cp_tweet_proposal_reference_part", new Object[] { proposal.getKey() }, locale);
                    String tags = proposal.getCriteria().size() == 0 ? "" : LabelExtractor.get("cp_tweet_tags_part", new Object[] { proposal.getSerializedCriteria() }, locale);
                    try {
                        communicateToConsumer(
                                rawCommand,
                                demandOwner,
                                new String[] { LabelExtractor.get("cp_command_cancel_proposal_canceled_demand_to_be_published", new Object[] { proposalRef, demandRef, tags }, locale) }
                        );
                    }
                    catch (CommunicationException ex) {} // Not a critical error, should not block the rest of the process

                    // TODO: Place the associated demand in the published state again if not expired
                    demand.setState(State.published);
                    demand.updateModificationDate(); // To be sure it's picked-up by the next cron job 'processPublishedDemands'
                }
                getDemandOperations().updateDemand(pm, demand);
            }
            else {
                throw new InvalidStateException("Invalid state change attempt to: " + proposedState, currentState.toString(), proposedState);
            }

            proposal.setState(proposedState);
            proposal = getProposalOperations().updateProposal(pm, proposal);
        }
        // Normal attribute update
        else if (State.opened.equals(currentState) || State.published.equals(currentState) || State.invalid.equals(currentState)) {
            // Neutralise read-only parameters
            parameters.remove(Proposal.DEMAND_KEY);
            parameters.remove(Proposal.CONSUMER_KEY);

            // Integrate updates
            proposal.fromJson(parameters);

            // Prepare as a new Demand
            proposal.setState(State.opened); // Will force the re-validation of the entire demand

            // Persist updates
            proposal = getProposalOperations().updateProposal(pm, proposal);

            // Detach the proposal from the associated demand (just in case it's now invalid)
            Demand demand = getDemandOperations().getDemand(pm, proposal.getDemandKey(), null);
            demand.removeProposalKey(proposalKey);
            getDemandOperations().updateDemand(pm, demand);

            // Related workflow step
            MaezelServlet.triggerValidationTask(proposal);
        }
        else {
            throw new InvalidStateException("Entity not in modifiable state", currentState.toString(), null);
        }

        return proposal;
    }

    /**
     * Utility method updating the proposal state if the specified user owns the attached demand
     *
     * @param pm Persistence manager instance to use - let open at the end to allow possible object updates later
     * @param proposalKey Resource identifier
     * @param parameters Parameters produced by the Command line parser or transmitted via the REST API
     * @param demandOwner Owner of the demand associated to the identified proposal
     * @return Just updated proposal
     *
     * @throws DataSourceException if the retrieval of the last created proposal or of the location information fail
     * @throws InvalidIdentifierException if there's an issue with the Proposal identifier is invalid
     * @throws InvalidStateException if the Proposal is not update-able
     * @throws ReservedOperationException if the Consumer does not own the Demand associated to the identified Proposal
     * @throws CommunicationException If the result of the operation cannot be communicated to the SaleAssociate owning the concerned Proposal
     */
    public static Proposal updateProposal(PersistenceManager pm, Long proposalKey, JsonObject parameters, Consumer demandOwner) throws DataSourceException, InvalidIdentifierException, InvalidStateException, ReservedOperationException, CommunicationException {

        // Verify the correct parameter sequence
        if (parameters.size() != 2 ||
            !parameters.containsKey(Command.POINT_OF_VIEW) ||
            !parameters.containsKey(Command.STATE) ||
            !State.confirmed.toString().equals(parameters.getString(Command.STATE)) && !State.declined.toString().equals(parameters.getString(Command.STATE))
        ) {
            throw new InvalidStateException("Invalid parameter sequence for the Proposal state update!");
        }
        boolean confirmProposal = State.confirmed.toString().equals(parameters.getString(Command.STATE));
        State newState = confirmProposal ? State.confirmed : State.declined;

        // Get the proposal and verify its state
        Proposal proposal = getProposalOperations().getProposal(pm, proposalKey, null, null);
        State currentState = proposal.getState();
        if (!State.published.equals(currentState)) {
            throw new InvalidStateException("Invalid Proposal state for a " + (confirmProposal ? "confirmation" : "declination"), currentState.toString(), newState.toString());
        }

        // Verify the consumer owns the associated demand and its state
        Demand demand = getDemandOperations().getDemand(pm, proposal.getDemandKey(), demandOwner.getKey());
        if (!State.published.equals(demand.getState())) {
            throw new InvalidStateException("Invalid associated Demand state for a " + (confirmProposal ? "confirmation" : "declination"), demand.getState().toString(), newState.toString());
        }

        // Persist the proposal state change
        proposal.setState(newState);
        if (!confirmProposal) {
            proposal.setCancelerKey(demandOwner.getKey());
        }
        proposal = getProposalOperations().updateProposal(pm, proposal);

        if (confirmProposal) {
            // Cancel the other proposals associated to the demand
            List<Long> proposalKeys = demand.getProposalKeys();
            if (1 < proposalKeys.size()) {
                // Schedule the other proposal cancellation
                MaezelServlet.triggerProposalCancellationTask(proposalKeys, demandOwner.getKey(), proposalKey);

                // Clean-up the list of associated proposals and sale associates
                proposalKeys.clear();
                proposalKeys.add(proposalKey);
                demand.resetSaleAssociateKeys();
                demand.addSaleAssociateKey(proposal.getOwnerKey());
            }

            // Persist the demand state change
            demand.setState(State.confirmed);
            getDemandOperations().updateDemand(pm, demand);

            Long robotKey = RobotResponder.getRobotSaleAssociateKey(pm);
            if (proposal.getOwnerKey().equals(robotKey)) {
                // Schedule the automatic Proposal closing by the Robot
                prepareProposalClosingByTheRobot(pm, proposalKey);
            }
            else {
                // Inform the sale associate of the successful confirmation
                SaleAssociate saleAssociate = getSaleAssociateOperations().getSaleAssociate(pm, proposal.getOwnerKey());
                Consumer saConsumerRecord = getConsumerOperations().getConsumer(pm, saleAssociate.getConsumerKey());
                RawCommand rawCommand = null;
                if (Source.api.equals(proposal.getSource())) {
                    rawCommand = new RawCommand(saConsumerRecord.getPreferredConnection());
                }
                else {
                    rawCommand = getRawCommandOperations().getRawCommand(pm, proposal.getRawCommandId());
                }

                Locale locale = saConsumerRecord.getLocale();
                String demandRef = LabelExtractor.get("cp_tweet_demand_reference_part", new Object[] { demand.getKey() }, locale);
                String proposalRef = LabelExtractor.get("cp_tweet_proposal_reference_part", new Object[] { proposal.getKey() }, locale);
                String tags = LabelExtractor.get("cp_tweet_tags_part", new Object[] { proposal.getSerializedCriteria() }, locale);
                communicateToConsumer(
                        rawCommand,
                        saConsumerRecord,
                        new String[] { LabelExtractor.get("cp_command_confirm_inform_about_confirmation", new Object[] { proposalRef, tags, demandRef }, locale) }
                );
            }

            // Notify CC-ed
            Store store = getStoreOperations().getStore(pm, proposal.getStoreKey());
            notifyConfirmationToCCed(pm, demand, proposalKey, store, demandOwner);
        }

        return proposal;
    }

    protected static void prepareProposalClosingByTheRobot(PersistenceManager pm, Long proposalKey) {
        // Simulated interaction
        RawCommand consequence = new RawCommand();
        consequence.setCommand(Prefix.action + CommandLineParser.PREFIX_SEPARATOR + Action.close + " " + Prefix.proposal + CommandLineParser.PREFIX_SEPARATOR + proposalKey);
        consequence.setSource(Source.robot);

        // Persist message
        consequence = getRawCommandOperations().createRawCommand(pm, consequence);

        // Create a task for that command
        MaezelServlet.triggerCommandProcessorTask(consequence.getKey());
    }

    /**
     * Utility method deleting the identified proposal
     *
     * @param pm Persistence manager instance to use - let open at the end to allow possible object updates later
     * @param proposalKey Resource identifier
     * @param owner Sale associate who owns the proposal to be deleted
     * @param saConsumerRecord Consumer record attached to the sale associate
     *
     * @throws DataSourceException if the retrieval of the last created proposal or of the location information fail
     * @throws InvalidIdentifierException if there's an issue with the proposal identifier is invalid
     * @throws InvalidStateException if the proposal is not already cancelled
     */
    public static void deleteProposal(PersistenceManager pm, Long proposalKey, SaleAssociate owner, Consumer saConsumerRecord) throws DataSourceException, InvalidIdentifierException, InvalidStateException {

        Proposal proposal = getProposalOperations().getProposal(pm, proposalKey, owner.getKey(), null);

        State currentState = proposal.getState();
        if (!State.cancelled.equals(currentState)) {
            throw new InvalidStateException("Invalid state change attempt to: " + State.markedForDeletion.toString(), currentState.toString(), State.markedForDeletion.toString());
        }

        proposal.setState(State.markedForDeletion);
        getProposalOperations().updateProposal(pm, proposal);
    }
}