package twetailer.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;

import domderrien.jsontools.GenericJsonObject;
import domderrien.jsontools.JsonException;
import domderrien.jsontools.JsonObject;
import domderrien.jsontools.JsonParser;

public class TestStore {

    private static LocalServiceTestHelper  helper;

    @BeforeClass
    public static void setUpBeforeClass() {
        helper = new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
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
        Store object = new Store();
        assertNull(object.getKey());
        assertNotNull(object.getCreationDate());
    }

    @Test
    public void testConstructorII() throws JsonException {
        Store object = new Store(new JsonParser("{}").getJsonObject());
        assertNull(object.getKey());
        assertNotNull(object.getCreationDate());
    }

    String address = "North Pole, H0H 0H0, Canada";
    String email = "d.d@d.dom";
    Long locationKey = 12345L;
    String name = "dom";
    String phoneNumber = "514-123-4567 #890";

    @Test
    public void testAccessors() {
        Store object = new Store();

        object.setAddress(address);
        object.setEmail(email);
        object.setLocationKey(locationKey);
        object.setName(name);
        object.setPhoneNumber(phoneNumber);

        assertEquals(address, object.getAddress());
        assertEquals(email, object.getEmail());
        assertEquals(locationKey, object.getLocationKey());
        assertEquals(name, object.getName());
        assertEquals(phoneNumber, object.getPhoneNumber());
    }

    @Test
    public void testJsonCommandsI() {
        Store object = new Store();

        object.setAddress(address);
        object.setEmail(email);
        object.setLocationKey(locationKey);
        object.setName(name);
        object.setPhoneNumber(phoneNumber);

        Store clone = new Store(object.toJson());

        assertEquals(address, clone.getAddress());
        assertEquals(email, clone.getEmail());
        assertEquals(locationKey, clone.getLocationKey());
        assertEquals(name, clone.getName());
        assertEquals(phoneNumber, clone.getPhoneNumber());
    }

    @Test
    public void testJsonCommandsII() {
        Store object = new Store();

        assertNull(object.getLocationKey());

        Store clone = new Store(object.toJson());

        assertNull(clone.getLocationKey());
    }

    @Test
    public void testShortcut() {
        Long key = 12345L;
        JsonObject parameters = new GenericJsonObject();
        parameters.put(Store.STORE_KEY, key);

        assertEquals(key, new Store(parameters).getKey());
    }

    @Test
    public void testGetAddress() {
        Store store = new Store();
        store.setAddress(null);
        assertNull(store.getAddress());
        store.setAddress("");
        assertNull(store.getAddress());
        store.setAddress("test");
        assertEquals("test", store.getAddress());
    }

    @Test
    public void testGetEmail() {
        Store store = new Store();
        store.setEmail(null);
        assertNull(store.getEmail());
        store.setEmail("");
        assertNull(store.getEmail());
        store.setEmail("test");
        assertEquals("test", store.getEmail());
    }

    @Test
    public void testGetName() {
        Store store = new Store();
        store.setName(null);
        assertNull(store.getName());
        store.setName("");
        assertNull(store.getName());
        store.setName("test");
        assertEquals("test", store.getName());
    }

    @Test
    public void testGetPhoneNumber() {
        Store store = new Store();
        store.setPhoneNumber(null);
        assertNull(store.getPhoneNumber());
        store.setPhoneNumber("");
        assertNull(store.getPhoneNumber());
        store.setPhoneNumber("test");
        assertEquals("test", store.getPhoneNumber());
    }
}
