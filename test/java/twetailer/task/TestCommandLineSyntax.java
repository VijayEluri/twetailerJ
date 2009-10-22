package twetailer.task;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import twetailer.ClientException;
import twetailer.connector.BaseConnector;
import twetailer.dao.BaseOperations;
import twetailer.dto.Command;
import twetailer.dto.Demand;
import twetailer.dto.Location;
import twetailer.dto.Proposal;
import twetailer.dto.Retailer;
import twetailer.validator.LocaleValidator;
import twetailer.validator.CommandSettings.Action;
import twetailer.validator.CommandSettings.Prefix;
import twetailer.validator.CommandSettings.State;
import domderrien.jsontools.GenericJsonArray;
import domderrien.jsontools.GenericJsonObject;
import domderrien.jsontools.JsonArray;
import domderrien.jsontools.JsonObject;

public class TestCommandLineSyntax {

    @Before
    public void setUp() throws Exception {
        // Simplified list of prefixes
        CommandProcessor.localizedPrefixes.clear();
        JsonObject prefixes = new GenericJsonObject();
        for (Prefix prefix: Prefix.values()) {
            JsonArray equivalents = new GenericJsonArray();
            equivalents.add(prefix.toString());
            if (Prefix.action.equals(prefix)) {
                equivalents.add("!");
            }
            else if (Prefix.help.equals(prefix)) {
                equivalents.add("?");
            }
            prefixes.put(prefix.toString(), equivalents);
        }
        CommandProcessor.localizedPrefixes.put(Locale.ENGLISH, prefixes);

        // Simplified list of actions
        CommandProcessor.localizedActions.clear();
        JsonObject actions = new GenericJsonObject();
        for (Action action: Action.values()) {
            JsonArray equivalents = new GenericJsonArray();
            equivalents.add(action.toString());
            actions.put(action.toString(), equivalents);
        }
        CommandProcessor.localizedActions.put(Locale.ENGLISH, actions);

        // Simplified list of states
        CommandProcessor.localizedStates.clear();
        JsonObject states = new GenericJsonObject();
        for (State state: State.values()) {
            states.put(state.toString(), state.toString());
        }
        CommandProcessor.localizedStates.put(Locale.ENGLISH, states);

        // Invoke the defined logic to build the list of RegEx patterns for the simplified list of prefixes
        CommandProcessor.localizedPatterns.clear();
        CommandProcessor.loadLocalizedSettings(Locale.ENGLISH);
    }

    @After
    public void tearDown() throws Exception {
        BaseConnector.resetLastCommunicationInSimulatedMode();

        CommandProcessor._baseOperations = new BaseOperations();
        CommandProcessor.consumerOperations = CommandProcessor._baseOperations.getConsumerOperations();
        CommandProcessor.demandOperations = CommandProcessor._baseOperations.getDemandOperations();
        CommandProcessor.locationOperations = CommandProcessor._baseOperations.getLocationOperations();
        CommandProcessor.proposalOperations = CommandProcessor._baseOperations.getProposalOperations();
        CommandProcessor.rawCommandOperations = CommandProcessor._baseOperations.getRawCommandOperations();
        CommandProcessor.retailerOperations = CommandProcessor._baseOperations.getRetailerOperations();
        CommandProcessor.settingsOperations = CommandProcessor._baseOperations.getSettingsOperations();
        // CommandProcessor.storeOperations = CommandProcessor._baseOperations.getStoreOperations();

        CommandProcessor.localizedPrefixes = new HashMap<Locale, JsonObject>();
        CommandProcessor.localizedActions = new HashMap<Locale, JsonObject>();
        CommandProcessor.localizedStates = new HashMap<Locale, JsonObject>();
        CommandProcessor.localizedHelpKeywords = new HashMap<Locale, JsonObject>();
        CommandProcessor.localizedPatterns = new HashMap<Locale, Map<String, Pattern>>();
    }

    @Test
    public void testLoadLocalizedSettings() {
        CommandProcessor.localizedPrefixes.clear();
        CommandProcessor.localizedActions.clear();
        CommandProcessor.localizedStates.clear();
        CommandProcessor.localizedHelpKeywords.clear();
        CommandProcessor.localizedPatterns.clear();

        CommandProcessor.loadLocalizedSettings(Locale.ENGLISH);

        assertNotSame(0, CommandProcessor.localizedPrefixes.size());
        assertNotSame(0, CommandProcessor.localizedActions.size());
        assertNotSame(0, CommandProcessor.localizedStates.size());
        assertNotSame(0, CommandProcessor.localizedHelpKeywords.size());
        assertNotSame(0, CommandProcessor.localizedPatterns.size());
    }

    @Test(expected=java.lang.NullPointerException.class)
    public void testParseNull() throws ClientException, ParseException {
        // Cannot pass a null reference
        CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), null, Locale.ENGLISH);
    }

    @Test(expected=twetailer.ClientException.class)
    public void testParseEmpty() throws ClientException, ParseException {
        // At least the twitter identifier of the sender is required
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "", Locale.ENGLISH);
        assertEquals(0, data.size());
    }

    @Test(expected=twetailer.ClientException.class)
    public void testParseWithOnlySeparators() throws ClientException, ParseException {
        // At least the twitter identifier of the sender is required
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), " \t \r\n ", Locale.ENGLISH);
        assertEquals(0, data.size());
    }

    @Test
    public void testParseReferenceI() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "reference:21", Locale.ENGLISH);
        assertEquals(21, data.getLong(Demand.REFERENCE));
    }

    @Test
    public void testParseReferenceII() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "reference: 21", Locale.ENGLISH);
        assertEquals(21, data.getLong(Demand.REFERENCE));
    }

    @Test
    public void testParseReferenceShort() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:21", Locale.ENGLISH);
        assertEquals(21, data.getLong(Demand.REFERENCE));
    }

    @Test
    public void testParseOneWordTag() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:21 product", Locale.ENGLISH);
        assertEquals("product", data.getJsonArray(Demand.CRITERIA).getString(0));
    }

    @Test
    public void testParseOneWordTagPrefixed() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:21 tags:product", Locale.ENGLISH);
        assertEquals("product", data.getJsonArray(Demand.CRITERIA).getString(0));
    }

    @Test
    public void testParseMultipleWordsTag() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:21 brand product part", Locale.ENGLISH);
        assertEquals("brand", data.getJsonArray(Demand.CRITERIA).getString(0));
        assertEquals("product", data.getJsonArray(Demand.CRITERIA).getString(1));
        assertEquals("part", data.getJsonArray(Demand.CRITERIA).getString(2));
    }

    @Test
    public void testParseMultipleWordsTagPrefixed() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:21 tags:brand product part", Locale.ENGLISH);
        assertEquals("brand", data.getJsonArray(Demand.CRITERIA).getString(0));
        assertEquals("product", data.getJsonArray(Demand.CRITERIA).getString(1));
        assertEquals("part", data.getJsonArray(Demand.CRITERIA).getString(2));
    }

    @Test
    public void testParseExpirationI() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:21 expiration:2050-01-01", Locale.ENGLISH);
        assertEquals("2050-01-01T23:59:59", data.getString(Demand.EXPIRATION_DATE));
    }

    @Test
    public void testParseExpirationII() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:21 expiration: 2050-01-01", Locale.ENGLISH);
        assertEquals("2050-01-01T23:59:59", data.getString(Demand.EXPIRATION_DATE));
    }

    @Test
    public void testParseExpirationIII() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:21 expiration: 20500101", Locale.ENGLISH);
        assertEquals("2050-01-01T23:59:59", data.getString(Demand.EXPIRATION_DATE));
    }

    @Test
    public void testParseExpirationIV() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:21 expiration:50-01-01", Locale.ENGLISH);
        assertEquals("2050-01-01T23:59:59", data.getString(Demand.EXPIRATION_DATE));
    }

    @Test
    public void testParseExpirationShort() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:21 exp:2050-01-01", Locale.ENGLISH);
        assertEquals("2050-01-01T23:59:59", data.getString(Demand.EXPIRATION_DATE));
    }

    @Test
    public void testParseRangeI() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:21 range:1mi", Locale.ENGLISH);
        assertEquals(1, data.getLong(Demand.RANGE));
        assertEquals(LocaleValidator.MILE_UNIT, data.getString(Demand.RANGE_UNIT));
    }

    @Test
    public void testParseRangeII() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:21 range: 1mi", Locale.ENGLISH);
        assertEquals(1, data.getLong(Demand.RANGE));
        assertEquals(LocaleValidator.MILE_UNIT, data.getString(Demand.RANGE_UNIT));
    }

    @Test
    public void testParseRangeIII() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:21 range: 1 mi", Locale.ENGLISH);
        assertEquals(1, data.getLong(Demand.RANGE));
        assertEquals(LocaleValidator.MILE_UNIT, data.getString(Demand.RANGE_UNIT));
    }

    @Test
    public void testParseRangeIV() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:21 range:1234567mi", Locale.ENGLISH);
        assertEquals(1234567, data.getLong(Demand.RANGE));
        assertEquals(LocaleValidator.MILE_UNIT, data.getString(Demand.RANGE_UNIT));
    }

    @Test
    public void testParseRangeV() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:21 range:1km", Locale.ENGLISH);
        assertEquals(1, data.getLong(Demand.RANGE));
        assertEquals(LocaleValidator.KILOMETER_UNIT, data.getString(Demand.RANGE_UNIT));
    }

    @Test
    public void testParseRangeVI() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:21 range:100 km", Locale.ENGLISH);
        assertEquals(100, data.getLong(Demand.RANGE));
        assertEquals(LocaleValidator.KILOMETER_UNIT, data.getString(Demand.RANGE_UNIT));
    }

    @Test
    public void testParseRangeVII() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:21 range:1mile", Locale.ENGLISH);
        assertEquals(1, data.getLong(Demand.RANGE));
        assertEquals("mile", data.getString(Demand.RANGE_UNIT));
    }

    @Test
    public void testParseRangeVIII() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:21 range:10miles", Locale.ENGLISH);
        assertEquals(10, data.getLong(Demand.RANGE));
        assertEquals(LocaleValidator.ALTERNATE_MILE_UNIT, data.getString(Demand.RANGE_UNIT));
    }

    @Test
    public void testParseRangeShortI() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:21 ran:1mi", Locale.ENGLISH);
        assertEquals(1, data.getLong(Demand.RANGE));
        assertEquals(LocaleValidator.MILE_UNIT, data.getString(Demand.RANGE_UNIT));
    }

    @Test
    public void testParseRangeShortII() throws ClientException, ParseException {
        // Add an equivalent to "range" and rebuild the RegEx patterns
        CommandProcessor.localizedPrefixes.get(Locale.ENGLISH).getJsonArray(Prefix.range.toString()).add("rng");
        CommandProcessor.localizedPatterns.clear();
        CommandProcessor.loadLocalizedSettings(Locale.ENGLISH);

        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:21 rng:1mi", Locale.ENGLISH);
        assertEquals(1, data.getLong(Demand.RANGE));
        assertEquals(LocaleValidator.MILE_UNIT, data.getString(Demand.RANGE_UNIT));
    }

    @Test
    public void testParseLocaleI() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:21 locale:h3c2n6 ca", Locale.ENGLISH);
        assertEquals("H3C2N6", data.getString(Location.POSTAL_CODE));
        assertEquals(RobotResponder.ROBOT_COUNTRY_CODE, data.getString(Location.COUNTRY_CODE));
    }

    @Test
    public void testParseLocaleII() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:21 locale: h3c 2n6 ca", Locale.ENGLISH);
        assertEquals("H3C2N6", data.getString(Location.POSTAL_CODE));
        assertEquals(RobotResponder.ROBOT_COUNTRY_CODE, data.getString(Location.COUNTRY_CODE));
    }

    @Test
    public void testParseLocaleIII() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:21 locale:h3c2n6-ca", Locale.ENGLISH);
        assertEquals("H3C2N6", data.getString(Location.POSTAL_CODE));
        assertEquals(RobotResponder.ROBOT_COUNTRY_CODE, data.getString(Location.COUNTRY_CODE));
    }

    @Test
    public void testParseLocaleIV() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:21 locale:97323 us", Locale.ENGLISH);
        assertEquals("97323", data.getString(Location.POSTAL_CODE));
        assertEquals(LocaleValidator.DEFAULT_COUNTRY_CODE, data.getString(Location.COUNTRY_CODE));
    }

    @Test
    public void testParseLocaleV() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:21 locale:97323-12345 us", Locale.ENGLISH);
        assertEquals("97323-12345", data.getString(Location.POSTAL_CODE));
        assertEquals(LocaleValidator.DEFAULT_COUNTRY_CODE, data.getString(Location.COUNTRY_CODE));
    }

    @Test
    public void testParseLocaleVI() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:21 locale:97323-12345-us", Locale.ENGLISH);
        assertEquals("97323-12345", data.getString(Location.POSTAL_CODE));
        assertEquals(LocaleValidator.DEFAULT_COUNTRY_CODE, data.getString(Location.COUNTRY_CODE));
    }

    @Test
    public void testParseLocaleShortI() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:21 loc:97343-us", Locale.ENGLISH);
        assertEquals("97343", data.getString(Location.POSTAL_CODE));
        assertEquals(LocaleValidator.DEFAULT_COUNTRY_CODE, data.getString(Location.COUNTRY_CODE));
    }

    @Test
    public void testParseLocaleShortII() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:21 loc:97343us", Locale.ENGLISH);
        assertEquals("97343", data.getString(Location.POSTAL_CODE));
        assertEquals(LocaleValidator.DEFAULT_COUNTRY_CODE, data.getString(Location.COUNTRY_CODE));
    }

    @Test
    public void testParsePriceI() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:21 price:25.99", Locale.ENGLISH);
        assertEquals(25.99, data.getDouble(Proposal.PRICE), 0.0);
    }

    @Test
    public void testParsePriceII() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:21 price:  25.99", Locale.ENGLISH);
        assertEquals(25.99, data.getDouble(Proposal.PRICE), 0.0);
    }

    @Test
    public void testParsePriceIII() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:21 price: $ 25.99", Locale.ENGLISH);
        assertEquals(25.99, data.getDouble(Proposal.PRICE), 0.0);
    }

    @Test
    public void testParsePriceIV() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:21 price:  25.99€", Locale.ENGLISH);
        assertEquals(25.99, data.getDouble(Proposal.PRICE), 0.0);
    }

    @Test
    public void testParsePriceV() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:21 price: € 25,3243", Locale.FRENCH);
        assertEquals(25.3243, data.getDouble(Proposal.PRICE), 0.0);
    }

    @Test
    public void testParsePriceVI() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:21 price:  25$", Locale.ENGLISH);
        assertEquals(25, data.getDouble(Proposal.PRICE), 0.0);
    }

    @Test
    public void testParsePriceShort() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:21 pri: 25.99", Locale.ENGLISH);
        assertEquals(25.99, data.getDouble(Proposal.PRICE), 0.0);
    }

    @Test
    public void testParseProposalI() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:21 proposal:1234567890", Locale.ENGLISH);
        assertEquals(1234567890L, data.getLong(Proposal.PROPOSAL_KEY));
    }

    @Test
    public void testParseProposalII() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:21 proposal: \t 1234567890", Locale.ENGLISH);
        assertEquals(1234567890L, data.getLong(Proposal.PROPOSAL_KEY));
    }

    @Test
    public void testParseProposalShort() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:21 pro: 1234567890", Locale.ENGLISH);
        assertEquals(1234567890L, data.getLong(Proposal.PROPOSAL_KEY));
    }

    @Test
    public void testParseQuantityI() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:21 quantity:21", Locale.ENGLISH);
        assertEquals(21, data.getLong(Demand.QUANTITY));
    }

    @Test
    public void testParseQuantityII() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:21 quantity: 21", Locale.ENGLISH);
        assertEquals(21, data.getLong(Demand.QUANTITY));
    }

    @Test
    public void testParseQuantityIII() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:21 quantity: 21", Locale.ENGLISH);
        assertEquals(21, data.getLong(Demand.QUANTITY));
    }

    @Test
    public void testParseQuantityShortI() throws ClientException, ParseException {
        // Add an equivalent to "quantity" and rebuild the RegEx patterns
        CommandProcessor.localizedPrefixes.get(Locale.ENGLISH).getJsonArray(Prefix.quantity.toString()).add("qty");
        CommandProcessor.localizedPatterns.clear();
        CommandProcessor.loadLocalizedSettings(Locale.ENGLISH);

        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:21 qty:21", Locale.ENGLISH);
        assertEquals(21, data.getLong(Demand.QUANTITY));
    }

    @Test
    public void testParseQuantityShortII() throws ClientException, ParseException {
        // Add an equivalent to "quantity" and rebuild the RegEx patterns
        CommandProcessor.localizedPrefixes.get(Locale.ENGLISH).getJsonArray(Prefix.quantity.toString()).add("qty");
        CommandProcessor.localizedPatterns.clear();
        CommandProcessor.loadLocalizedSettings(Locale.ENGLISH);

        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:  21    qty:  \t 50   ", Locale.ENGLISH);
        assertEquals(21, data.getLong(Demand.REFERENCE));
        assertEquals(50, data.getLong(Demand.QUANTITY));
    }

    @Test
    public void testParseTotalI() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:21 total:25.99", Locale.ENGLISH);
        assertEquals(25.99, data.getDouble(Proposal.TOTAL), 0.0);
    }

    @Test
    public void testParseTotalII() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:21 toTAL:  25.99", Locale.ENGLISH);
        assertEquals(25.99, data.getDouble(Proposal.TOTAL), 0.0);
    }

    @Test
    public void testParseTotalIII() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:21 toTAL: € 25.99", Locale.ENGLISH);
        assertEquals(25.99, data.getDouble(Proposal.TOTAL), 0.0);
    }

    @Test
    public void testParseTotalIV() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:21 toTAL:  25.99£", Locale.ENGLISH);
        assertEquals(25.99, data.getDouble(Proposal.TOTAL), 0.0);
    }

    @Test
    public void testParseTotalShort() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:21 tot: 25.99", Locale.ENGLISH);
        assertEquals(25.99, data.getDouble(Proposal.TOTAL), 0.0);
    }

    @Test
    public void testParseMixedCase() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:21 RaNge: 25 kM", Locale.ENGLISH);
        assertEquals(25, data.getLong(Demand.RANGE));
        assertEquals(LocaleValidator.KILOMETER_UNIT, data.getString(Demand.RANGE_UNIT));
    }

    @Test
    public void testParseCompositeI() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:1234 exp:2050-12-31", Locale.ENGLISH);
        assertEquals(1234, data.getLong(Demand.REFERENCE));
        assertEquals("2050-12-31T23:59:59", data.getString(Demand.EXPIRATION_DATE));
    }

    @Test
    public void testParseCompositeII() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:1234 range: 10 mi exp:2050-12-31", Locale.ENGLISH);
        assertEquals(1234, data.getLong(Demand.REFERENCE));
        assertEquals("2050-12-31T23:59:59", data.getString(Demand.EXPIRATION_DATE));
        assertEquals(10, data.getLong(Demand.RANGE));
        assertEquals(LocaleValidator.MILE_UNIT, data.getString(Demand.RANGE_UNIT));
    }

    @Test
    public void testParseCompositeIII() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:1234 range: 10 mi exp:2050-12-31 locale: h0h 0h0 ca", Locale.ENGLISH);
        assertEquals(1234, data.getLong(Demand.REFERENCE));
        assertEquals("2050-12-31T23:59:59", data.getString(Demand.EXPIRATION_DATE));
        assertEquals(10, data.getLong(Demand.RANGE));
        assertEquals(LocaleValidator.MILE_UNIT, data.getString(Demand.RANGE_UNIT));
        assertEquals(RobotResponder.ROBOT_POSTAL_CODE, data.getString(Location.POSTAL_CODE));
        assertEquals(RobotResponder.ROBOT_COUNTRY_CODE, data.getString(Location.COUNTRY_CODE));
    }

    @Test
    public void testParseCompositeIV() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:1234 quantity:12 range: 10 mi exp:2050-12-31 locale: h0h 0h0 ca", Locale.ENGLISH);
        assertEquals(1234, data.getLong(Demand.REFERENCE));
        assertEquals("2050-12-31T23:59:59", data.getString(Demand.EXPIRATION_DATE));
        assertEquals(10, data.getLong(Demand.RANGE));
        assertEquals(LocaleValidator.MILE_UNIT, data.getString(Demand.RANGE_UNIT));
        assertEquals(RobotResponder.ROBOT_POSTAL_CODE, data.getString(Location.POSTAL_CODE));
        assertEquals(RobotResponder.ROBOT_COUNTRY_CODE, data.getString(Location.COUNTRY_CODE));
        assertEquals(12, data.getLong(Demand.QUANTITY));
    }

    @Test
    public void testParseCompositeV() throws ClientException, ParseException {
        String keywords = "Wii  console\tremote \t control";
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "qua:12 range: 10 mi exp:2050-12-31 locale: h0h 0h0 ca " + keywords, Locale.ENGLISH);
        assertEquals("2050-12-31T23:59:59", data.getString(Demand.EXPIRATION_DATE));
        assertEquals(10, data.getLong(Demand.RANGE));
        assertEquals(LocaleValidator.MILE_UNIT, data.getString(Demand.RANGE_UNIT));
        assertEquals(RobotResponder.ROBOT_POSTAL_CODE, data.getString(Location.POSTAL_CODE));
        assertEquals(RobotResponder.ROBOT_COUNTRY_CODE, data.getString(Location.COUNTRY_CODE));
        assertEquals(12, data.getLong(Demand.QUANTITY));
        String[] parts = keywords.split("\\s+");
        for (int i = 0; i < parts.length; i ++) {
            assertEquals(parts[i], data.getJsonArray(Demand.CRITERIA).getString(i));
        }
    }

    @Test
    public void testParseCompositeVI() throws ClientException, ParseException {
        String keywords = "Wii console remote control";
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "qua:12 range: 10 mi exp:2050-12-31 " + keywords + " locale: h0h 0h0 ca", Locale.ENGLISH);
        assertEquals("2050-12-31T23:59:59", data.getString(Demand.EXPIRATION_DATE));
        assertEquals(10, data.getLong(Demand.RANGE));
        assertEquals(LocaleValidator.MILE_UNIT, data.getString(Demand.RANGE_UNIT));
        assertEquals(RobotResponder.ROBOT_POSTAL_CODE, data.getString(Location.POSTAL_CODE));
        assertEquals(RobotResponder.ROBOT_COUNTRY_CODE, data.getString(Location.COUNTRY_CODE));
        assertEquals(12, data.getLong(Demand.QUANTITY));
        String[] parts = keywords.split("\\s+");
        for (int i = 0; i < parts.length; i ++) {
            assertEquals(parts[i], data.getJsonArray(Demand.CRITERIA).getString(i));
        }
    }

    @Test
    public void testParseCompositeVII() throws ClientException, ParseException {
        String keywords = "Wii console remote control";
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "qua:12 range: 10 mi " + keywords + " exp:2050-12-31 locale: h0h 0h0 ca", Locale.ENGLISH);
        assertEquals("2050-12-31T23:59:59", data.getString(Demand.EXPIRATION_DATE));
        assertEquals(10, data.getLong(Demand.RANGE));
        assertEquals(LocaleValidator.MILE_UNIT, data.getString(Demand.RANGE_UNIT));
        assertEquals(RobotResponder.ROBOT_POSTAL_CODE, data.getString(Location.POSTAL_CODE));
        assertEquals(RobotResponder.ROBOT_COUNTRY_CODE, data.getString(Location.COUNTRY_CODE));
        assertEquals(12, data.getLong(Demand.QUANTITY));
        String[] parts = keywords.split("\\s+");
        for (int i = 0; i < parts.length; i ++) {
            assertEquals(parts[i], data.getJsonArray(Demand.CRITERIA).getString(i));
        }
    }

    @Test
    public void testParseCompositeVIII() throws ClientException, ParseException {
        String keywords = "Wii console remote control";
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "" + keywords + " quant:12 range: 10 mi exp:2050-12-31 locale: h0h 0h0 ca", Locale.ENGLISH);
        assertEquals("2050-12-31T23:59:59", data.getString(Demand.EXPIRATION_DATE));
        assertEquals(10, data.getLong(Demand.RANGE));
        assertEquals(LocaleValidator.MILE_UNIT, data.getString(Demand.RANGE_UNIT));
        assertEquals(RobotResponder.ROBOT_POSTAL_CODE, data.getString(Location.POSTAL_CODE));
        assertEquals(RobotResponder.ROBOT_COUNTRY_CODE, data.getString(Location.COUNTRY_CODE));
        assertEquals(12, data.getLong(Demand.QUANTITY));
        String[] parts = keywords.split("\\s+");
        for (int i = 0; i < parts.length; i ++) {
            assertEquals(parts[i], data.getJsonArray(Demand.CRITERIA).getString(i));
        }
    }

    @Test
    public void testParseCompositeIX() throws ClientException, ParseException {
        String keywords = "Wii console remote control et des épices sèches de suiße";
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "quant:12 range: 10 mi " + keywords + " exp:2050-12-31 locale: h0h 0h0 ca", Locale.ENGLISH);
        assertEquals("2050-12-31T23:59:59", data.getString(Demand.EXPIRATION_DATE));
        assertEquals(10, data.getLong(Demand.RANGE));
        assertEquals(LocaleValidator.MILE_UNIT, data.getString(Demand.RANGE_UNIT));
        assertEquals(RobotResponder.ROBOT_POSTAL_CODE, data.getString(Location.POSTAL_CODE));
        assertEquals(RobotResponder.ROBOT_COUNTRY_CODE, data.getString(Location.COUNTRY_CODE));
        assertEquals(12, data.getLong(Demand.QUANTITY));
        String[] parts = keywords.split("\\s+");
        for (int i = 0; i < parts.length; i ++) {
            assertEquals(parts[i], data.getJsonArray(Demand.CRITERIA).getString(i));
        }
    }

    @Test
    public void testParseActionI() throws ClientException, ParseException {
        String keywords = "Wii console remote control";
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "action:demand ref:1234 " + keywords, Locale.ENGLISH);
        assertEquals("demand", data.getString(Command.ACTION));
        assertEquals(1234, data.getLong(Demand.REFERENCE));
        String[] parts = keywords.split("\\s+");
        for (int i = 0; i < parts.length; i ++) {
            assertEquals(parts[i], data.getJsonArray(Demand.CRITERIA).getString(i));
        }
    }

    @Test
    public void testParseActionII() throws ClientException, ParseException {
        String keywords = "Wii console remote control";
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "!demand ref:1234 " + keywords, Locale.ENGLISH);
        assertEquals("demand", data.getString(Command.ACTION));
        assertEquals(1234, data.getLong(Demand.REFERENCE));
        String[] parts = keywords.split("\\s+");
        for (int i = 0; i < parts.length; i ++) {
            assertEquals(parts[i], data.getJsonArray(Demand.CRITERIA).getString(i));
        }
    }

    @Test
    public void testParseIncompleteMessage() throws ClientException, ParseException {
        String keywords = "Wii console remote control";
        CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "action:demand " + keywords, Locale.ENGLISH);
        // Now, the function consuming the incomplete tweet does the checking
    }

    @Test
    public void testParseActionIII() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "action:list ref:1234", Locale.ENGLISH);
        assertEquals("list", data.getString(Command.ACTION));
        assertEquals(1234, data.getLong(Demand.REFERENCE));
    }

    @Test
    public void testParseHelpI() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "help:", Locale.ENGLISH);
        assertEquals(1, data.size());
        assertNotNull(data.getString(Command.NEED_HELP));
        assertEquals(0, data.getString(Command.NEED_HELP).length());
    }

    @Test
    public void testParseHelpII() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "help:action:", Locale.ENGLISH);
        assertEquals(1, data.size());
        assertNotNull(data.getString(Command.NEED_HELP));
        assertEquals("action:", data.getString(Command.NEED_HELP));
    }

    @Test
    public void testParseHelpShortI() throws ClientException, ParseException {
        // Add an equivalent to "quantity" and rebuild the RegEx patterns
        CommandProcessor.localizedPrefixes.get(Locale.ENGLISH).getJsonArray(Prefix.help.toString()).add("?");
        CommandProcessor.localizedPatterns.clear();
        CommandProcessor.loadLocalizedSettings(Locale.ENGLISH);

        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "?", Locale.ENGLISH);
        assertEquals(1, data.size());
        assertTrue(data.containsKey(Command.NEED_HELP));
        assertNotNull(data.getString(Command.NEED_HELP));
    }
    @Test
    public void testParseHelpShortII() throws ClientException, ParseException {
        // Add an equivalent to "quantity" and rebuild the RegEx patterns
        CommandProcessor.localizedPrefixes.get(Locale.ENGLISH).getJsonArray(Prefix.help.toString()).add("?");
        CommandProcessor.localizedPatterns.clear();
        CommandProcessor.loadLocalizedSettings(Locale.ENGLISH);

        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), " action ? ", Locale.ENGLISH);
        assertEquals(1, data.size());
        assertTrue(data.containsKey(Command.NEED_HELP));
        assertNotNull(data.getString(Command.NEED_HELP));
        assertEquals("action", data.getString(Command.NEED_HELP));
    }

    @Test
    public void testParseHelpShortIII() throws ClientException, ParseException {
        // Add an equivalent to "quantity" and rebuild the RegEx patterns
        CommandProcessor.localizedPrefixes.get(Locale.ENGLISH).getJsonArray(Prefix.help.toString()).add("?");
        CommandProcessor.localizedPatterns.clear();
        CommandProcessor.loadLocalizedSettings(Locale.ENGLISH);

        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), " action: ? exp:", Locale.ENGLISH);
        assertEquals(1, data.size());
        assertTrue(data.containsKey(Command.NEED_HELP));
        assertNotNull(data.getString(Command.NEED_HELP));
        assertEquals("action:  exp:", data.getString(Command.NEED_HELP));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testGenerateFullTweetI() {
        List<String> criteria = new ArrayList<String>();
        criteria.add("first");
        criteria.add("second");

        Demand demand = new Demand();
        demand.setKey(1L);
        demand.setCriteria(criteria);
        demand.setExpirationDate(new Date(2025-1900, 0, 1, 0, 0, 0));
        demand.setQuantity(3L);
        demand.setRange(4.0D);
        demand.setRangeUnit(LocaleValidator.KILOMETER_UNIT);
        demand.setState(State.published);

        Location location = new Location();
        location.setPostalCode("zzz".toUpperCase());
        location.setCountryCode(Locale.CANADA.getCountry());

        Locale locale = Locale.ENGLISH;

        String response = CommandProcessor.generateTweet(demand, location, locale);

        assertNotNull(response);
        assertNotSame(0, response.length());
        JsonObject prefixes = CommandProcessor.localizedPrefixes.get(locale);
        JsonObject states = CommandProcessor.localizedStates.get(locale);
        assertTrue(response.contains(prefixes.getJsonArray(Prefix.reference.toString()).getString(0) + ":1"));
        assertTrue(response.contains(prefixes.getJsonArray(Prefix.tags.toString()).getString(0) + ":first second"));
        assertTrue(response.contains(prefixes.getJsonArray(Prefix.expiration.toString()).getString(0) + ":2025-01-01"));
        assertTrue(response.contains(prefixes.getJsonArray(Prefix.quantity.toString()).getString(0) + ":3"));
        assertTrue(response.contains(prefixes.getJsonArray(Prefix.range.toString()).getString(0) + ":4.0" + LocaleValidator.KILOMETER_UNIT));
        assertTrue(response.contains(prefixes.getJsonArray(Prefix.state.toString()).getString(0) + ":" + states.getString(State.published.toString())));
        assertTrue(response.contains(prefixes.getJsonArray(Prefix.locale.toString()).getString(0) + ":ZZZ " + Locale.CANADA.getCountry()));
    }

    @Test
    public void testGenerateFullTweetII() {
        List<String> criteria = new ArrayList<String>();
        criteria.add("first");
        criteria.add("second");

        Proposal proposal = new Proposal();
        proposal.setKey(1L);
        proposal.setCriteria(criteria);
        proposal.setDemandKey(12345L);
        proposal.setPrice(25.99D);
        proposal.setQuantity(3L);
        proposal.setStoreKey(67890L);
        proposal.setState(State.published);
        proposal.setTotal(35.33D);

        Locale locale = Locale.ENGLISH;

        String response = CommandProcessor.generateTweet(proposal, locale);

        assertNotNull(response);
        assertNotSame(0, response.length());
        JsonObject prefixes = CommandProcessor.localizedPrefixes.get(locale);
        JsonObject states = CommandProcessor.localizedStates.get(locale);
        assertTrue(response.contains(prefixes.getJsonArray(Prefix.proposal.toString()).getString(0) + ":1"));
        assertTrue(response.contains(prefixes.getJsonArray(Prefix.tags.toString()).getString(0) + ":first second"));
        assertTrue(response.contains(prefixes.getJsonArray(Prefix.reference.toString()).getString(0) + ":12345"));
        assertTrue(response.contains(prefixes.getJsonArray(Prefix.price.toString()).getString(0) + ":25.99"));
        assertTrue(response.contains(prefixes.getJsonArray(Prefix.quantity.toString()).getString(0) + ":3"));
        assertTrue(response.contains(prefixes.getJsonArray(Prefix.store.toString()).getString(0) + ":67890"));
        assertTrue(response.contains(prefixes.getJsonArray(Prefix.state.toString()).getString(0) + ":" + states.getString(State.published.toString())));
        assertTrue(response.contains(prefixes.getJsonArray(Prefix.total.toString()).getString(0) + ":35.33"));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testGeneratePartialTweetI() {
        Demand demand = new Demand();
        demand.setExpirationDate(new Date(2025-1900, 0, 1, 0, 0, 0));
        demand.setQuantity(3L);
        demand.setRange(4.0D);
        demand.setRangeUnit(LocaleValidator.KILOMETER_UNIT);
        demand.setState(State.published);

        Locale locale = Locale.ENGLISH;

        String response = CommandProcessor.generateTweet(demand, null, locale);

        assertNotNull(response);
        assertNotSame(0, response.length());
        JsonObject prefixes = CommandProcessor.localizedPrefixes.get(locale);
        JsonObject states = CommandProcessor.localizedStates.get(locale);
        assertFalse(response.contains(prefixes.getJsonArray(Prefix.reference.toString()).getString(0)));
        assertFalse(response.contains(prefixes.getJsonArray(Prefix.tags.toString()).getString(0)));
        assertTrue(response.contains(prefixes.getJsonArray(Prefix.expiration.toString()).getString(0) + ":2025-01-01"));
        assertTrue(response.contains(prefixes.getJsonArray(Prefix.quantity.toString()).getString(0) + ":3"));
        assertTrue(response.contains(prefixes.getJsonArray(Prefix.range.toString()).getString(0) + ":4.0" + LocaleValidator.KILOMETER_UNIT));
        assertTrue(response.contains(prefixes.getJsonArray(Prefix.state.toString()).getString(0) + ":" + states.getString(State.published.toString())));
        assertFalse(response.contains(prefixes.getJsonArray(Prefix.locale.toString()).getString(0)));
    }

    @Test
    public void testGeneratePartialTweetII() {
        Demand demand = new Demand();

        Location location = new Location();
        location.setCountryCode(Locale.CANADA.getCountry());

        Locale locale = Locale.ENGLISH;

        String response = CommandProcessor.generateTweet(demand, location, locale);

        assertNotNull(response);
        assertNotSame(0, response.length());
        JsonObject prefixes = CommandProcessor.localizedPrefixes.get(locale);
        assertFalse(response.contains(prefixes.getJsonArray(Prefix.locale.toString()).getString(0)));
    }

    @Test
    public void testGeneratePartialTweetIII() {
        Demand demand = new Demand();

        Location location = new Location();
        location.setPostalCode("zzz");

        Locale locale = Locale.ENGLISH;

        String response = CommandProcessor.generateTweet(demand, location, locale);

        assertNotNull(response);
        assertNotSame(0, response.length());
        JsonObject prefixes = CommandProcessor.localizedPrefixes.get(locale);
        assertFalse(response.contains(prefixes.getJsonArray(Prefix.locale.toString()).getString(0)));
    }

    @Test
    public void testGeneratePartialTweetIV() {

        Proposal proposal = new Proposal();
        proposal.setPrice(25.99D);
        proposal.setQuantity(3L);
        proposal.setState(State.published);
        proposal.setStoreKey(67890L);
        proposal.setTotal(35.33D);

        Locale locale = Locale.ENGLISH;

        String response = CommandProcessor.generateTweet(proposal, locale);

        assertNotNull(response);
        assertNotSame(0, response.length());
        JsonObject prefixes = CommandProcessor.localizedPrefixes.get(locale);
        JsonObject states = CommandProcessor.localizedStates.get(locale);
        assertFalse(response.contains(prefixes.getJsonArray(Prefix.proposal.toString()).getString(0)));
        assertFalse(response.contains(prefixes.getJsonArray(Prefix.tags.toString()).getString(0)));
        assertFalse(response.contains(prefixes.getJsonArray(Prefix.reference.toString()).getString(0)));
        assertTrue(response.contains(prefixes.getJsonArray(Prefix.price.toString()).getString(0) + ":25.99"));
        assertTrue(response.contains(prefixes.getJsonArray(Prefix.quantity.toString()).getString(0) + ":3"));
        assertTrue(response.contains(prefixes.getJsonArray(Prefix.store.toString()).getString(0) + ":67890"));
        assertTrue(response.contains(prefixes.getJsonArray(Prefix.state.toString()).getString(0) + ":" + states.getString(State.published.toString())));
        assertTrue(response.contains(prefixes.getJsonArray(Prefix.total.toString()).getString(0) + ":35.33"));
    }

    @Test
    public void testParseAddTag() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:21 +tags:product", Locale.ENGLISH);
        assertNull(data.getJsonArray(Demand.CRITERIA));
        assertEquals(1, data.getJsonArray("\\+" + Demand.CRITERIA).size());
        assertEquals("product", data.getJsonArray("\\+" + Demand.CRITERIA).getString(0));
    }

    @Test
    public void testParseRemoveTag() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:21 -tags:product", Locale.ENGLISH);
        assertNull(data.getJsonArray(Demand.CRITERIA));
        assertEquals(1, data.getJsonArray("\\-" + Demand.CRITERIA).size());
        assertEquals("product", data.getJsonArray("\\-" + Demand.CRITERIA).getString(0));
    }

    @Test
    public void testParseMixedTagsI() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:21 -tags:product +tags:service tags:excellence", Locale.ENGLISH);
        assertEquals(1, data.getJsonArray(Demand.CRITERIA).size());
        assertEquals(1, data.getJsonArray("\\+" + Demand.CRITERIA).size());
        assertEquals(1, data.getJsonArray("\\-" + Demand.CRITERIA).size());
        assertEquals("excellence", data.getJsonArray(Demand.CRITERIA).getString(0));
        assertEquals("service", data.getJsonArray("\\+" + Demand.CRITERIA).getString(0));
        assertEquals("product", data.getJsonArray("\\-" + Demand.CRITERIA).getString(0));
    }

    @Test
    public void testParseMixedTagsII() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(CommandProcessor.localizedPatterns.get(Locale.ENGLISH), "ref:21 tags:excellence -tags:product price:25.80£ +tags:service quantity:12", Locale.ENGLISH);
        assertEquals(1, data.getJsonArray(Demand.CRITERIA).size());
        assertEquals(1, data.getJsonArray("\\+" + Demand.CRITERIA).size());
        assertEquals(1, data.getJsonArray("\\-" + Demand.CRITERIA).size());
        assertEquals("excellence", data.getJsonArray(Demand.CRITERIA).getString(0));
        assertEquals("service", data.getJsonArray("\\+" + Demand.CRITERIA).getString(0));
        assertEquals("product", data.getJsonArray("\\-" + Demand.CRITERIA).getString(0));
    }

    @Test
    public void testParseMixedTagsIII() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(
                CommandProcessor.localizedPatterns.get(Locale.ENGLISH),
                "ref:21 tags:excellence excellency royal royalty +tags:product thing toy price:$25.80 -tags:service help quantity:12",
                Locale.ENGLISH
        );
        assertEquals(4, data.getJsonArray(Demand.CRITERIA).size());
        assertEquals(3, data.getJsonArray("\\+" + Demand.CRITERIA).size());
        assertEquals(2, data.getJsonArray("\\-" + Demand.CRITERIA).size());
        assertEquals("excellence", data.getJsonArray(Demand.CRITERIA).getString(0));
        assertEquals("excellency", data.getJsonArray(Demand.CRITERIA).getString(1));
        assertEquals("royal", data.getJsonArray(Demand.CRITERIA).getString(2));
        assertEquals("royalty", data.getJsonArray(Demand.CRITERIA).getString(3));
        assertEquals("product", data.getJsonArray("\\+" + Demand.CRITERIA).getString(0));
        assertEquals("thing", data.getJsonArray("\\+" + Demand.CRITERIA).getString(1));
        assertEquals("toy", data.getJsonArray("\\+" + Demand.CRITERIA).getString(2));
        assertEquals("service", data.getJsonArray("\\-" + Demand.CRITERIA).getString(0));
        assertEquals("help", data.getJsonArray("\\-" + Demand.CRITERIA).getString(1));
    }

    @Test
    public void testParseMixedTagsIV() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(
                CommandProcessor.localizedPatterns.get(Locale.ENGLISH),
                "ref:21 tags: total:45.3222 +tags: price:$25.80 -tags: quantity:12",
                Locale.ENGLISH
        );
        assertEquals(1, data.getJsonArray(Demand.CRITERIA).size());
        assertEquals(1, data.getJsonArray("\\+" + Demand.CRITERIA).size());
        assertEquals(1, data.getJsonArray("\\-" + Demand.CRITERIA).size());
    }

    @Test
    public void testParseMixedTagsV() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(
                CommandProcessor.localizedPatterns.get(Locale.ENGLISH),
                "action:demand ref:21 tags:one two three four +tags:four five six price:$25.80 -tags:three four six quantity:12",
                Locale.ENGLISH
        );
        Demand demand = new Demand(data);
        assertNotNull(demand.getCriteria());
        assertEquals(4, demand.getCriteria().size());
        assertTrue(demand.getCriteria().contains("one"));
        assertTrue(demand.getCriteria().contains("two"));
        assertTrue(demand.getCriteria().contains("four"));
        assertTrue(demand.getCriteria().contains("five"));
    }

    @Test
    public void testParseMixedTagsVI() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(
                CommandProcessor.localizedPatterns.get(Locale.ENGLISH),
                "action:propose proposal:21 tags:one two three four +tags:four five six price:$25.80 -tags:three four six quantity:12",
                Locale.ENGLISH
        );
        Proposal proposal = new Proposal(data);
        assertNotNull(proposal.getCriteria());
        assertEquals(4, proposal.getCriteria().size());
        assertTrue(proposal.getCriteria().contains("one"));
        assertTrue(proposal.getCriteria().contains("two"));
        assertTrue(proposal.getCriteria().contains("four"));
        assertTrue(proposal.getCriteria().contains("five"));
    }

    @Test
    public void testParseMixedTagsVII() throws ClientException, ParseException {
        JsonObject data = CommandProcessor.parseCommand(
                CommandProcessor.localizedPatterns.get(Locale.ENGLISH),
                "action:supply tags:one two three four +tags:four five six price:$25.80 -tags:three four six quantity:12",
                Locale.ENGLISH
        );
        Retailer retailer = new Retailer(data);
        assertNotNull(retailer.getCriteria());
        assertEquals(4, retailer.getCriteria().size());
        assertTrue(retailer.getCriteria().contains("one"));
        assertTrue(retailer.getCriteria().contains("two"));
        assertTrue(retailer.getCriteria().contains("four"));
        assertTrue(retailer.getCriteria().contains("five"));
    }
}