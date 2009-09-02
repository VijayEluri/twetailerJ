package twetailer.adapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.jdo.PersistenceManager;

import org.easymock.classextension.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import twitter4j.DirectMessage;
import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

import twetailer.ClientException;
import twetailer.DataSourceException;
import twetailer.dto.Command;
import twetailer.dto.Consumer;
import twetailer.dto.Demand;
import twetailer.dto.Location;
import twetailer.dto.Settings;
import twetailer.dao.BaseOperations;
import twetailer.dao.ConsumerOperations;
import twetailer.dao.DemandOperations;
import twetailer.dao.LocationOperations;
import twetailer.dao.MockPersistenceManager;
import twetailer.dao.SettingsOperations;
import twetailer.validator.CommandSettings;
import twetailer.validator.LocaleValidator;

import domderrien.i18n.LabelExtractor;
import domderrien.jsontools.GenericJsonArray;
import domderrien.jsontools.GenericJsonObject;
import domderrien.jsontools.JsonArray;
import domderrien.jsontools.JsonException;
import domderrien.jsontools.JsonObject;

public class TestTwitterAdapter {

    private class MockBaseOperations extends BaseOperations {
        @Override
        public PersistenceManager getPersistenceManager() {
            return new MockPersistenceManager();
        }
    };
    
    private class MockSettingsOperations extends SettingsOperations {
        @Override
        public Settings getSettings() {
            return new Settings();
        }
        @Override
        public Settings getSettings(PersistenceManager pm) {
            return new Settings();
        }
        @Override
        public Settings updateSettings(Settings settings) {
            return settings;
        }
        @Override
        public Settings updateSettings(PersistenceManager pm, Settings settings) {
            return settings;
        }
    };
    
    @Before
    public void setUp() throws Exception {
        // Simplified list of prefixes
        TwitterAdapter.localizedPrefixes.clear();
        JsonObject prefixes = new GenericJsonObject();
        for (CommandSettings.Prefix prefix: CommandSettings.Prefix.values()) {
            JsonArray equivalents = new GenericJsonArray();
            equivalents.add(prefix.toString());
            if (CommandSettings.Prefix.action.equals(prefix)) {
                equivalents.add("!");
            }
            else if (CommandSettings.Prefix.help.equals(prefix)) {
                equivalents.add("?");
            }
            prefixes.put(prefix.toString(), equivalents);
        }
        TwitterAdapter.localizedPrefixes.put(Locale.ENGLISH, prefixes);

        // Simplified list of actions
        TwitterAdapter.localizedActions.clear();
        JsonObject actions = new GenericJsonObject();
        for (CommandSettings.Action action: CommandSettings.Action.values()) {
            JsonArray equivalents = new GenericJsonArray();
            equivalents.add(action.toString());
            actions.put(action.toString(), equivalents);
        }
        TwitterAdapter.localizedActions.put(Locale.ENGLISH, actions);

        // Simplified list of states
        TwitterAdapter.localizedStates.clear();
        JsonObject states = new GenericJsonObject();
        for (CommandSettings.State state: CommandSettings.State.values()) {
            states.put(state.toString(), state.toString());
        }
        TwitterAdapter.localizedStates.put(Locale.ENGLISH, states);
        
        // Invoke the defined logic to build the list of RegEx patterns for the simplified list of prefixes
        TwitterAdapter.localizedPatterns.clear();
        TwitterAdapter.loadLocalizedSettings(Locale.ENGLISH);
    }

    @After
    public void tearDown() throws Exception {
    }
    
    @Test
    public void testConstructor() {
        new TwitterAdapter();
    }
    
    @Test
    public void testLoadLocalizedSettings() {
        TwitterAdapter.localizedPrefixes.clear();
        TwitterAdapter.localizedActions.clear();
        TwitterAdapter.localizedStates.clear();
        TwitterAdapter.localizedHelpKeywords.clear();
        TwitterAdapter.localizedPatterns.clear();

        TwitterAdapter.loadLocalizedSettings(Locale.ENGLISH);

        assertNotSame(0, TwitterAdapter.localizedPrefixes.size());
        assertNotSame(0, TwitterAdapter.localizedActions.size());
        assertNotSame(0, TwitterAdapter.localizedStates.size());
        assertNotSame(0, TwitterAdapter.localizedHelpKeywords.size());
        assertNotSame(0, TwitterAdapter.localizedPatterns.size());
    }
    
    @Test(expected=java.lang.NullPointerException.class)
    public void testParseNull() throws ClientException, ParseException {
        // Cannot pass a null reference
        TwitterAdapter.parseTweet(TwitterAdapter.localizedPatterns.get(Locale.ENGLISH), null);
    }

    @Test(expected=twetailer.ClientException.class)
    public void testParseEmpty() throws ClientException, ParseException {
        // At least the twitter identifier of the sender is required
        JsonObject data = TwitterAdapter.parseTweet(TwitterAdapter.localizedPatterns.get(Locale.ENGLISH), "");
        assertEquals(0, data.size());
    }

    @Test(expected=twetailer.ClientException.class)
    public void testParseWithOnlySeparators() throws ClientException, ParseException {
        // At least the twitter identifier of the sender is required
        JsonObject data = TwitterAdapter.parseTweet(TwitterAdapter.localizedPatterns.get(Locale.ENGLISH), " \t \r\n ");
        assertEquals(0, data.size());
    }

    @Test
    public void testParseReferenceI() throws ClientException, ParseException {
        JsonObject data = TwitterAdapter.parseTweet(TwitterAdapter.localizedPatterns.get(Locale.ENGLISH), "reference:21");
        assertEquals(21, data.getLong(Demand.REFERENCE));
    }

    @Test
    public void testParseReferenceII() throws ClientException, ParseException {
        JsonObject data = TwitterAdapter.parseTweet(TwitterAdapter.localizedPatterns.get(Locale.ENGLISH), "reference: 21");
        assertEquals(21, data.getLong(Demand.REFERENCE));
    }

    @Test
    public void testParseReferenceShort() throws ClientException, ParseException {
        JsonObject data = TwitterAdapter.parseTweet(TwitterAdapter.localizedPatterns.get(Locale.ENGLISH), "ref:21");
        assertEquals(21, data.getLong(Demand.REFERENCE));
    }

    @Test
    public void testParseOneWordTag() throws ClientException, ParseException {
        JsonObject data = TwitterAdapter.parseTweet(TwitterAdapter.localizedPatterns.get(Locale.ENGLISH), "ref:21 product");
        assertEquals("product", data.getJsonArray(Demand.CRITERIA).getString(0));
    }

    @Test
    public void testParseOneWordTagPrefixed() throws ClientException, ParseException {
        JsonObject data = TwitterAdapter.parseTweet(TwitterAdapter.localizedPatterns.get(Locale.ENGLISH), "ref:21 tags:product");
        assertEquals("product", data.getJsonArray(Demand.CRITERIA).getString(0));
    }

    @Test
    public void testParseMultipleWordsTag() throws ClientException, ParseException {
        JsonObject data = TwitterAdapter.parseTweet(TwitterAdapter.localizedPatterns.get(Locale.ENGLISH), "ref:21 brand product part");
        assertEquals("brand", data.getJsonArray(Demand.CRITERIA).getString(0));
        assertEquals("product", data.getJsonArray(Demand.CRITERIA).getString(1));
        assertEquals("part", data.getJsonArray(Demand.CRITERIA).getString(2));
    }

    @Test
    public void testParseMultipleWordsTagPrefixed() throws ClientException, ParseException {
        JsonObject data = TwitterAdapter.parseTweet(TwitterAdapter.localizedPatterns.get(Locale.ENGLISH), "ref:21 tags:brand product part");
        assertEquals("brand", data.getJsonArray(Demand.CRITERIA).getString(0));
        assertEquals("product", data.getJsonArray(Demand.CRITERIA).getString(1));
        assertEquals("part", data.getJsonArray(Demand.CRITERIA).getString(2));
    }

    @Test
    public void testParseExpirationI() throws ClientException, ParseException {
        JsonObject data = TwitterAdapter.parseTweet(TwitterAdapter.localizedPatterns.get(Locale.ENGLISH), "ref:21 expiration:2050-01-01");
        assertEquals("2050-01-01T23:59:59", data.getString(Demand.EXPIRATION_DATE));
    }

    @Test
    public void testParseExpirationII() throws ClientException, ParseException {
        JsonObject data = TwitterAdapter.parseTweet(TwitterAdapter.localizedPatterns.get(Locale.ENGLISH), "ref:21 expiration: 2050-01-01");
        assertEquals("2050-01-01T23:59:59", data.getString(Demand.EXPIRATION_DATE));
    }

    @Test
    public void testParseExpirationIII() throws ClientException, ParseException {
        JsonObject data = TwitterAdapter.parseTweet(TwitterAdapter.localizedPatterns.get(Locale.ENGLISH), "ref:21 expiration: 20500101");
        assertEquals("2050-01-01T23:59:59", data.getString(Demand.EXPIRATION_DATE));
    }

    @Test
    public void testParseExpirationIV() throws ClientException, ParseException {
        JsonObject data = TwitterAdapter.parseTweet(TwitterAdapter.localizedPatterns.get(Locale.ENGLISH), "ref:21 expiration:50-01-01");
        assertEquals("2050-01-01T23:59:59", data.getString(Demand.EXPIRATION_DATE));
    }

    @Test
    public void testParseExpirationShort() throws ClientException, ParseException {
        JsonObject data = TwitterAdapter.parseTweet(TwitterAdapter.localizedPatterns.get(Locale.ENGLISH), "ref:21 exp:2050-01-01");
        assertEquals("2050-01-01T23:59:59", data.getString(Demand.EXPIRATION_DATE));
    }

    @Test
    public void testParseRangeI() throws ClientException, ParseException {
        JsonObject data = TwitterAdapter.parseTweet(TwitterAdapter.localizedPatterns.get(Locale.ENGLISH), "ref:21 range:1mi");
        assertEquals(1, data.getLong(Demand.RANGE));
        assertEquals("mi", data.getString(Demand.RANGE_UNIT));
    }

    @Test
    public void testParseRangeII() throws ClientException, ParseException {
        JsonObject data = TwitterAdapter.parseTweet(TwitterAdapter.localizedPatterns.get(Locale.ENGLISH), "ref:21 range: 1mi");
        assertEquals(1, data.getLong(Demand.RANGE));
        assertEquals("mi", data.getString(Demand.RANGE_UNIT));
    }

    @Test
    public void testParseRangeIII() throws ClientException, ParseException {
        JsonObject data = TwitterAdapter.parseTweet(TwitterAdapter.localizedPatterns.get(Locale.ENGLISH), "ref:21 range: 1 mi");
        assertEquals(1, data.getLong(Demand.RANGE));
        assertEquals("mi", data.getString(Demand.RANGE_UNIT));
    }

    @Test
    public void testParseRangeIV() throws ClientException, ParseException {
        JsonObject data = TwitterAdapter.parseTweet(TwitterAdapter.localizedPatterns.get(Locale.ENGLISH), "ref:21 range:1234567mi");
        assertEquals(1234567, data.getLong(Demand.RANGE));
        assertEquals("mi", data.getString(Demand.RANGE_UNIT));
    }

    @Test
    public void testParseRangeV() throws ClientException, ParseException {
        JsonObject data = TwitterAdapter.parseTweet(TwitterAdapter.localizedPatterns.get(Locale.ENGLISH), "ref:21 range:1km");
        assertEquals(1, data.getLong(Demand.RANGE));
        assertEquals("km", data.getString(Demand.RANGE_UNIT));
    }

    @Test
    public void testParseRangeVI() throws ClientException, ParseException {
        JsonObject data = TwitterAdapter.parseTweet(TwitterAdapter.localizedPatterns.get(Locale.ENGLISH), "ref:21 range:100 km");
        assertEquals(100, data.getLong(Demand.RANGE));
        assertEquals("km", data.getString(Demand.RANGE_UNIT));
    }

    @Test
    public void testParseRangeShortI() throws ClientException, ParseException {
        JsonObject data = TwitterAdapter.parseTweet(TwitterAdapter.localizedPatterns.get(Locale.ENGLISH), "ref:21 ran:1mi");
        assertEquals(1, data.getLong(Demand.RANGE));
        assertEquals("mi", data.getString(Demand.RANGE_UNIT));
    }

    @Test
    public void testParseRangeShortII() throws ClientException, ParseException {
        // Add an equivalent to "range" and rebuild the RegEx patterns
        TwitterAdapter.localizedPrefixes.get(Locale.ENGLISH).getJsonArray(CommandSettings.Prefix.range.toString()).add("rng");
        TwitterAdapter.localizedPatterns.clear();
        TwitterAdapter.loadLocalizedSettings(Locale.ENGLISH);
        
        JsonObject data = TwitterAdapter.parseTweet(TwitterAdapter.localizedPatterns.get(Locale.ENGLISH), "ref:21 rng:1mi");
        assertEquals(1, data.getLong(Demand.RANGE));
        assertEquals("mi", data.getString(Demand.RANGE_UNIT));
    }

    @Test
    public void testParseLocaleI() throws ClientException, ParseException {
        JsonObject data = TwitterAdapter.parseTweet(TwitterAdapter.localizedPatterns.get(Locale.ENGLISH), "ref:21 locale:h3c2n6 ca");
        assertEquals("H3C2N6", data.getString(Location.POSTAL_CODE));
        assertEquals("CA", data.getString(Location.COUNTRY_CODE));
    }

    @Test
    public void testParseLocaleII() throws ClientException, ParseException {
        JsonObject data = TwitterAdapter.parseTweet(TwitterAdapter.localizedPatterns.get(Locale.ENGLISH), "ref:21 locale: h3c 2n6 ca");
        assertEquals("H3C 2N6", data.getString(Location.POSTAL_CODE));
        assertEquals("CA", data.getString(Location.COUNTRY_CODE));
    }

    @Test
    public void testParseLocaleIII() throws ClientException, ParseException {
        JsonObject data = TwitterAdapter.parseTweet(TwitterAdapter.localizedPatterns.get(Locale.ENGLISH), "ref:21 locale:h3c2n6-ca");
        assertEquals("H3C2N6", data.getString(Location.POSTAL_CODE));
        assertEquals("CA", data.getString(Location.COUNTRY_CODE));
    }

    @Test
    public void testParseLocaleIV() throws ClientException, ParseException {
        JsonObject data = TwitterAdapter.parseTweet(TwitterAdapter.localizedPatterns.get(Locale.ENGLISH), "ref:21 locale:97323 us");
        assertEquals("97323", data.getString(Location.POSTAL_CODE));
        assertEquals("US", data.getString(Location.COUNTRY_CODE));
    }

    @Test
    public void testParseLocaleV() throws ClientException, ParseException {
        JsonObject data = TwitterAdapter.parseTweet(TwitterAdapter.localizedPatterns.get(Locale.ENGLISH), "ref:21 locale:97323-12345 us");
        assertEquals("97323-12345", data.getString(Location.POSTAL_CODE));
        assertEquals("US", data.getString(Location.COUNTRY_CODE));
    }

    @Test
    public void testParseLocaleVI() throws ClientException, ParseException {
        JsonObject data = TwitterAdapter.parseTweet(TwitterAdapter.localizedPatterns.get(Locale.ENGLISH), "ref:21 locale:97323-12345-us");
        assertEquals("97323-12345", data.getString(Location.POSTAL_CODE));
        assertEquals("US", data.getString(Location.COUNTRY_CODE));
    }

    @Test
    public void testParseLocaleShort() throws ClientException, ParseException {
        JsonObject data = TwitterAdapter.parseTweet(TwitterAdapter.localizedPatterns.get(Locale.ENGLISH), "ref:21 loc:97343-us");
        assertEquals("97343", data.getString(Location.POSTAL_CODE));
        assertEquals("US", data.getString(Location.COUNTRY_CODE));
    }

    @Test
    public void testParseQuantityI() throws ClientException, ParseException {
        JsonObject data = TwitterAdapter.parseTweet(TwitterAdapter.localizedPatterns.get(Locale.ENGLISH), "ref:21 quantity:21");
        assertEquals(21, data.getLong(Demand.QUANTITY));
    }

    @Test
    public void testParseQuantityII() throws ClientException, ParseException {
        JsonObject data = TwitterAdapter.parseTweet(TwitterAdapter.localizedPatterns.get(Locale.ENGLISH), "ref:21 quantity: 21");
        assertEquals(21, data.getLong(Demand.QUANTITY));
    }

    @Test
    public void testParseQuantityIII() throws ClientException, ParseException {
        JsonObject data = TwitterAdapter.parseTweet(TwitterAdapter.localizedPatterns.get(Locale.ENGLISH), "ref:21 quantity: 21");
        assertEquals(21, data.getLong(Demand.QUANTITY));
    }

    @Test
    public void testParseQuantityShortI() throws ClientException, ParseException {
        // Add an equivalent to "quantity" and rebuild the RegEx patterns
        TwitterAdapter.localizedPrefixes.get(Locale.ENGLISH).getJsonArray(CommandSettings.Prefix.quantity.toString()).add("qty");
        TwitterAdapter.localizedPatterns.clear();
        TwitterAdapter.loadLocalizedSettings(Locale.ENGLISH);
        
        JsonObject data = TwitterAdapter.parseTweet(TwitterAdapter.localizedPatterns.get(Locale.ENGLISH), "ref:21 qty:21");
        assertEquals(21, data.getLong(Demand.QUANTITY));
    }
    
    @Test
    public void testParseQuantityShortII() throws ClientException, ParseException {
        // Add an equivalent to "quantity" and rebuild the RegEx patterns
        TwitterAdapter.localizedPrefixes.get(Locale.ENGLISH).getJsonArray(CommandSettings.Prefix.quantity.toString()).add("qty");
        TwitterAdapter.localizedPatterns.clear();
        TwitterAdapter.loadLocalizedSettings(Locale.ENGLISH);
        
        JsonObject data = TwitterAdapter.parseTweet(TwitterAdapter.localizedPatterns.get(Locale.ENGLISH), "ref:  21    qty:  \t 50   ");
        assertEquals(21, data.getLong(Demand.REFERENCE));
        assertEquals(50, data.getLong(Demand.QUANTITY));
    }

    @Test
    public void testParseMixedCase() throws ClientException, ParseException {
        JsonObject data = TwitterAdapter.parseTweet(TwitterAdapter.localizedPatterns.get(Locale.ENGLISH), "ref:21 RaNge: 25 kM");
        assertEquals(25, data.getLong(Demand.RANGE));
        assertEquals("km", data.getString(Demand.RANGE_UNIT));
    }

    @Test
    public void testParseCompositeI() throws ClientException, ParseException {
        JsonObject data = TwitterAdapter.parseTweet(TwitterAdapter.localizedPatterns.get(Locale.ENGLISH), "ref:1234 exp:2050-12-31");
        assertEquals(1234, data.getLong(Demand.REFERENCE));
        assertEquals("2050-12-31T23:59:59", data.getString(Demand.EXPIRATION_DATE));
    }

    @Test
    public void testParseCompositeII() throws ClientException, ParseException {
        JsonObject data = TwitterAdapter.parseTweet(TwitterAdapter.localizedPatterns.get(Locale.ENGLISH), "ref:1234 range: 10 mi exp:2050-12-31");
        assertEquals(1234, data.getLong(Demand.REFERENCE));
        assertEquals("2050-12-31T23:59:59", data.getString(Demand.EXPIRATION_DATE));
        assertEquals(10, data.getLong(Demand.RANGE));
        assertEquals("mi", data.getString(Demand.RANGE_UNIT));
    }

    @Test
    public void testParseCompositeIII() throws ClientException, ParseException {
        JsonObject data = TwitterAdapter.parseTweet(TwitterAdapter.localizedPatterns.get(Locale.ENGLISH), "ref:1234 range: 10 mi exp:2050-12-31 locale: h0h 0h0 ca");
        assertEquals(1234, data.getLong(Demand.REFERENCE));
        assertEquals("2050-12-31T23:59:59", data.getString(Demand.EXPIRATION_DATE));
        assertEquals(10, data.getLong(Demand.RANGE));
        assertEquals("mi", data.getString(Demand.RANGE_UNIT));
        assertEquals("H0H 0H0", data.getString(Location.POSTAL_CODE));
        assertEquals("CA", data.getString(Location.COUNTRY_CODE));
    }

    @Test
    public void testParseCompositeIV() throws ClientException, ParseException {
        JsonObject data = TwitterAdapter.parseTweet(TwitterAdapter.localizedPatterns.get(Locale.ENGLISH), "ref:1234 quantity:12 range: 10 mi exp:2050-12-31 locale: h0h 0h0 ca");
        assertEquals(1234, data.getLong(Demand.REFERENCE));
        assertEquals("2050-12-31T23:59:59", data.getString(Demand.EXPIRATION_DATE));
        assertEquals(10, data.getLong(Demand.RANGE));
        assertEquals("mi", data.getString(Demand.RANGE_UNIT));
        assertEquals("H0H 0H0", data.getString(Location.POSTAL_CODE));
        assertEquals("CA", data.getString(Location.COUNTRY_CODE));
        assertEquals(12, data.getLong(Demand.QUANTITY));
    }

    @Test
    public void testParseCompositeV() throws ClientException, ParseException {
        String keywords = "Wii  console\tremote \t control";
        JsonObject data = TwitterAdapter.parseTweet(TwitterAdapter.localizedPatterns.get(Locale.ENGLISH), "qua:12 range: 10 mi exp:2050-12-31 locale: h0h 0h0 ca " + keywords);
        assertEquals("2050-12-31T23:59:59", data.getString(Demand.EXPIRATION_DATE));
        assertEquals(10, data.getLong(Demand.RANGE));
        assertEquals("mi", data.getString(Demand.RANGE_UNIT));
        assertEquals("H0H 0H0", data.getString(Location.POSTAL_CODE));
        assertEquals("CA", data.getString(Location.COUNTRY_CODE));
        assertEquals(12, data.getLong(Demand.QUANTITY));
        String[] parts = keywords.split("\\s+");
        for (int i = 0; i < parts.length; i ++) {
            assertEquals(parts[i], data.getJsonArray(Demand.CRITERIA).getString(i));
        }
    }

    @Test
    public void testParseCompositeVI() throws ClientException, ParseException {
        String keywords = "Wii console remote control";
        JsonObject data = TwitterAdapter.parseTweet(TwitterAdapter.localizedPatterns.get(Locale.ENGLISH), "qua:12 range: 10 mi exp:2050-12-31 " + keywords + " locale: h0h 0h0 ca");
        assertEquals("2050-12-31T23:59:59", data.getString(Demand.EXPIRATION_DATE));
        assertEquals(10, data.getLong(Demand.RANGE));
        assertEquals("mi", data.getString(Demand.RANGE_UNIT));
        assertEquals("H0H 0H0", data.getString(Location.POSTAL_CODE));
        assertEquals("CA", data.getString(Location.COUNTRY_CODE));
        assertEquals(12, data.getLong(Demand.QUANTITY));
        String[] parts = keywords.split("\\s+");
        for (int i = 0; i < parts.length; i ++) {
            assertEquals(parts[i], data.getJsonArray(Demand.CRITERIA).getString(i));
        }
    }

    @Test
    public void testParseCompositeVII() throws ClientException, ParseException {
        String keywords = "Wii console remote control";
        JsonObject data = TwitterAdapter.parseTweet(TwitterAdapter.localizedPatterns.get(Locale.ENGLISH), "qua:12 range: 10 mi " + keywords + " exp:2050-12-31 locale: h0h 0h0 ca");
        assertEquals("2050-12-31T23:59:59", data.getString(Demand.EXPIRATION_DATE));
        assertEquals(10, data.getLong(Demand.RANGE));
        assertEquals("mi", data.getString(Demand.RANGE_UNIT));
        assertEquals("H0H 0H0", data.getString(Location.POSTAL_CODE));
        assertEquals("CA", data.getString(Location.COUNTRY_CODE));
        assertEquals(12, data.getLong(Demand.QUANTITY));
        String[] parts = keywords.split("\\s+");
        for (int i = 0; i < parts.length; i ++) {
            assertEquals(parts[i], data.getJsonArray(Demand.CRITERIA).getString(i));
        }
    }

    @Test
    public void testParseCompositeVIII() throws ClientException, ParseException {
        String keywords = "Wii console remote control";
        JsonObject data = TwitterAdapter.parseTweet(TwitterAdapter.localizedPatterns.get(Locale.ENGLISH), "" + keywords + " quant:12 range: 10 mi exp:2050-12-31 locale: h0h 0h0 ca");
        assertEquals("2050-12-31T23:59:59", data.getString(Demand.EXPIRATION_DATE));
        assertEquals(10, data.getLong(Demand.RANGE));
        assertEquals("mi", data.getString(Demand.RANGE_UNIT));
        assertEquals("H0H 0H0", data.getString(Location.POSTAL_CODE));
        assertEquals("CA", data.getString(Location.COUNTRY_CODE));
        assertEquals(12, data.getLong(Demand.QUANTITY));
        String[] parts = keywords.split("\\s+");
        for (int i = 0; i < parts.length; i ++) {
            assertEquals(parts[i], data.getJsonArray(Demand.CRITERIA).getString(i));
        }
    }

    @Test
    public void testParseActionI() throws ClientException, ParseException {
        String keywords = "Wii console remote control";
        JsonObject data = TwitterAdapter.parseTweet(TwitterAdapter.localizedPatterns.get(Locale.ENGLISH), "action:demand ref:1234 " + keywords);
        assertEquals("demand", data.getString(Demand.ACTION));
        assertEquals(1234, data.getLong(Demand.REFERENCE));
        String[] parts = keywords.split("\\s+");
        for (int i = 0; i < parts.length; i ++) {
            assertEquals(parts[i], data.getJsonArray(Demand.CRITERIA).getString(i));
        }
    }

    @Test
    public void testParseIncompleteMessage() throws ClientException, ParseException {
        String keywords = "Wii console remote control";
        TwitterAdapter.parseTweet(TwitterAdapter.localizedPatterns.get(Locale.ENGLISH), "action:demand " + keywords);
        // Now, the function consuming the incomplete tweet does the checking
    }

    @Test
    public void testParseActionIII() throws ClientException, ParseException {
        JsonObject data = TwitterAdapter.parseTweet(TwitterAdapter.localizedPatterns.get(Locale.ENGLISH), "action:list ref:1234");
        assertEquals("list", data.getString(Demand.ACTION));
        assertEquals(1234, data.getLong(Demand.REFERENCE));
    }

    @Test
    public void testParseHelpI() throws ClientException, ParseException {
        JsonObject data = TwitterAdapter.parseTweet(TwitterAdapter.localizedPatterns.get(Locale.ENGLISH), "help:");
        assertEquals(1, data.size());
        assertNotNull(data.getString(Command.NEED_HELP));
        assertEquals(0, data.getString(Command.NEED_HELP).length());
    }

    @Test
    public void testParseHelpII() throws ClientException, ParseException {
        JsonObject data = TwitterAdapter.parseTweet(TwitterAdapter.localizedPatterns.get(Locale.ENGLISH), "help:action:");
        assertEquals(1, data.size());
        assertNotNull(data.getString(Command.NEED_HELP));
        assertEquals("action:", data.getString(Command.NEED_HELP));
    }

    @Test
    public void testParseHelpShortI() throws ClientException, ParseException {
        // Add an equivalent to "quantity" and rebuild the RegEx patterns
        TwitterAdapter.localizedPrefixes.get(Locale.ENGLISH).getJsonArray(CommandSettings.Prefix.help.toString()).add("?");
        TwitterAdapter.localizedPatterns.clear();
        TwitterAdapter.loadLocalizedSettings(Locale.ENGLISH);

        JsonObject data = TwitterAdapter.parseTweet(TwitterAdapter.localizedPatterns.get(Locale.ENGLISH), "?");
        assertEquals(1, data.size());
        assertTrue(data.containsKey(Command.NEED_HELP));
        assertNotNull(data.getString(Command.NEED_HELP));
    }
    @Test
    public void testParseHelpShortII() throws ClientException, ParseException {
        // Add an equivalent to "quantity" and rebuild the RegEx patterns
        TwitterAdapter.localizedPrefixes.get(Locale.ENGLISH).getJsonArray(CommandSettings.Prefix.help.toString()).add("?");
        TwitterAdapter.localizedPatterns.clear();
        TwitterAdapter.loadLocalizedSettings(Locale.ENGLISH);

        JsonObject data = TwitterAdapter.parseTweet(TwitterAdapter.localizedPatterns.get(Locale.ENGLISH), " action ? ");
        assertEquals(1, data.size());
        assertTrue(data.containsKey(Command.NEED_HELP));
        assertNotNull(data.getString(Command.NEED_HELP));
        assertEquals("action", data.getString(Command.NEED_HELP));
    }

    @Test
    public void testParseHelpShortIII() throws ClientException, ParseException {
        // Add an equivalent to "quantity" and rebuild the RegEx patterns
        TwitterAdapter.localizedPrefixes.get(Locale.ENGLISH).getJsonArray(CommandSettings.Prefix.help.toString()).add("?");
        TwitterAdapter.localizedPatterns.clear();
        TwitterAdapter.loadLocalizedSettings(Locale.ENGLISH);

        JsonObject data = TwitterAdapter.parseTweet(TwitterAdapter.localizedPatterns.get(Locale.ENGLISH), " action: ? exp:");
        assertEquals(1, data.size());
        assertTrue(data.containsKey(Command.NEED_HELP));
        assertNotNull(data.getString(Command.NEED_HELP));
        assertEquals("action:  exp:", data.getString(Command.NEED_HELP));
    }
    
    @Test
    @SuppressWarnings("deprecation")
    public void testGenerateFullTweet() {
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
        demand.setState(CommandSettings.State.published);
        
        Location location = new Location();
        location.setPostalCode("zzz");
        location.setCountryCode(Locale.CANADA.getCountry());
        
        Locale locale = Locale.ENGLISH;
        JsonObject prefixes = TwitterAdapter.localizedPrefixes.get(locale);
        JsonObject actions = TwitterAdapter.localizedActions.get(locale);
        JsonObject states = TwitterAdapter.localizedStates.get(locale);
        
        String response = TwitterAdapter.generateTweet(demand, location, prefixes, actions, locale);
        
        assertNotNull(response);
        assertNotSame(0, response.length());
        assertTrue(response.contains(prefixes.getJsonArray(CommandSettings.Prefix.reference.toString()).getString(0) + ":1"));
        assertTrue(response.contains(prefixes.getJsonArray(CommandSettings.Prefix.tags.toString()).getString(0) + ":first second"));
        assertTrue(response.contains(prefixes.getJsonArray(CommandSettings.Prefix.expiration.toString()).getString(0) + ":2025-01-01"));
        assertTrue(response.contains(prefixes.getJsonArray(CommandSettings.Prefix.quantity.toString()).getString(0) + ":3"));
        assertTrue(response.contains(prefixes.getJsonArray(CommandSettings.Prefix.range.toString()).getString(0) + ":4.0" + LocaleValidator.KILOMETER_UNIT));
        assertTrue(response.contains(prefixes.getJsonArray(CommandSettings.Prefix.state.toString()).getString(0) + ":" + states.getString(CommandSettings.State.published.toString())));
        assertTrue(response.contains(prefixes.getJsonArray(CommandSettings.Prefix.locale.toString()).getString(0) + ":zzz " + Locale.CANADA.getCountry()));
    }
    
    @Test
    @SuppressWarnings("deprecation")
    public void testGeneratePartialTweetI() {
        Demand demand = new Demand();
        demand.setExpirationDate(new Date(2025-1900, 0, 1, 0, 0, 0));
        demand.setQuantity(3L);
        demand.setRange(4.0D);
        demand.setRangeUnit(LocaleValidator.KILOMETER_UNIT);
        demand.setState(CommandSettings.State.published);
        
        Locale locale = Locale.ENGLISH;
        JsonObject prefixes = TwitterAdapter.localizedPrefixes.get(locale);
        JsonObject actions = TwitterAdapter.localizedActions.get(locale);
        JsonObject states = TwitterAdapter.localizedStates.get(locale);
        
        String response = TwitterAdapter.generateTweet(demand, null, prefixes, actions, locale);

        assertNotNull(response);
        assertNotSame(0, response.length());
        assertFalse(response.contains(prefixes.getJsonArray(CommandSettings.Prefix.reference.toString()).getString(0)));
        assertFalse(response.contains(prefixes.getJsonArray(CommandSettings.Prefix.tags.toString()).getString(0)));
        assertTrue(response.contains(prefixes.getJsonArray(CommandSettings.Prefix.expiration.toString()).getString(0) + ":2025-01-01"));
        assertTrue(response.contains(prefixes.getJsonArray(CommandSettings.Prefix.quantity.toString()).getString(0) + ":3"));
        assertTrue(response.contains(prefixes.getJsonArray(CommandSettings.Prefix.range.toString()).getString(0) + ":4.0" + LocaleValidator.KILOMETER_UNIT));
        assertTrue(response.contains(prefixes.getJsonArray(CommandSettings.Prefix.state.toString()).getString(0) + ":" + states.getString(CommandSettings.State.published.toString())));
        assertFalse(response.contains(prefixes.getJsonArray(CommandSettings.Prefix.locale.toString()).getString(0)));
    }
    
    @Test
    public void testGeneratePartialTweetII() {
        Demand demand = new Demand();
        
        Location location = new Location();
        location.setCountryCode(Locale.CANADA.getCountry());
        
        Locale locale = Locale.ENGLISH;
        JsonObject prefixes = TwitterAdapter.localizedPrefixes.get(locale);
        JsonObject actions = TwitterAdapter.localizedActions.get(locale);
        
        String response = TwitterAdapter.generateTweet(demand, location, prefixes, actions, locale);

        assertNotNull(response);
        assertNotSame(0, response.length());
        assertFalse(response.contains(prefixes.getJsonArray(CommandSettings.Prefix.locale.toString()).getString(0)));
    }
    
    @Test
    public void testGeneratePartialTweetIII() {
        Demand demand = new Demand();
        
        Location location = new Location();
        location.setPostalCode("zzz");
        
        Locale locale = Locale.ENGLISH;
        JsonObject prefixes = TwitterAdapter.localizedPrefixes.get(locale);
        JsonObject actions = TwitterAdapter.localizedActions.get(locale);
        
        String response = TwitterAdapter.generateTweet(demand, location, prefixes, actions, locale);

        assertNotNull(response);
        assertNotSame(0, response.length());
        assertFalse(response.contains(prefixes.getJsonArray(CommandSettings.Prefix.locale.toString()).getString(0)));
    }

    @Test
    @SuppressWarnings("serial")
    public void testProcessDirectMessageWithNoMessageI() throws JsonException, TwitterException, DataSourceException, ParseException, ClientException {
        // Inject a fake Twitter account
        TwitterUtils.releaseTwetailerAccount(new Twitter() {
            public List<DirectMessage> getDirectMessages(Paging paging) {
                return null;
            }
        });
        TwitterAdapter._baseOperations = new MockBaseOperations();
        TwitterAdapter.settingsOperations = new MockSettingsOperations();
        
        TwitterAdapter.processDirectMessages();
        
        // Remove the fake Twitter account
        TwitterUtils.getTwetailerAccount();
    }

    @Test
    @SuppressWarnings("serial")
    public void testProcessDirectMessageWithNoMessageII() throws JsonException, TwitterException, DataSourceException, ParseException, ClientException {
        // Inject a fake Twitter account
        TwitterUtils.releaseTwetailerAccount(new Twitter() {
            public List<DirectMessage> getDirectMessages(Paging paging) {
                return new ArrayList<DirectMessage>();
            }
        });
        TwitterAdapter._baseOperations = new MockBaseOperations();
        TwitterAdapter.settingsOperations = new MockSettingsOperations();
        
        TwitterAdapter.processDirectMessages();
        
        // Remove the fake Twitter account
        TwitterUtils.getTwetailerAccount();
    }

    @SuppressWarnings("deprecation")
    private User createUser(int id, boolean isFollowing, String screenName) {
        User user = EasyMock.createMock(User.class);
        EasyMock.expect(user.getId()).andReturn(id).atLeastOnce();
        EasyMock.expect(user.isFollowing()).andReturn(isFollowing).once();
        EasyMock.expect(user.getScreenName()).andReturn(screenName).once();
        EasyMock.replay(user);
        return user;
    }
    
    private DirectMessage createDM(int id, int senderId, String screenName, User sender, String message) {
        DirectMessage dm = EasyMock.createMock(DirectMessage.class);
        EasyMock.expect(dm.getSenderScreenName()).andReturn(screenName).once();
        EasyMock.expect(dm.getSenderId()).andReturn(senderId).once();
        EasyMock.expect(dm.getSender()).andReturn(sender).once();
        EasyMock.expect(dm.getId()).andReturn(id).once();
        if (message != null) {
            EasyMock.expect(dm.getText()).andReturn(message).once();
        }
        EasyMock.replay(dm);
        return dm;
    }
    
    @Test
    @SuppressWarnings({ "serial" })
    public void testProcessDirectMessageFromNewSenderNotFollowingTwetailer() throws JsonException, TwitterException, DataSourceException, ParseException, ClientException {
        final int senderId = 1111;
        final int dmId = 2222;
        final long consumerKey = 3333L;
        final String senderScreenName = "Tom";
        
        // Sender mock
        User sender = createUser(senderId, false, senderScreenName); // <-- The sender does not follow @twetailer
        // DirectMessage mock
        final DirectMessage dm = createDM(dmId, senderId, String.valueOf(senderId), sender, null);
        // Twitter mock
        final Twitter twitterAccount = new Twitter() {
            @Override
            public List<DirectMessage> getDirectMessages(Paging paging) {
                List<DirectMessage> messages = new ArrayList<DirectMessage>();
                messages.add(dm);
                return messages;
            }
            @Override
            public Status updateStatus(String status) {
                assertTrue(status.startsWith("@" + senderScreenName));
                assertTrue(status.contains("Follow Twetailer"));
                return null;
            }
            @Override
            public User follow(String id) {
                assertEquals(id, String.valueOf(senderId));
                return null;
            }
        };
        // Inject the fake Twitter account
        TwitterUtils.releaseTwetailerAccount(twitterAccount);
        // ConsumerOperations mock
        final ConsumerOperations consumerOperations = new ConsumerOperations() {
            @Override
            public Consumer createConsumer(PersistenceManager pm, User twitterAccount) {
                int twitterId = twitterAccount.getId();
                assertEquals(senderId, twitterId);         // <-- Verify the correct creation submission
                Consumer consumer = new Consumer();
                consumer.setTwitterId(Long.valueOf(twitterId));
                consumer.setKey(consumerKey);
                return consumer;
            }
        };
        // TwitterAdapter mock 
        TwitterAdapter._baseOperations = new MockBaseOperations();
        TwitterAdapter.consumerOperations = consumerOperations;
        
        // Test itself
        Long newSinceId = TwitterAdapter.processDirectMessages(new MockPersistenceManager(), 1L);
        assertNotSame(Long.valueOf(dmId), newSinceId); // Because the nessage is not processed
        
        // Remove the fake Twitter account
        TwitterUtils.getTwetailerAccount();
    }
    
    static String referenceLabel = CommandSettings.getPrefixes(Locale.ENGLISH).getJsonArray(CommandSettings.Prefix.reference.toString()).getString(0);

    @Test
    @SuppressWarnings({ "serial" })
    public void testProcessDirectMessageWithOneCorrectMessage() throws JsonException, TwitterException, DataSourceException, ParseException, ClientException {
        final int senderId = 1111;
        final int dmId = 2222;
        final long consumerKey = 333;
        final long demandKey = 4444;
        final long locationKey = 55555;
        final String senderScreenName = "Tom";
        
        // Sender mock
        User sender = createUser(senderId, true, senderScreenName);
        // DirectMessage mock
        final DirectMessage dm = createDM(dmId, senderId, String.valueOf(senderId), sender, "action:demand tags:wii console quantity:1 loc:h0h0h0 ca exp:2050-01-01");
        // Twitter mock
        final Twitter twitterAccount = new Twitter() {
            @Override
            public List<DirectMessage> getDirectMessages(Paging paging) {
                List<DirectMessage> messages = new ArrayList<DirectMessage>();
                messages.add(dm);
                return messages;
            }
            @Override
            public DirectMessage sendDirectMessage(String id, String text) {
                assertEquals(senderScreenName, id);
                assertNotSame(0, text.length());
                assertTrue(text.contains(referenceLabel + ":" + String.valueOf(demandKey)));
                return dm;
            }
        };
        // Inject the fake Twitter account
        TwitterUtils.releaseTwetailerAccount(twitterAccount);
        // ConsumerOperations mock
        final ConsumerOperations consumerOperations = new ConsumerOperations() {
            @Override
            public Consumer createConsumer(PersistenceManager pm, User twitterAccount) {
                int twitterId = twitterAccount.getId();
                assertEquals(senderId, twitterId);         // <-- Verify the correct creation submission
                Consumer consumer = new Consumer();
                consumer.setTwitterId(Long.valueOf(twitterId));
                consumer.setKey(consumerKey);
                return consumer;
            }
        };
        // DemandOperations mock
        final DemandOperations demandOperations = new DemandOperations() {
            @Override
            public List<Demand> getDemands(PersistenceManager pm, String key, Object value, int limit) {
                assertEquals("consumerKey", key);                     // <-- Sender is already known
                assertEquals(Long.valueOf(consumerKey), (Long) value);
                return new ArrayList<Demand>();
            }
            @Override
            public Demand createDemand(PersistenceManager pm, JsonObject parameters, Long consumerKey) {
                assertEquals(2, parameters.getJsonArray(Demand.CRITERIA).size());
                Demand demand = new Demand();
                demand.setKey(demandKey);
                return demand;
            }
        };
        // LocationOperations mock
        final LocationOperations locationOperations = new LocationOperations() {
            @Override
            public Location getLocation(PersistenceManager pm, Long givenLocationKey) {
                assertEquals(locationKey, givenLocationKey.longValue());
                Location location = new Location();
                location.setKey(locationKey);
                return location;
            }
            @Override
            public Location createLocation(PersistenceManager pm, JsonObject parameters) {
                assertEquals("H0H0H0", parameters.getString(Location.POSTAL_CODE));
                Location location = new Location();
                location.setPostalCode("H0H0H0");
                location.setCountryCode("CA");
                location.setKey(locationKey);
                return location;
            }
        };
        // TwitterAdapter mock 
        TwitterAdapter._baseOperations = new MockBaseOperations();
        TwitterAdapter.consumerOperations = consumerOperations;
        TwitterAdapter.demandOperations = demandOperations;
        TwitterAdapter.locationOperations = locationOperations;
        
        // Test itself
        Long newSinceId = TwitterAdapter.processDirectMessages();
        assertEquals(Long.valueOf(dmId), newSinceId);
        
        // Remove the fake Twitter account
        TwitterUtils.getTwetailerAccount();
    }

    @Test
    @SuppressWarnings({ "serial" })
    public void testProcessDirectMessageWithUnsupportedAction() throws JsonException, TwitterException, DataSourceException, ParseException, ClientException {
        final int senderId = 1111;
        final int dmId = 2222;
        final long consumerKey = 333;
        final String senderScreenName = "Tom";

        // Sender mock
        User sender = createUser(senderId, true, senderScreenName);
        // DirectMessage mock
        final DirectMessage dm = createDM(dmId, senderId, String.valueOf(senderId), sender, "!grrrr ref:10 wii console quantity:1 loc:h0h0h0 ca exp:2050-01-01");
        // Twitter mock
        final Twitter twitterAccount = new Twitter() {
            @Override
            public List<DirectMessage> getDirectMessages(Paging paging) {
                List<DirectMessage> messages = new ArrayList<DirectMessage>();
                messages.add(dm);
                return messages;
            }
            @Override
            public DirectMessage sendDirectMessage(String id, String text) {
                assertEquals(senderScreenName, id);
                assertNotSame(0, text.length());
                assertTrue(text.contains(LabelExtractor.get("error_unsupported_action", Locale.ENGLISH)));
                return dm;
            }
        };
        // Inject the fake Twitter account
        TwitterUtils.releaseTwetailerAccount(twitterAccount);
        // ConsumerOperations mock
        final ConsumerOperations consumerOperations = new ConsumerOperations() {
            @Override
            public Consumer createConsumer(PersistenceManager pm, User twitterAccount) {
                int twitterId = twitterAccount.getId();
                assertEquals(senderId, twitterId);         // <-- Verify the correct creation submission
                Consumer consumer = new Consumer();
                consumer.setTwitterId(Long.valueOf(twitterId));
                consumer.setKey(consumerKey);
                return consumer;
            }
        };
        // TwitterAdapter mock 
        TwitterAdapter._baseOperations = new MockBaseOperations();
        TwitterAdapter.consumerOperations = consumerOperations;
        
        // Test itself
        Long newSinceId = TwitterAdapter.processDirectMessages(new MockPersistenceManager(), 1L);
        assertEquals(Long.valueOf(dmId), newSinceId);
        
        // Remove the fake Twitter account
        TwitterUtils.getTwetailerAccount();
    }
    
    @Test
    @SuppressWarnings("serial")
    public void testProcessHelpCommandWithPrefixI() throws DataSourceException, TwitterException {
        final int senderId = 1111;
        final int dmId = 2222;
        final long consumerKey = 333;
        final String senderScreenName = "Tom";

        // Sender mock
        User sender = createUser(senderId, true, senderScreenName);
        // DirectMessage mock
        final DirectMessage dm = createDM(dmId, senderId, String.valueOf(senderId), sender, "help: " + CommandSettings.Prefix.action.toString());
        // Twitter mock
        final Twitter twitterAccount = new Twitter() {
            @Override
            public List<DirectMessage> getDirectMessages(Paging paging) {
                List<DirectMessage> messages = new ArrayList<DirectMessage>();
                messages.add(dm);
                return messages;
            }
            @Override
            public DirectMessage sendDirectMessage(String id, String text) {
                assertEquals(senderScreenName, id);
                assertNotSame(0, text.length());
                assertTrue(text.contains(LabelExtractor.get(CommandSettings.HELP_DEFINITION_PREFIX_PREFIX + CommandSettings.Prefix.action.toString(), Locale.ENGLISH)));
                return dm;
            }
        };
        // Inject the fake Twitter account
        TwitterUtils.releaseTwetailerAccount(twitterAccount);
        // ConsumerOperations mock
        final ConsumerOperations consumerOperations = new ConsumerOperations() {
            @Override
            public Consumer createConsumer(PersistenceManager pm, User twitterAccount) {
                int twitterId = twitterAccount.getId();
                assertEquals(senderId, twitterId);         // <-- Verify the correct creation submission
                Consumer consumer = new Consumer();
                consumer.setTwitterId(Long.valueOf(twitterId));
                consumer.setKey(consumerKey);
                return consumer;
            }
        };
        // TwitterAdapter mock 
        TwitterAdapter._baseOperations = new MockBaseOperations();
        TwitterAdapter.consumerOperations = consumerOperations;

        // Test itself
        Long newSinceId = TwitterAdapter.processDirectMessages(new MockPersistenceManager(), 1L);
        assertEquals(Long.valueOf(dmId), newSinceId);
        
        // Remove the fake Twitter account
        TwitterUtils.getTwetailerAccount();
    }
    
    @Test
    @SuppressWarnings("serial")
    public void testProcessHelpCommandWithPrefixII() throws DataSourceException, TwitterException {
        final int senderId = 1111;
        final int dmId = 2222;
        final long consumerKey = 333;
        final String senderScreenName = "Tom";

        // Sender mock
        User sender = createUser(senderId, true, senderScreenName);
        // DirectMessage mock
        final DirectMessage dm = createDM(dmId, senderId, String.valueOf(senderId), sender, "!help " + CommandSettings.Prefix.action.toString());
        // Twitter mock
        final Twitter twitterAccount = new Twitter() {
            @Override
            public List<DirectMessage> getDirectMessages(Paging paging) {
                List<DirectMessage> messages = new ArrayList<DirectMessage>();
                messages.add(dm);
                return messages;
            }
            @Override
            public DirectMessage sendDirectMessage(String id, String text) {
                assertEquals(senderScreenName, id);
                assertNotSame(0, text.length());
                assertTrue(text.contains(LabelExtractor.get(CommandSettings.HELP_DEFINITION_PREFIX_PREFIX + CommandSettings.Prefix.action.toString(), Locale.ENGLISH)));
                return dm;
            }
        };
        // Inject the fake Twitter account
        TwitterUtils.releaseTwetailerAccount(twitterAccount);
        // ConsumerOperations mock
        final ConsumerOperations consumerOperations = new ConsumerOperations() {
            @Override
            public Consumer createConsumer(PersistenceManager pm, User twitterAccount) {
                int twitterId = twitterAccount.getId();
                assertEquals(senderId, twitterId);         // <-- Verify the correct creation submission
                Consumer consumer = new Consumer();
                consumer.setTwitterId(Long.valueOf(twitterId));
                consumer.setKey(consumerKey);
                return consumer;
            }
        };
        // TwitterAdapter mock 
        TwitterAdapter._baseOperations = new MockBaseOperations();
        TwitterAdapter.consumerOperations = consumerOperations;

        // Test itself
        Long newSinceId = TwitterAdapter.processDirectMessages(new MockPersistenceManager(), 1L);
        assertEquals(Long.valueOf(dmId), newSinceId);
        
        // Remove the fake Twitter account
        TwitterUtils.getTwetailerAccount();
    }
    
    @Test
    @SuppressWarnings("serial")
    public void testProcessHelpCommandWithPrefixIII() throws DataSourceException, TwitterException {
        final int senderId = 1111;
        final int dmId = 2222;
        final long consumerKey = 333;
        final String senderScreenName = "Tom";

        // Sender mock
        User sender = createUser(senderId, true, senderScreenName);
        // DirectMessage mock
        final DirectMessage dm = createDM(dmId, senderId, String.valueOf(senderId), sender, "? " + CommandSettings.Prefix.expiration.toString());
        // Twitter mock
        final Twitter twitterAccount = new Twitter() {
            @Override
            public List<DirectMessage> getDirectMessages(Paging paging) {
                List<DirectMessage> messages = new ArrayList<DirectMessage>();
                messages.add(dm);
                return messages;
            }
            @Override
            public DirectMessage sendDirectMessage(String id, String text) {
                assertEquals(senderScreenName, id);
                assertNotSame(0, text.length());
                assertTrue(text.contains(LabelExtractor.get(CommandSettings.HELP_DEFINITION_PREFIX_PREFIX + CommandSettings.Prefix.expiration.toString(), Locale.ENGLISH)));
                return dm;
            }
        };
        // Inject the fake Twitter account
        TwitterUtils.releaseTwetailerAccount(twitterAccount);
        // ConsumerOperations mock
        final ConsumerOperations consumerOperations = new ConsumerOperations() {
            @Override
            public Consumer createConsumer(PersistenceManager pm, User twitterAccount) {
                int twitterId = twitterAccount.getId();
                assertEquals(senderId, twitterId);         // <-- Verify the correct creation submission
                Consumer consumer = new Consumer();
                consumer.setTwitterId(Long.valueOf(twitterId));
                consumer.setKey(consumerKey);
                return consumer;
            }
        };
        // TwitterAdapter mock 
        TwitterAdapter._baseOperations = new MockBaseOperations();
        TwitterAdapter.consumerOperations = consumerOperations;

        // Test itself
        Long newSinceId = TwitterAdapter.processDirectMessages(new MockPersistenceManager(), 1L);
        assertEquals(Long.valueOf(dmId), newSinceId);
        
        // Remove the fake Twitter account
        TwitterUtils.getTwetailerAccount();
    }
    
    @Test
    @SuppressWarnings("serial")
    public void testProcessHelpCommandWithPrefixIV() throws DataSourceException, TwitterException {
        final int senderId = 1111;
        final int dmId = 2222;
        final long consumerKey = 333;
        final String senderScreenName = "Tom";

        // Sender mock
        User sender = createUser(senderId, true, senderScreenName);
        // DirectMessage mock
        final DirectMessage dm = createDM(dmId, senderId, String.valueOf(senderId), sender, CommandSettings.Prefix.range.toString() + "?");
        // Twitter mock
        final Twitter twitterAccount = new Twitter() {
            @Override
            public List<DirectMessage> getDirectMessages(Paging paging) {
                List<DirectMessage> messages = new ArrayList<DirectMessage>();
                messages.add(dm);
                return messages;
            }
            @Override
            public DirectMessage sendDirectMessage(String id, String text) {
                assertEquals(senderScreenName, id);
                assertNotSame(0, text.length());
                assertTrue(text.contains(LabelExtractor.get(CommandSettings.HELP_DEFINITION_PREFIX_PREFIX + CommandSettings.Prefix.range.toString(), Locale.ENGLISH)));
                return dm;
            }
        };
        // Inject the fake Twitter account
        TwitterUtils.releaseTwetailerAccount(twitterAccount);
        // ConsumerOperations mock
        final ConsumerOperations consumerOperations = new ConsumerOperations() {
            @Override
            public Consumer createConsumer(PersistenceManager pm, User twitterAccount) {
                int twitterId = twitterAccount.getId();
                assertEquals(senderId, twitterId);         // <-- Verify the correct creation submission
                Consumer consumer = new Consumer();
                consumer.setTwitterId(Long.valueOf(twitterId));
                consumer.setKey(consumerKey);
                return consumer;
            }
        };
        // TwitterAdapter mock 
        TwitterAdapter._baseOperations = new MockBaseOperations();
        TwitterAdapter.consumerOperations = consumerOperations;

        // Test itself
        Long newSinceId = TwitterAdapter.processDirectMessages(new MockPersistenceManager(), 1L);
        assertEquals(Long.valueOf(dmId), newSinceId);
        
        // Remove the fake Twitter account
        TwitterUtils.getTwetailerAccount();
    }
    
    @Test
    @SuppressWarnings("serial")
    public void testProcessHelpCommandWithActionI() throws DataSourceException, TwitterException {
        final int senderId = 1111;
        final int dmId = 2222;
        final long consumerKey = 333;
        final String senderScreenName = "Tom";

        // Sender mock
        User sender = createUser(senderId, true, senderScreenName);
        // DirectMessage mock
        final DirectMessage dm = createDM(dmId, senderId, String.valueOf(senderId), sender, "help: " + CommandSettings.Action.demand.toString());
        // Twitter mock
        final Twitter twitterAccount = new Twitter() {
            @Override
            public List<DirectMessage> getDirectMessages(Paging paging) {
                List<DirectMessage> messages = new ArrayList<DirectMessage>();
                messages.add(dm);
                return messages;
            }
            @Override
            public DirectMessage sendDirectMessage(String id, String text) {
                assertEquals(senderScreenName, id);
                assertNotSame(0, text.length());
                assertTrue(text.contains(LabelExtractor.get(CommandSettings.HELP_DEFINITION_ACTION_PREFIX + CommandSettings.Action.demand.toString(), Locale.ENGLISH)));
                return dm;
            }
        };
        // Inject the fake Twitter account
        TwitterUtils.releaseTwetailerAccount(twitterAccount);
        // ConsumerOperations mock
        final ConsumerOperations consumerOperations = new ConsumerOperations() {
            @Override
            public Consumer createConsumer(PersistenceManager pm, User twitterAccount) {
                int twitterId = twitterAccount.getId();
                assertEquals(senderId, twitterId);         // <-- Verify the correct creation submission
                Consumer consumer = new Consumer();
                consumer.setTwitterId(Long.valueOf(twitterId));
                consumer.setKey(consumerKey);
                return consumer;
            }
        };
        // TwitterAdapter mock 
        TwitterAdapter._baseOperations = new MockBaseOperations();
        TwitterAdapter.consumerOperations = consumerOperations;

        // Test itself
        Long newSinceId = TwitterAdapter.processDirectMessages(new MockPersistenceManager(), 1L);
        assertEquals(Long.valueOf(dmId), newSinceId);
        
        // Remove the fake Twitter account
        TwitterUtils.getTwetailerAccount();
    }
    
    @Test
    @SuppressWarnings("serial")
    public void testProcessHelpCommandWithRegisteredHelpKeywordI() throws DataSourceException, TwitterException {
        final int senderId = 1111;
        final int dmId = 2222;
        final long consumerKey = 333;
        final String senderScreenName = "Tom";
        final String helpKeyword = "deposit-test";
        final String helpKeywordEquivalent = "prepay-test";

        // Simplified list of registered help keywords
        JsonArray equivalents = new GenericJsonArray();
        equivalents.add(helpKeyword);
        equivalents.add(helpKeywordEquivalent);
        JsonObject helpKeywords = new GenericJsonObject();
        helpKeywords.put(helpKeyword, equivalents);
        TwitterAdapter.localizedHelpKeywords.clear();
        TwitterAdapter.localizedHelpKeywords.put(Locale.ENGLISH, helpKeywords);
        
        // Sender mock
        User sender = createUser(senderId, true, senderScreenName);
        // DirectMessage mock
        final DirectMessage dm = createDM(dmId, senderId, String.valueOf(senderId), sender, "help: " + helpKeywordEquivalent);
        // Twitter mock
        final Twitter twitterAccount = new Twitter() {
            @Override
            public List<DirectMessage> getDirectMessages(Paging paging) {
                List<DirectMessage> messages = new ArrayList<DirectMessage>();
                messages.add(dm);
                return messages;
            }
            @Override
            public DirectMessage sendDirectMessage(String id, String text) {
                assertEquals(senderScreenName, id);
                assertNotSame(0, text.length());
                assertTrue(text.contains(LabelExtractor.get(CommandSettings.HELP_INTRODUCTION_MESSAGE_ID, Locale.ENGLISH)));
                return dm;
            }
        };
        // Inject the fake Twitter account
        TwitterUtils.releaseTwetailerAccount(twitterAccount);
        // ConsumerOperations mock
        final ConsumerOperations consumerOperations = new ConsumerOperations() {
            @Override
            public Consumer createConsumer(PersistenceManager pm, User twitterAccount) {
                int twitterId = twitterAccount.getId();
                assertEquals(senderId, twitterId);         // <-- Verify the correct creation submission
                Consumer consumer = new Consumer();
                consumer.setTwitterId(Long.valueOf(twitterId));
                consumer.setKey(consumerKey);
                return consumer;
            }
        };
        // TwitterAdapter mock 
        TwitterAdapter._baseOperations = new MockBaseOperations();
        TwitterAdapter.consumerOperations = consumerOperations;

        // Test itself
        Long newSinceId = TwitterAdapter.processDirectMessages(new MockPersistenceManager(), 1L);
        assertEquals(Long.valueOf(dmId), newSinceId);
        
        // Remove the fake Twitter account
        TwitterUtils.getTwetailerAccount();
    }
}