package twetailer.task.command;

import static com.google.appengine.api.labs.taskqueue.TaskOptions.Builder.url;
import static twetailer.connector.BaseConnector.communicateToSaleAssociate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;

import twetailer.ClientException;
import twetailer.DataSourceException;
import twetailer.dto.Command;
import twetailer.dto.Consumer;
import twetailer.dto.Proposal;
import twetailer.dto.RawCommand;
import twetailer.dto.SaleAssociate;
import twetailer.dto.Store;
import twetailer.task.CommandLineParser;
import twetailer.task.CommandProcessor;
import twetailer.validator.ApplicationSettings;
import twetailer.validator.CommandSettings.Action;
import twetailer.validator.CommandSettings.State;

import com.google.appengine.api.labs.taskqueue.Queue;
import com.google.appengine.api.labs.taskqueue.TaskOptions.Method;

import domderrien.i18n.LabelExtractor;
import domderrien.jsontools.JsonObject;

public class ProposeCommandProcessor {
    private static Logger log = Logger.getLogger(ProposeCommandProcessor.class.getName());

    public static void processProposeCommand(PersistenceManager pm, Consumer consumer, RawCommand rawCommand, JsonObject command) throws ClientException, DataSourceException {
        //
        // Used by a sale associate to:
        //
        // 1. create a new proposal
        // 2. update the identified proposal
        //
        Long proposalKey = 0L;
        SaleAssociate saleAssociate = CommandProcessor.retrieveSaleAssociate(pm, consumer, Action.propose, Proposal.class.getName());
        Store store = CommandProcessor.storeOperations.getStore(pm, saleAssociate.getStoreKey());
        List<String> messages = new ArrayList<String>();
        if (command.containsKey(Proposal.PROPOSAL_KEY)) {
            // Update the proposal attributes
            Proposal proposal = null;
            try {
                proposal = CommandProcessor.proposalOperations.getProposal(pm, command.getLong(Proposal.PROPOSAL_KEY), null, saleAssociate.getStoreKey());
            }
            catch(Exception ex) {
                messages.add(LabelExtractor.get("cp_command_propose_invalid_proposal_id", saleAssociate.getLocale()));
            }
            if (proposal != null) {
                State state = proposal.getState();
                if (state.equals(State.opened) || state.equals(State.published) || state.equals(State.invalid)) {
                    proposal.fromJson(command);
                    proposal.setState(State.opened); // Will force the re-validation of the entire proposal
                    proposal = CommandProcessor.proposalOperations.updateProposal(pm, proposal);
                    // Echo back the updated proposal
                    messages.add(CommandProcessor.generateTweet(proposal, store, false, saleAssociate.getLocale()));
                    // Get the proposalKey for the task scheduling
                    proposalKey = proposal.getKey();
                }
                else {
                    Locale locale = saleAssociate.getLocale();
                    String proposalRef = LabelExtractor.get("cp_tweet_proposal_reference_part", new Object[] { proposal.getKey() }, locale);
                    String stateLabel = CommandLineParser.localizedStates.get(locale).getString(state.toString());
                    stateLabel = LabelExtractor.get("cp_tweet_state_part", new Object[] { stateLabel }, locale);
                    messages.add(LabelExtractor.get("cp_command_propose_non_modifiable_state", new Object[] { proposalRef, stateLabel }, locale));
                }
            }
        }
        else {
            // Get the proposal attributes
            command.put(Command.SOURCE, rawCommand.getSource().toString());
            command.put(Command.RAW_COMMAND_ID, rawCommand.getKey());
            command.put(Command.LOCATION_KEY, store.getLocationKey());
            // Persist the new proposal
            Proposal newProposal = CommandProcessor.proposalOperations.createProposal(pm, command, saleAssociate);
            Locale locale = saleAssociate.getLocale();
            String proposalRef = LabelExtractor.get("cp_tweet_proposal_reference_part", new Object[] { newProposal.getKey() }, locale);
            messages.add(
                    LabelExtractor.get(
                            "cp_command_propose_acknowledge_creation",
                            new Object[] { proposalRef },
                            saleAssociate.getLocale()
                    )
            );
            messages.add(CommandProcessor.generateTweet(newProposal, store, false, saleAssociate.getLocale()));
            // Get the proposalKey for the task scheduling
            proposalKey = newProposal.getKey();
        }

        // Communicate message
        communicateToSaleAssociate(
                rawCommand,
                saleAssociate,
                messages.toArray(new String[0])
        );

        // Create a task for that proposal
        if (proposalKey != 0L) {
            Queue queue = CommandProcessor._baseOperations.getQueue();
            log.warning("Preparing the task: /maezel/validateOpenProposal?key=" + proposalKey.toString());
            queue.add(
                    url(ApplicationSettings.get().getServletApiPath() + "/maezel/validateOpenProposal").
                        param(Proposal.KEY, proposalKey.toString()).
                        method(Method.GET)
            );
        }
    }
}
