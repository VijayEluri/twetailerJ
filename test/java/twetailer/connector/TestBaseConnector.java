package twetailer.connector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.Locale;

import javamocks.util.logging.MockLogger;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import twetailer.ClientException;
import twetailer.connector.BaseConnector.Source;
import twetailer.dto.Consumer;
import twetailer.dto.RawCommand;
import twetailer.dto.SaleAssociate;
import twitter4j.DirectMessage;
import twitter4j.Twitter;
import twitter4j.TwitterException;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;

public class TestBaseConnector {

    private static LocalServiceTestHelper  helper;

    @BeforeClass
    public static void setUpBeforeClass() {
        BaseConnector.setLogger(new MockLogger("test", null));
        helper = new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());;
    }

    @Before
    public void setUp() throws Exception {
        helper.setUp();
    }

    @After
    public void tearDown() throws Exception {
        helper.tearDown();
    }

    @Test
    public void testConstructor() {
        new BaseConnector();
    }

    @Test(expected=ClientException.class)
    public void testUnsupportedSource() throws ClientException {
        BaseConnector.communicateToUser(null, null, null, null, null, Locale.ENGLISH);
    }

    @Test
    public void testSimulatedSource() throws ClientException {
        BaseConnector.resetLastCommunicationInSimulatedMode();
        assertNull(BaseConnector.getLastCommunicationInSimulatedMode());

        final String message = "test";
        BaseConnector.communicateToUser(Source.simulated, null, null, null, new String[] { message }, Locale.ENGLISH);

        assertEquals(BaseConnector.getLastCommunicationInSimulatedMode(), message);
    }

    @Test
    public void testFromRawCommand() throws ClientException {
        RawCommand rawCommand = new RawCommand(Source.simulated);

        final String message = "test";
        BaseConnector.communicateToEmitter(rawCommand, new String[] { message }, Locale.ENGLISH);

        assertEquals(BaseConnector.getLastCommunicationInSimulatedMode(), message);
    }

    @Test
    @SuppressWarnings({ "serial", "deprecation" })
    public void testTwitterSourceI() throws ClientException {
        final String twitterId = "tId";
        final String message = "test";
        final Twitter mockTwitterAccount = (new Twitter() {
            @Override
            public DirectMessage sendDirectMessage(String id, String text) throws TwitterException {
                assertEquals(twitterId, id);
                assertEquals(message, text);
                return null;
            }
        });
        MockTwitterConnector.injectMockTwitterAccount(mockTwitterAccount);

        BaseConnector.communicateToUser(Source.twitter, twitterId, null, null, new String[] { message }, Locale.ENGLISH);

        MockTwitterConnector.restoreTwitterConnector(mockTwitterAccount, null);
    }

    @Test(expected=ClientException.class)
    @SuppressWarnings({ "serial", "deprecation" })
    public void testTwitterSourceII() throws ClientException {
        final String twitterId = "tId";
        final String message = "test";
        final Twitter mockTwitterAccount = (new Twitter() {
            @Override
            public DirectMessage sendDirectMessage(String id, String text) throws TwitterException {
                throw new TwitterException("Done in purpose");
            }
        });
        MockTwitterConnector.injectMockTwitterAccount(mockTwitterAccount);

        BaseConnector.communicateToUser(Source.twitter, twitterId, null, null, new String[] { message }, Locale.ENGLISH);

        MockTwitterConnector.restoreTwitterConnector(mockTwitterAccount, null);
    }

    @Test
    public void testJabberSource() throws ClientException {
        final String jabberId = "jId";
        final String message = "test";
        BaseConnector.communicateToUser(Source.jabber, jabberId, null, null, new String[] { message }, Locale.ENGLISH);
    }

    @Test
    public void testMailSourceI() throws ClientException {
        final String mailAddress = "unit@test.net";
        final String message = "test";
        final String subject = "subject";
        BaseConnector.communicateToUser(Source.mail, mailAddress, null, subject, new String[] { message }, Locale.ENGLISH);
    }

    @Test(expected=ClientException.class)
    public void testMailSourceII() throws ClientException {
        final String mailAddress = "@@@";
        final String message = "test";
        final String subject = "subject";
        BaseConnector.communicateToUser(Source.mail, mailAddress, null, subject, new String[] { message }, Locale.ENGLISH);
    }

    @Test(expected=RuntimeException.class)
    public void testFacebookSource() throws ClientException {
        final String facebookId = "fId";
        final String message = "test";
        final String subject = "subject";
        BaseConnector.communicateToUser(Source.facebook, facebookId, null, subject, new String[] { message }, Locale.ENGLISH);
    }

    @Test
    public void testCommunicateToConsumerI() throws ClientException {
        BaseConnector.communicateToConsumer(new RawCommand(Source.simulated), new Consumer(), new String[0]);
    }

    @Test
    public void testCommunicateToConsumerII() throws ClientException {
        BaseConnector.communicateToConsumer(new RawCommand(Source.twitter), new Consumer(), new String[0]);
    }

    @Test
    public void testCommunicateToConsumerIII() throws ClientException {
        BaseConnector.communicateToConsumer(new RawCommand(Source.jabber), new Consumer(), new String[0]);
    }

    @Test
    public void testCommunicateToConsumerIV() throws ClientException {
        BaseConnector.communicateToConsumer(new RawCommand(Source.mail), new Consumer(), new String[0]);
    }

    @Test
    public void testCommunicateToConsumerV() throws ClientException {
        BaseConnector.communicateToConsumer(new RawCommand(Source.mail), new Consumer() { @Override public String getEmail() { return "unit@test.net"; } }, new String[0]);
    }

    @Test
    public void testCommunicateToSaleAssociateI() throws ClientException {
        BaseConnector.communicateToSaleAssociate(new RawCommand(Source.simulated), new SaleAssociate(), new String[0]);
    }

    @Test
    public void testCommunicateToSaleAssociateII() throws ClientException {
        BaseConnector.communicateToSaleAssociate(new RawCommand(Source.twitter), new SaleAssociate(), new String[0]);
    }

    @Test
    public void testCommunicateToSaleAssociateIII() throws ClientException {
        BaseConnector.communicateToSaleAssociate(new RawCommand(Source.jabber), new SaleAssociate(), new String[0]);
    }

    @Test
    public void testCommunicateToSaleAssociateIV() throws ClientException {
        BaseConnector.communicateToSaleAssociate(new RawCommand(Source.mail), new SaleAssociate(), new String[0]);
    }

    @Test
    public void testCommunicateToSaleAssociateV() throws ClientException {
        BaseConnector.communicateToSaleAssociate(new RawCommand(Source.mail), new SaleAssociate() { @Override public String getEmail() { return "unit@test.net"; } }, new String[0]);
    }

    @Test
    public void testCommunicateManyMessagesI() throws ClientException {
        BaseConnector.resetLastCommunicationInSimulatedMode();
        assertNull(BaseConnector.getLastCommunicationInSimulatedMode());
        assertNull(BaseConnector.getCommunicationForRetroIndexInSimulatedMode(0));
        assertNull(BaseConnector.getCommunicationForRetroIndexInSimulatedMode(1));
        assertNull(BaseConnector.getCommunicationForRetroIndexInSimulatedMode(2));
        assertNull(BaseConnector.getCommunicationForRetroIndexInSimulatedMode(1000));

        String first = "first";
        BaseConnector.communicateToSaleAssociate(new RawCommand(Source.simulated), new SaleAssociate(), new String[] { first });
        assertNotNull(BaseConnector.getLastCommunicationInSimulatedMode());
        assertEquals(first, BaseConnector.getLastCommunicationInSimulatedMode());
        assertEquals(first, BaseConnector.getCommunicationForRetroIndexInSimulatedMode(0));
        assertNull(BaseConnector.getCommunicationForRetroIndexInSimulatedMode(1));

        String second = "second";
        BaseConnector.communicateToSaleAssociate(new RawCommand(Source.simulated), new SaleAssociate(), new String[] { second });
        assertNotNull(BaseConnector.getLastCommunicationInSimulatedMode());
        assertEquals(second, BaseConnector.getLastCommunicationInSimulatedMode());
        assertEquals(second, BaseConnector.getCommunicationForRetroIndexInSimulatedMode(0));
        assertEquals(first, BaseConnector.getCommunicationForRetroIndexInSimulatedMode(1));
        assertNull(BaseConnector.getCommunicationForRetroIndexInSimulatedMode(2));
    }

    @Test
    public void testCommunicateManyMessagesII() throws ClientException {
        BaseConnector.resetLastCommunicationInSimulatedMode();
        assertNull(BaseConnector.getLastCommunicationInSimulatedMode());
        assertNull(BaseConnector.getCommunicationForRetroIndexInSimulatedMode(0));
        assertNull(BaseConnector.getCommunicationForRetroIndexInSimulatedMode(1));
        assertNull(BaseConnector.getCommunicationForRetroIndexInSimulatedMode(2));
        assertNull(BaseConnector.getCommunicationForRetroIndexInSimulatedMode(1000));

        String first = "first";
        String second = "second";
        BaseConnector.communicateToSaleAssociate(new RawCommand(Source.simulated), new SaleAssociate(), new String[] { first, second });
        assertNotNull(BaseConnector.getLastCommunicationInSimulatedMode());
        assertEquals(second, BaseConnector.getLastCommunicationInSimulatedMode());
        assertEquals(second, BaseConnector.getCommunicationForRetroIndexInSimulatedMode(0));
        assertEquals(first, BaseConnector.getCommunicationForRetroIndexInSimulatedMode(1));
        assertNull(BaseConnector.getCommunicationForRetroIndexInSimulatedMode(2));
    }

    @Test
    public void testCheckMessageLengthIa() {
        List<String> output = BaseConnector.checkMessageLength(null, 1000);
        assertNotNull(output);
        assertEquals(0, output.size());
    }

    @Test
    public void testCheckMessageLengthIb() {
        List<String> output = BaseConnector.checkMessageLength("", 1000);
        assertNotNull(output);
        assertEquals(0, output.size());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testCheckMessageLengthIc() {
        BaseConnector.checkMessageLength("", 0);
    }

    @Test
    public void testCheckMessageLengthII() {
        String message = "blah blah blah";
        List<String> output = BaseConnector.checkMessageLength(message, 1000);
        assertNotNull(output);
        assertEquals(1, output.size());
        assertEquals(message, output.get(0));
    }

    @Test
    public void testCheckMessageLengthIIIa() {
        String part1 = "blah blah blah";
        String part2 = "and more";
        String message = part1 + BaseConnector.SUGGESTED_MESSAGE_SEPARATOR + part2;
        List<String> output = BaseConnector.checkMessageLength(message, 1000);
        assertNotNull(output);
        assertEquals(2, output.size());
        assertEquals(part1, output.get(0));
        assertEquals(part2, output.get(1));
    }

    @Test
    public void testCheckMessageLengthIIIb() {
        String part1 = "blah blah blah";
        String part2 = "and more";
        String message = part1 + BaseConnector.SUGGESTED_MESSAGE_SEPARATOR + BaseConnector.SUGGESTED_MESSAGE_SEPARATOR + BaseConnector.SUGGESTED_MESSAGE_SEPARATOR + part2;
        List<String> output = BaseConnector.checkMessageLength(message, 1000);
        assertNotNull(output);
        assertEquals(4, output.size());
        assertEquals(part1, output.get(0));
        assertEquals("", output.get(1));
        assertEquals("", output.get(2));
        assertEquals(part2, output.get(3));
    }

    @Test
    public void testCheckMessageLengthIIIc() {
        String part1 = "blah blah blah";
        String part2 = "and more";
        String message = part1 + BaseConnector.SUGGESTED_MESSAGE_SEPARATOR + part2 + BaseConnector.SUGGESTED_MESSAGE_SEPARATOR;
        List<String> output = BaseConnector.checkMessageLength(message, 1000);
        assertNotNull(output);
        assertEquals(2, output.size());
        assertEquals(part1, output.get(0));
        assertEquals(part2, output.get(1));
    }

    @Test
    public void testCheckMessageLengthIVa() {
        // Test separator after the word
        String part1 = "blah blah blah";
        String part2 = "and more";
        String message = part1 + " " + part2; // Space
        List<String> output = BaseConnector.checkMessageLength(message, part1.length());
        assertNotNull(output);
        assertEquals(2, output.size());
        assertEquals(part1, output.get(0));
        assertEquals(part2, output.get(1));
    }

    @Test
    public void testCheckMessageLengthIVb() {
        // Test separator after the word
        String part1 = "blah blah blah";
        String part2 = "and more";
        String message = part1 + "\t" + part2; // Space
        List<String> output = BaseConnector.checkMessageLength(message, part1.length());
        assertNotNull(output);
        assertEquals(2, output.size());
        assertEquals(part1, output.get(0));
        assertEquals(part2, output.get(1));
    }

    @Test
    public void testCheckMessageLengthVa() {
        // Test separator before the word
        String part1 = "blah blah blah";
        String part2 = "and more";
        String message = part1 + " " + part2; // Space
        List<String> output = BaseConnector.checkMessageLength(message, part1.length() + 3);
        assertNotNull(output);
        assertEquals(2, output.size());
        assertEquals(part1, output.get(0));
        assertEquals(part2, output.get(1));
    }

    @Test
    public void testCheckMessageLengthVb() {
        // Test separator before the word
        String part1 = "blah blah blah";
        String part2 = "and more";
        String message = part1 + "\t" + part2; // Space
        List<String> output = BaseConnector.checkMessageLength(message, part1.length() + 3);
        assertNotNull(output);
        assertEquals(2, output.size());
        assertEquals(part1, output.get(0));
        assertEquals(part2, output.get(1));
    }

    @Test
    public void testCheckMessageLengthVI() {
        // Verify the trim
        String part1 = "blah blah blah";
        String part2 = "and more";
        String message = part1 + " \t \t " + part2; // Space
        List<String> output = BaseConnector.checkMessageLength(message, part1.length() + 3);
        assertNotNull(output);
        assertEquals(2, output.size());
        assertEquals(part1, output.get(0));
        assertEquals(part2, output.get(1));
    }

    @Test
    public void testMailMultipleMessages() throws ClientException {
        final String mailAddress = "unit@test.net";
        final String subject = "subject";
        final String message1 = "test1";
        final String message2 = "test2";
        BaseConnector.communicateToUser(Source.mail, mailAddress, null, subject, new String[] { message1, message2 }, Locale.ENGLISH);
    }

    @Test
    public void testCheckMessageLengthVII() {
        String message = ":-) Proposal:106004 for tags:nikon d500 has been confirmed.| Please mark, hold for Consumer with Demand reference:106002 and then !close proposal:106004, or, !flag proposal:106004 note:your-note-here.";
        List<String> output = BaseConnector.checkMessageLength(message, 140);
        assertEquals(2, output.size());
    }
}
