package twetailer.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import twetailer.connector.BaseConnector.Source;
import twetailer.validator.CommandSettings;
import twetailer.validator.CommandSettings.Action;
import twetailer.validator.CommandSettings.State;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;

import domderrien.jsontools.GenericJsonArray;
import domderrien.jsontools.GenericJsonObject;
import domderrien.jsontools.JsonException;
import domderrien.jsontools.JsonObject;
import domderrien.jsontools.JsonParser;

public class TestCommand {

    private static LocalServiceTestHelper  helper;

    @BeforeClass
    public static void setUpBeforeClass() {
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
    public void testConstructorI() {
        Command object = new Command();
        assertNull(object.getKey());
        assertNotNull(object.getCreationDate());
    }

    @Test
    public void testConstructorII() throws JsonException {
        Command object = new Command(new JsonParser("{}").getJsonObject());
        assertNull(object.getKey());
        assertNotNull(object.getCreationDate());
    }

    Action action = Action.cancel;
    List<String> hashTags = new ArrayList<String>(Arrays.asList(new String[] {"first", "second"}));
    Long locationKey = 87541L;
    Long ownerKey = 12345L;
    Long rawCommandId = 67890L;
    Source source = Source.simulated;
    Long cancelerKey = 76545L;
    CommandSettings.State state = CommandSettings.State.closed;

    @Test
    public void testAccessors() {
        Command object = new Command();

        object.setAction(action);
        object.setAction(action.toString());
        object.setCancelerKey(cancelerKey);
        object.setHashTags(hashTags);
        object.setLocationKey(locationKey);
        object.setOwnerKey(ownerKey);
        object.setRawCommandId(rawCommandId);
        object.setSource(source);
        object.setSource(source.toString());
        object.setState(state);
        object.setState(state.toString());

        assertEquals(action, object.getAction());
        assertEquals(cancelerKey, object.getCancelerKey());
        assertEquals(hashTags, object.getHashTags());
        assertEquals(locationKey, object.getLocationKey());
        assertEquals(ownerKey, object.getOwnerKey());
        assertEquals(rawCommandId, object.getRawCommandId());
        assertEquals(source, object.getSource());
        assertEquals(state, object.getState());
    }

    @Test
    public void testJsonCommandsI() {
        Command object = new Command();

        object.setAction(action);
        object.setCancelerKey(cancelerKey);
        object.setHashTags(hashTags);
        object.setLocationKey(locationKey);
        object.setOwnerKey(ownerKey);
        object.setRawCommandId(rawCommandId);
        object.setSource(source);
        object.setState(state);

        Command clone = new Command(object.toJson());

        assertEquals(action, clone.getAction());
        assertEquals(cancelerKey, clone.getCancelerKey());
        assertEquals(hashTags, clone.getHashTags());
        assertEquals(locationKey, clone.getLocationKey());
        assertEquals(ownerKey, clone.getOwnerKey());
        assertEquals(rawCommandId, clone.getRawCommandId());
        assertEquals(source, clone.getSource());
        assertEquals(state, clone.getState());
    }

    @Test
    public void testJsonCommandsII() {
        Command object = new Command();
        object.setSource(source);

        assertNull(object.getRawCommandId());

        Command clone = new Command(object.toJson());

        assertNull(clone.getRawCommandId());
    }

    @Test
    public void testJsonDemandsIII() {
        Command object = new Command();
        object.setSource(source);

        object.resetLists();

        // Demand
        assertNull(object.getHashTags());

        Command clone = new Command(object.toJson());

        // Demand
        assertEquals(0, clone.getHashTags().size()); // Not null because the clone object creation creates empty List<String>
    }

    @Test(expected=IllegalArgumentException.class)
    public void testSetAction() {
        Command object = new Command();

        object.setAction((Action) null);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testSetSource() {
        Command object = new Command();

        object.setSource((Source) null);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testSetState() {
        Command object = new Command();

        object.setState((State) null);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testResetHashTagsI() {
        Command object = new Command();

        object.addHashTag("first");
        assertEquals(1, object.getHashTags().size());

        object.addHashTag("first"); // Add it twice
        assertEquals(1, object.getHashTags().size());

        object.addHashTag("second");
        assertEquals(2, object.getHashTags().size());

        object.removeHashTag("first"); // Remove first
        assertEquals(1, object.getHashTags().size());

        object.resetHashTags(); // Reset all
        assertEquals(0, object.getHashTags().size());

        object.setHashTags(null); // Failure!
    }

    @Test
    public void testResetHashTagsII() {
        Command object = new Command();

        object.resetLists(); // To force the hashTags list creation
        object.addHashTag("first");
        assertEquals(1, object.getHashTags().size());

        object.resetLists(); // To be sure there's no error
        object.removeHashTag("first"); // Remove first

        object.resetLists(); // To be sure there's no error
        object.resetHashTags(); // Reset all
    }

    @Test
    public void testGetSerializedTagsI() {
        assertEquals(Command.EMPTY_STRING, Command.getSerializedTags(null));
    }

    @Test
    public void testGetSerializedTagsII() {
        assertEquals(Command.EMPTY_STRING, Command.getSerializedTags(new ArrayList<String>()));
    }

    @Test
    public void testGetSerializedTagsIII() {
        assertEquals(Command.EMPTY_STRING, Command.getSerializedTags("Not important!", "Not important!", null));
    }

    @Test
    public void testGetSerializedTagsIV() {
        assertEquals(Command.EMPTY_STRING, Command.getSerializedTags("Not important!", "Not important!", new ArrayList<String>()));
    }

    @Test
    public void testGetSerializedHashTagsI() {
        Command command = new Command();

        assertEquals(Command.EMPTY_STRING, command.getSerializedHashTags());
    }

    @Test
    public void testGetSerializedHashTagsII() {
        Command command = new Command();
        command.addHashTag("one");
        command.addHashTag("two");
        command.addHashTag("three");

        assertEquals("#one #two #three", command.getSerializedHashTags());
    }

    @Test
    public void testGetSerializedHashTagsIII() {
        Command command = new Command();
        command.addHashTag("one");
        command.addHashTag("two");
        command.addHashTag("three");

        assertEquals("one two three", Command.getSerializedTags(command.getHashTags()));
        assertEquals("#one #two #three", Command.getSerializedTags(Command.HASH, Command.SPACE, command.getHashTags()));
    }

    @Test
    public void testHashTagJsonI() {
        Command command = new Command();
        command.addHashTag("one");
        command.addHashTag("two");
        command.addHashTag("three");

        JsonObject json = new GenericJsonObject();
        json.put(Command.HASH_TAGS_ADD, new GenericJsonArray());
        json.getJsonArray(Command.HASH_TAGS_ADD).add("four");

        command.fromJson(json);

        assertEquals(4, command.getHashTags().size());
    }

    @Test
    public void testHashTagJsonII() {
        Command command = new Command();
        command.addHashTag("one");
        command.addHashTag("two");
        command.addHashTag("three");

        JsonObject json = new GenericJsonObject();
        json.put(Command.HASH_TAGS_REMOVE, new GenericJsonArray());
        json.getJsonArray(Command.HASH_TAGS_REMOVE).add("two");

        command.fromJson(json);

        assertEquals(2, command.getHashTags().size());
    }
}
