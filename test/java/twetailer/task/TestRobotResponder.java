package twetailer.task;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.jdo.PersistenceManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import twetailer.DataSourceException;
import twetailer.connector.BaseConnector;
import twetailer.connector.BaseConnector.Source;
import twetailer.dao.BaseOperations;
import twetailer.dao.ConsumerOperations;
import twetailer.dao.DemandOperations;
import twetailer.dao.LocationOperations;
import twetailer.dao.MockAppEngineEnvironment;
import twetailer.dao.MockPersistenceManager;
import twetailer.dao.ProposalOperations;
import twetailer.dao.RetailerOperations;
import twetailer.dao.StoreOperations;
import twetailer.dto.Consumer;
import twetailer.dto.Demand;
import twetailer.dto.Location;
import twetailer.dto.Proposal;
import twetailer.dto.Retailer;
import twetailer.dto.Store;
import twetailer.validator.CommandSettings.State;
import twitter4j.TwitterException;
import domderrien.i18n.LabelExtractor;

public class TestRobotResponder {

    private class MockBaseOperations extends BaseOperations {
        PersistenceManager pm = new MockPersistenceManager();
        @Override
        public PersistenceManager getPersistenceManager() {
            return pm;
        }
    };

    @Before
    public void setUp() throws Exception {
        // Install the mocks
        RobotResponder._baseOperations = new MockBaseOperations();

        // Be sure to start with a clean message stack
        BaseConnector.resetLastCommunicationInSimulatedMode();
    }

    @After
    public void tearDown() throws Exception {
        RobotResponder._baseOperations = new BaseOperations();
        RobotResponder.demandOperations = RobotResponder._baseOperations.getDemandOperations();
        RobotResponder.consumerOperations = RobotResponder._baseOperations.getConsumerOperations();
        RobotResponder.locationOperations = RobotResponder._baseOperations.getLocationOperations();
        RobotResponder.retailerOperations = RobotResponder._baseOperations.getRetailerOperations();
        RobotResponder.proposalOperations = RobotResponder._baseOperations.getProposalOperations();
        RobotResponder.storeOperations = RobotResponder._baseOperations.getStoreOperations();
    }

    @Test
    public void testContructor() {
        new RobotResponder();
    }

    final Long demandKey = 111L;
    final Long retailerKey = 222L;
    final Long locationKey = 333L;
    final Long storeKey = 444L;
    final Long consumerKey = 555L;
    final Long proposalKey = 666L;

    @Test
    public void testProcessDemandI() throws TwitterException, DataSourceException {
        RobotResponder.retailerOperations = new RetailerOperations() {
            @Override
            public List<Retailer> getRetailers(PersistenceManager pm, String key, Object value, int limit) {
                List<Retailer> retailers = new ArrayList<Retailer>();
                return retailers;
            }
        };

        RobotResponder.processDemand(demandKey);

        assertNull(BaseConnector.getLastCommunicationInSimulatedMode());
        assertTrue(RobotResponder._baseOperations.getPersistenceManager().isClosed());
    }

    @Test
    public void testProcessDemandII() throws TwitterException, DataSourceException {
        RobotResponder.retailerOperations = new RetailerOperations() {
            @Override
            public List<Retailer> getRetailers(PersistenceManager pm, String key, Object value, int limit) {
                Retailer retailer = new Retailer();
                retailer.setKey(retailerKey);
                retailer.setStoreKey(storeKey);
                List<Retailer> retailers = new ArrayList<Retailer>();
                retailers.add(retailer);
                return retailers;
            }
        };
        RobotResponder.locationOperations = new LocationOperations() {
            @Override
            public List<Location> getLocations(PersistenceManager pm, String postalCode, String countryCode) {
                assertEquals(RobotResponder.ROBOT_POSTAL_CODE, postalCode);
                assertEquals(RobotResponder.ROBOT_COUNTRY_CODE, countryCode);
                Location location = new Location();
                location.setKey(locationKey);
                location.setPostalCode(RobotResponder.ROBOT_POSTAL_CODE);
                location.setCountryCode(RobotResponder.ROBOT_COUNTRY_CODE);
                List<Location> locations = new ArrayList<Location>();
                locations.add(location);
                return locations;
            }
        };
        RobotResponder.storeOperations = new StoreOperations() {
            @Override
            public List<Store> getStores(PersistenceManager pm, String key, Object value, int limit) {
                assertEquals(Store.LOCATION_KEY, key);
                assertEquals(locationKey, (Long) value);
                Store store = new Store();
                store.setKey(storeKey);
                store.setLocationKey(locationKey);
                List<Store> stores = new ArrayList<Store>();
                stores.add(store);
                return stores;
            }
        };
        RobotResponder.demandOperations = new DemandOperations() {
            @Override
            public Demand getDemand(PersistenceManager pm, Long key, Long cKey) {
                assertEquals(demandKey, key);
                assertNull(cKey);
                Demand demand = new Demand();
                demand.setOwnerKey(consumerKey);
                demand.setSource(Source.simulated);
                demand.setState(State.invalid);
                return demand;
            }
        };
        RobotResponder.consumerOperations = new ConsumerOperations() {
            @Override
            public Consumer getConsumer(PersistenceManager pm, Long key) {
                assertEquals(consumerKey, key);
                Consumer consumer = new Consumer();
                return consumer;
            }
        };

        RobotResponder.processDemand(demandKey);

        assertNull(BaseConnector.getLastCommunicationInSimulatedMode());
        assertTrue(RobotResponder._baseOperations.getPersistenceManager().isClosed());
    }

    @Test
    public void testProcessDemandIII() throws Exception {
        RobotResponder.retailerOperations = new RetailerOperations() {
            @Override
            public List<Retailer> getRetailers(PersistenceManager pm, String key, Object value, int limit) {
                Retailer retailer = new Retailer();
                retailer.setKey(retailerKey);
                retailer.setStoreKey(storeKey);
                List<Retailer> retailers = new ArrayList<Retailer>();
                retailers.add(retailer);
                return retailers;
            }
        };
        RobotResponder.locationOperations = new LocationOperations() {
            @Override
            public List<Location> getLocations(PersistenceManager pm, String postalCode, String countryCode) {
                assertEquals(RobotResponder.ROBOT_POSTAL_CODE, postalCode);
                assertEquals(RobotResponder.ROBOT_COUNTRY_CODE, countryCode);
                Location location = new Location();
                location.setKey(locationKey);
                location.setPostalCode(RobotResponder.ROBOT_POSTAL_CODE);
                location.setCountryCode(RobotResponder.ROBOT_COUNTRY_CODE);
                List<Location> locations = new ArrayList<Location>();
                locations.add(location);
                return locations;
            }
        };
        RobotResponder.storeOperations = new StoreOperations() {
            @Override
            public List<Store> getStores(PersistenceManager pm, String key, Object value, int limit) {
                assertEquals(Store.LOCATION_KEY, key);
                assertEquals(locationKey, (Long) value);
                Store store = new Store();
                store.setKey(storeKey);
                store.setLocationKey(locationKey);
                List<Store> stores = new ArrayList<Store>();
                stores.add(store);
                return stores;
            }
        };
        RobotResponder.demandOperations = new DemandOperations() {
            @Override
            public Demand getDemand(PersistenceManager pm, Long key, Long cKey) {
                assertEquals(demandKey, key);
                assertNull(cKey);
                Demand demand = new Demand();
                demand.setOwnerKey(consumerKey);
                demand.setSource(Source.simulated);
                demand.setState(State.published);
                return demand;
            }
        };
        RobotResponder.consumerOperations = new ConsumerOperations() {
            @Override
            public Consumer getConsumer(PersistenceManager pm, Long key) {
                assertEquals(consumerKey, key);
                Consumer consumer = new Consumer();
                return consumer;
            }
        };
        RobotResponder.proposalOperations = new ProposalOperations() {
            @Override
            public Proposal createProposal(PersistenceManager pm, Proposal proposal) {
                assertEquals(demandKey, proposal.getDemandKey());
                assertEquals(storeKey, proposal.getStoreKey());
                StringBuilder message = new StringBuilder();
                for (String tag : proposal.getCriteria()) {
                    message.append(tag).append(" ");
                }
                assertEquals(LabelExtractor.get("rr_automated_response", Locale.ENGLISH).trim(), message.toString().trim());
                proposal.setKey(proposalKey);
                return proposal;
            }
        };

        MockAppEngineEnvironment appEnv = new MockAppEngineEnvironment();
        appEnv.setUp();
        RobotResponder.processDemand(demandKey);
        appEnv.tearDown();

        assertNull(BaseConnector.getLastCommunicationInSimulatedMode());
        assertTrue(RobotResponder._baseOperations.getPersistenceManager().isClosed());
    }
}