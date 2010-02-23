package twetailer.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import twetailer.connector.BaseConnector.Source;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;

import domderrien.jsontools.GenericJsonObject;
import domderrien.jsontools.JsonException;
import domderrien.jsontools.JsonObject;
import domderrien.jsontools.JsonParser;

public class TestSaleAssociate {

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
        SaleAssociate object = new SaleAssociate();
        assertNull(object.getKey());
        assertNotNull(object.getCreationDate());
    }

    @Test
    public void testConstructorII() throws JsonException {
        SaleAssociate object = new SaleAssociate(new JsonParser("{}").getJsonObject());
        assertNull(object.getKey());
        assertNotNull(object.getCreationDate());
    }

    Long consumerKey = 67890L;
    Long creatorKey = 12345L;
    List<String> criteria = new ArrayList<String>(Arrays.asList(new String[] {"first", "second"}));
    String email = "d.d@d.dom";
    String imId = "ddd";
    Boolean isStoreAdmin = Boolean.TRUE;
    String language = Locale.FRENCH.getLanguage();
    Long locationKey = 12345L;
    String name = "dom";
    String openID = "http://dom.my-openid.org";
    String phoneNumber = "514-123-4567 #890";
    Long storeKey = 54321L;
    Long score = 5L;
    String twitterId = "Ryan";

    @Test
    public void testAccessors() {
        SaleAssociate object = new SaleAssociate();

        object.setConsumerKey(consumerKey);
        object.setCreatorKey(creatorKey);
        object.setCriteria(criteria);
        object.setEmail(email);
        object.setJabberId(imId);
        object.setIsStoreAdmin(isStoreAdmin);
        object.setLanguage(language);
        object.setLocationKey(locationKey);
        object.setName(name);
        object.setOpenID(openID);
        object.setPhoneNumber(phoneNumber);
        object.setStoreKey(storeKey);
        object.setScore(score);
        object.setTwitterId(twitterId);

        assertEquals(consumerKey, object.getConsumerKey());
        assertEquals(creatorKey, object.getCreatorKey());
        assertEquals(criteria, object.getCriteria());
        assertEquals(email, object.getEmail());
        assertEquals(imId, object.getJabberId());
        assertEquals(isStoreAdmin, object.getIsStoreAdmin());
        assertEquals(language, object.getLanguage());
        assertEquals(locationKey, object.getLocationKey());
        assertEquals(name, object.getName());
        assertEquals(name, object.getName());
        assertEquals(storeKey, object.getStoreKey());
        assertEquals(score, object.getScore());
        assertEquals(twitterId, object.getTwitterId());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testResetCriteriaI() {
        SaleAssociate object = new SaleAssociate();

        object.addCriterion(null);
        assertEquals(0, object.getCriteria().size());

        object.addCriterion("");
        assertEquals(0, object.getCriteria().size());

        object.addCriterion("first");
        assertEquals(1, object.getCriteria().size());

        object.addCriterion("first"); // Add it twice
        assertEquals(1, object.getCriteria().size());

        object.addCriterion("FiRsT"); // Add it twice, mixed case
        assertEquals(1, object.getCriteria().size());

        object.addCriterion("second");
        assertEquals(2, object.getCriteria().size());

        object.removeCriterion("first"); // Remove first
        assertEquals(1, object.getCriteria().size());

        object.addCriterion("Troisième");
        assertEquals(2, object.getCriteria().size());

        object.addCriterion("TROISIÈME");
        assertEquals(2, object.getCriteria().size());

        object.removeCriterion("TROISIÈME"); // Remove mixed case and disparate accents
        assertEquals(1, object.getCriteria().size());

        object.removeCriterion(null);
        assertEquals(1, object.getCriteria().size());

        object.removeCriterion("");
        assertEquals(1, object.getCriteria().size());

        object.resetCriteria(); // Reset all
        assertEquals(0, object.getCriteria().size());

        object.setCriteria(null); // Failure!
    }

    @Test
    public void testResetCriteriaII() {
        SaleAssociate object = new SaleAssociate();

        object.resetLists(); // To force the criteria list creation
        object.addCriterion("first");
        assertEquals(1, object.getCriteria().size());

        object.resetLists(); // To be sure there's no error
        object.removeCriterion("first"); // Remove first

        object.resetLists(); // To be sure there's no error
        object.resetCriteria(); // Reset all
    }

    @Test
    public void testGetLocale() {
        SaleAssociate object = new SaleAssociate();
        object.setLanguage(language);
        assertEquals(Locale.FRENCH, object.getLocale());
    }

    @Test
    public void testJsonCommandsI() {
        SaleAssociate object = new SaleAssociate();

        object.setConsumerKey(consumerKey);
        object.setCreatorKey(creatorKey);
        object.setCriteria(criteria);
        object.setEmail(email);
        object.setJabberId(imId);
        object.setIsStoreAdmin(isStoreAdmin);
        object.setLanguage(language);
        object.setLocationKey(locationKey);
        object.setName(name);
        object.setOpenID(openID);
        object.setPhoneNumber(phoneNumber);
        object.setStoreKey(storeKey);
        object.setScore(score);
        object.setTwitterId(twitterId);

        SaleAssociate clone = new SaleAssociate(object.toJson());

        assertEquals(consumerKey, clone.getConsumerKey());
        assertEquals(creatorKey, clone.getCreatorKey());
        assertEquals(criteria, clone.getCriteria());
        assertEquals(email, clone.getEmail());
        assertEquals(imId, clone.getJabberId());
        assertEquals(isStoreAdmin, clone.getIsStoreAdmin());
        assertEquals(language, clone.getLanguage());
        assertEquals(locationKey, clone.getLocationKey());
        assertEquals(name, clone.getName());
        assertEquals(name, clone.getName());
        assertEquals(storeKey, clone.getStoreKey());
        assertEquals(score, clone.getScore());
        assertEquals(twitterId, clone.getTwitterId());
    }

    @Test
    public void testJsonCommandsII() {
        SaleAssociate object = new SaleAssociate();

        object.resetLists();

        assertNull(object.getConsumerKey());
        assertNull(object.getCreatorKey());
        assertNull(object.getCriteria());
        assertNull(object.getLocationKey());
        assertNull(object.getStoreKey());
        assertNull(object.getScore());
        assertNull(object.getTwitterId());

        SaleAssociate clone = new SaleAssociate(object.toJson());

        assertNull(clone.getConsumerKey());
        assertNull(clone.getCreatorKey());
        assertEquals(0, clone.getCriteria().size()); // Not null because the clone object creation creates empty List<String>
        assertNull(clone.getLocationKey());
        assertNull(clone.getStoreKey());
        assertNull(clone.getScore());
        assertNull(clone.getTwitterId());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testSetPreferredConnection() {
        SaleAssociate object = new SaleAssociate();

        object.setPreferredConnection((Source) null);
    }

    @Test
    public void testShortcut() {
        Long key = 12345L;
        JsonObject parameters = new GenericJsonObject();
        parameters.put(SaleAssociate.SALEASSOCIATE_KEY, key);

        assertEquals(key, new SaleAssociate(parameters).getKey());
    }

    @Test
    public void testGetSerialized() {
        SaleAssociate saleAssociate = new SaleAssociate();
        saleAssociate.addCriterion("one");
        saleAssociate.addCriterion("two");
        saleAssociate.addCriterion("three");

        assertEquals("one two three", saleAssociate.getSerializedCriteria());
    }

    @Test
    public void testGetIsStoreAdmin() {
        SaleAssociate saleAssociate = new SaleAssociate();
        assertFalse(saleAssociate.getIsStoreAdmin());
        saleAssociate.setIsStoreAdmin(null);
        assertFalse(saleAssociate.getIsStoreAdmin());
        saleAssociate.setIsStoreAdmin(false);
        assertFalse(saleAssociate.getIsStoreAdmin());
        saleAssociate.setIsStoreAdmin(true);
        assertTrue(saleAssociate.getIsStoreAdmin());
    }

    @Test
    public void testGetLocatorI() {
        assertNotNull(new SaleAssociate().getCollator());
    }

    @Test
    public void testGetLocatorII() {
        SaleAssociate saleAssociate = new SaleAssociate();
        Collator collator = saleAssociate.getCollator();
        assertEquals(collator, saleAssociate.getCollator());
    }
}
