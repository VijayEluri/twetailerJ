package twetailer.task.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.jdo.PersistenceManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import domderrien.i18n.LabelExtractor;
import domderrien.jsontools.GenericJsonObject;
import domderrien.jsontools.JsonObject;

import twetailer.DataSourceException;
import twetailer.connector.BaseConnector;
import twetailer.connector.BaseConnector.Source;
import twetailer.dao.DemandOperations;
import twetailer.dao.LocationOperations;
import twetailer.dao.MockAppEngineEnvironment;
import twetailer.dao.MockBaseOperations;
import twetailer.dao.MockPersistenceManager;
import twetailer.dao.ProposalOperations;
import twetailer.dao.SaleAssociateOperations;
import twetailer.dto.Command;
import twetailer.dto.Consumer;
import twetailer.dto.Demand;
import twetailer.dto.Location;
import twetailer.dto.Proposal;
import twetailer.dto.RawCommand;
import twetailer.dto.SaleAssociate;
import twetailer.task.CommandLineParser;
import twetailer.task.CommandProcessor;
import twetailer.task.RobotResponder;
import twetailer.task.TestCommandProcessor;
import twetailer.validator.CommandSettings.Action;
import twetailer.validator.CommandSettings.State;

public class TestDemandCommandProcessor {

    @Before
    public void setUp() throws Exception {
        new TestCommandProcessor().setUp();
    }

    @After
    public void tearDown() throws Exception {
        new TestCommandProcessor().tearDown();
    }

    @Test
    public void testConstructor() {
        new DemandCommandProcessor();
    }

    @Test
    public void testProcessExisitingDemandIa() throws Exception {
        final Long demandKey = 2222L;
        final Long locationKey = 3333L;

        // DemandOperations mock
        final DemandOperations demandOperations = new DemandOperations() {
            @Override
            public Demand getDemand(PersistenceManager pm, Long key, Long consumerKey) {
                assertEquals(demandKey, key);
                Demand demand = new Demand();
                // demand.setState(State.opened); // Default state
                demand.setKey(demandKey);
                return demand;
            }
            @Override
            public Demand updateDemand(PersistenceManager pm, Demand demand) {
                assertEquals(demandKey, demand.getKey());
                assertEquals(State.opened, demand.getState());
                return demand;
            }
        };
        // LocationOperations mock
        final LocationOperations locationOperations = new LocationOperations() {
            @Override
            public Location createLocation(PersistenceManager pm, JsonObject command) {
                Location location = new Location();
                location.setKey(locationKey);
                return location;
            }
        };
        // CommandProcessor mock
        CommandProcessor._baseOperations = new MockBaseOperations();
        CommandProcessor.demandOperations = demandOperations;
        CommandProcessor.locationOperations = locationOperations;

        // Command mock
        JsonObject command = new GenericJsonObject();
        command.put(Demand.REFERENCE, demandKey);
        command.put(Location.POSTAL_CODE, "zzz");

        // RawCommand mock
        RawCommand rawCommand = new RawCommand(Source.simulated);

        // App Engine Environment mock
        MockAppEngineEnvironment appEnv = new MockAppEngineEnvironment();

        appEnv.setUp();
        DemandCommandProcessor.processDemandCommand(new MockPersistenceManager(), new Consumer(), rawCommand, command, CommandLineParser.localizedPrefixes.get(Locale.ENGLISH), CommandLineParser.localizedActions.get(Locale.ENGLISH));
        appEnv.tearDown();

        String sentText = BaseConnector.getLastCommunicationInSimulatedMode();
        assertNotNull(sentText);
        assertTrue(sentText.contains(demandKey.toString()));
    }

    @Test
    public void testProcessExisitingDemandIb() throws Exception {
        final Long demandKey = 2222L;
        final Long locationKey = 3333L;

        // DemandOperations mock
        final DemandOperations demandOperations = new DemandOperations() {
            @Override
            public Demand getDemand(PersistenceManager pm, Long key, Long consumerKey) {
                assertEquals(demandKey, key);
                Demand demand = new Demand();
                demand.setKey(demandKey);
                demand.setState(State.published);
                return demand;
            }
            @Override
            public Demand updateDemand(PersistenceManager pm, Demand demand) {
                assertEquals(demandKey, demand.getKey());
                assertEquals(State.opened, demand.getState());
                return demand;
            }
        };
        // LocationOperations mock
        final LocationOperations locationOperations = new LocationOperations() {
            @Override
            public Location createLocation(PersistenceManager pm, JsonObject command) {
                Location location = new Location();
                location.setKey(locationKey);
                return location;
            }
        };
        // CommandProcessor mock
        CommandProcessor._baseOperations = new MockBaseOperations();
        CommandProcessor.demandOperations = demandOperations;
        CommandProcessor.locationOperations = locationOperations;

        // Command mock
        JsonObject command = new GenericJsonObject();
        command.put(Demand.REFERENCE, demandKey);
        command.put(Location.POSTAL_CODE, "zzz");

        // RawCommand mock
        RawCommand rawCommand = new RawCommand(Source.simulated);

        // App Engine Environment mock
        MockAppEngineEnvironment appEnv = new MockAppEngineEnvironment();

        appEnv.setUp();
        DemandCommandProcessor.processDemandCommand(new MockPersistenceManager(), new Consumer(), rawCommand, command, CommandLineParser.localizedPrefixes.get(Locale.ENGLISH), CommandLineParser.localizedActions.get(Locale.ENGLISH));
        appEnv.tearDown();

        String sentText = BaseConnector.getLastCommunicationInSimulatedMode();
        assertNotNull(sentText);
        assertTrue(sentText.contains(demandKey.toString()));
    }

    @Test
    public void testProcessExisitingDemandIc() throws Exception {
        final Long demandKey = 2222L;
        final Long locationKey = 3333L;

        // DemandOperations mock
        final DemandOperations demandOperations = new DemandOperations() {
            @Override
            public Demand getDemand(PersistenceManager pm, Long key, Long consumerKey) {
                assertEquals(demandKey, key);
                Demand demand = new Demand();
                demand.setKey(demandKey);
                demand.setState(State.invalid);
                return demand;
            }
            @Override
            public Demand updateDemand(PersistenceManager pm, Demand demand) {
                assertEquals(demandKey, demand.getKey());
                assertEquals(State.opened, demand.getState());
                return demand;
            }
        };
        // LocationOperations mock
        final LocationOperations locationOperations = new LocationOperations() {
            @Override
            public Location createLocation(PersistenceManager pm, JsonObject command) {
                Location location = new Location();
                location.setKey(locationKey);
                return location;
            }
        };
        // CommandProcessor mock
        CommandProcessor._baseOperations = new MockBaseOperations();
        CommandProcessor.demandOperations = demandOperations;
        CommandProcessor.locationOperations = locationOperations;

        // Command mock
        JsonObject command = new GenericJsonObject();
        command.put(Demand.REFERENCE, demandKey);
        command.put(Location.POSTAL_CODE, "zzz");

        // RawCommand mock
        RawCommand rawCommand = new RawCommand(Source.simulated);

        // App Engine Environment mock
        MockAppEngineEnvironment appEnv = new MockAppEngineEnvironment();

        appEnv.setUp();
        DemandCommandProcessor.processDemandCommand(new MockPersistenceManager(), new Consumer(), rawCommand, command, CommandLineParser.localizedPrefixes.get(Locale.ENGLISH), CommandLineParser.localizedActions.get(Locale.ENGLISH));
        appEnv.tearDown();

        String sentText = BaseConnector.getLastCommunicationInSimulatedMode();
        assertNotNull(sentText);
        assertTrue(sentText.contains(demandKey.toString()));
    }

    @Test
    public void testProcessExisitingDemandId() throws Exception {
        final Long demandKey = 2222L;

        // DemandOperations mock
        final DemandOperations demandOperations = new DemandOperations() {
            @Override
            public Demand getDemand(PersistenceManager pm, Long key, Long consumerKey) {
                assertEquals(demandKey, key);
                Demand demand = new Demand();
                demand.setKey(demandKey);
                demand.setState(State.confirmed); // Too late to update it
                return demand;
            }
        };
        // CommandProcessor mock
        CommandProcessor._baseOperations = new MockBaseOperations();
        CommandProcessor.demandOperations = demandOperations;

        // Command mock
        JsonObject command = new GenericJsonObject();
        command.put(Demand.REFERENCE, demandKey);
        command.put(Demand.QUANTITY, 123L);

        // RawCommand mock
        RawCommand rawCommand = new RawCommand(Source.simulated);

        // App Engine Environment mock
        MockAppEngineEnvironment appEnv = new MockAppEngineEnvironment();

        appEnv.setUp();
        DemandCommandProcessor.processDemandCommand(new MockPersistenceManager(), new Consumer(), rawCommand, command, CommandLineParser.localizedPrefixes.get(Locale.ENGLISH), CommandLineParser.localizedActions.get(Locale.ENGLISH));
        appEnv.tearDown();

        String sentText = BaseConnector.getLastCommunicationInSimulatedMode();
        assertNotNull(sentText);
        assertEquals(LabelExtractor.get("cp_command_demand_non_modifiable_state", new Object[] { demandKey, State.confirmed.toString()}, Locale.ENGLISH), sentText);
    }

    @Test
    public void testProcessExisitingDemandII() throws Exception {
        final Long demandKey = 2222L;
        final Long locationKey = 3333L;

        // DemandOperations mock
        final DemandOperations demandOperations = new DemandOperations() {
            @Override
            public Demand getDemand(PersistenceManager pm, Long key, Long consumerKey) {
                assertEquals(demandKey, key);
                Demand demand = new Demand();
                demand.setKey(demandKey);
                return demand;
            }
            @Override
            public Demand updateDemand(PersistenceManager pm, Demand demand) {
                assertEquals(demandKey, demand.getKey());
                assertEquals(State.opened, demand.getState());
                return demand;
            }
        };
        // LocationOperations mock
        final LocationOperations locationOperations = new LocationOperations() {
            @Override
            public Location createLocation(PersistenceManager pm, JsonObject command) {
                throw new IllegalArgumentException("Done in purpose");
            }
            @Override
            public Location getLocation(PersistenceManager pm, Long key) {
                Location location = new Location();
                location.setKey(locationKey);
                return location;
            }
        };
        // CommandProcessor mock
        CommandProcessor._baseOperations = new MockBaseOperations();
        CommandProcessor.demandOperations = demandOperations;
        CommandProcessor.locationOperations = locationOperations;

        // Command mock
        JsonObject command = new GenericJsonObject();
        command.put(Demand.REFERENCE, demandKey);

        // RawCommand mock
        RawCommand rawCommand = new RawCommand(Source.simulated);

        // App Engine Environment mock
        MockAppEngineEnvironment appEnv = new MockAppEngineEnvironment();

        appEnv.setUp();
        DemandCommandProcessor.processDemandCommand(new MockPersistenceManager(), new Consumer(), rawCommand, command, CommandLineParser.localizedPrefixes.get(Locale.ENGLISH), CommandLineParser.localizedActions.get(Locale.ENGLISH));
        appEnv.tearDown();

        String sentText = BaseConnector.getLastCommunicationInSimulatedMode();
        assertNotNull(sentText);
        assertTrue(sentText.contains(demandKey.toString()));
    }

    @Test
    public void testProcessExisitingDemandIII() throws Exception {
        final Long demandKey = 2222L;

        // DemandOperations mock
        final DemandOperations demandOperations = new DemandOperations() {
            @Override
            public Demand getDemand(PersistenceManager pm, Long key, Long consumerKey) {
                assertEquals(demandKey, key);
                Demand demand = new Demand();
                demand.setKey(demandKey);
                return demand;
            }
            @Override
            public Demand updateDemand(PersistenceManager pm, Demand demand) {
                assertEquals(demandKey, demand.getKey());
                assertEquals(State.opened, demand.getState());
                return demand;
            }
        };
        // LocationOperations mock
        final LocationOperations locationOperations = new LocationOperations() {
            @Override
            public Location createLocation(PersistenceManager pm, JsonObject command) {
                throw new IllegalArgumentException("Done in purpose");
            }
            @Override
            public Location getLocation(PersistenceManager pm, Long key) throws DataSourceException {
                throw new DataSourceException("Done in purpose");
            }
        };
        // CommandProcessor mock
        CommandProcessor._baseOperations = new MockBaseOperations();
        CommandProcessor.demandOperations = demandOperations;
        CommandProcessor.locationOperations = locationOperations;

        // Command mock
        JsonObject command = new GenericJsonObject();
        command.put(Demand.REFERENCE, demandKey);

        // RawCommand mock
        RawCommand rawCommand = new RawCommand(Source.simulated);

        // App Engine Environment mock
        MockAppEngineEnvironment appEnv = new MockAppEngineEnvironment();

        appEnv.setUp();
        DemandCommandProcessor.processDemandCommand(new MockPersistenceManager(), new Consumer(), rawCommand, command, CommandLineParser.localizedPrefixes.get(Locale.ENGLISH), CommandLineParser.localizedActions.get(Locale.ENGLISH));
        appEnv.tearDown();

        String sentText = BaseConnector.getLastCommunicationInSimulatedMode();
        assertNotNull(sentText);
        assertTrue(sentText.contains(demandKey.toString()));
    }

    @Test
    public void testProcessFirstNewDemandI() throws Exception {
        final Long demandKey = 2222L;
        final Long locationKey = 3333L;

        // DemandOperations mock
        final DemandOperations demandOperations = new DemandOperations() {
            @Override
            public List<Demand> getDemands(PersistenceManager pm, String key, Object value, int limit) {
                return new ArrayList<Demand>();
            }
            @Override
            public Demand getDemand(PersistenceManager pm, Long key, Long consumerKey) {
                assertEquals(demandKey, key);
                Demand demand = new Demand();
                demand.setKey(demandKey);
                return demand;
            }
            @Override
            public Demand createDemand(PersistenceManager pm, Demand demand) {
                assertNull(demand.getKey());
                assertEquals(State.opened, demand.getState());
                demand.setKey(demandKey);
                return demand;
            }
        };
        // LocationOperations mock
        final LocationOperations locationOperations = new LocationOperations() {
            @Override
            public Location createLocation(PersistenceManager pm, JsonObject command) {
                Location location = new Location();
                location.setKey(locationKey);
                return location;
            }
        };
        // CommandProcessor mock
        CommandProcessor._baseOperations = new MockBaseOperations();
        CommandProcessor.demandOperations = demandOperations;
        CommandProcessor.locationOperations = locationOperations;

        // Command mock
        JsonObject command = new GenericJsonObject();

        // RawCommand mock
        RawCommand rawCommand = new RawCommand(Source.simulated);
        command.put(Location.POSTAL_CODE, "zzz");

        // App Engine Environment mock
        MockAppEngineEnvironment appEnv = new MockAppEngineEnvironment();

        appEnv.setUp();
        DemandCommandProcessor.processDemandCommand(new MockPersistenceManager(), new Consumer(), rawCommand, command, CommandLineParser.localizedPrefixes.get(Locale.ENGLISH), CommandLineParser.localizedActions.get(Locale.ENGLISH));
        appEnv.tearDown();

        String sentText = BaseConnector.getLastCommunicationInSimulatedMode();
        assertNotNull(sentText);
        assertTrue(sentText.contains(demandKey.toString()));
    }

    @Test
    public void testProcessFirstNewDemandII() throws Exception {
        final Long demandKey = 2222L;
        final Long locationKey = 3333L;

        // DemandOperations mock
        final DemandOperations demandOperations = new DemandOperations() {
            @Override
            public List<Demand> getDemands(PersistenceManager pm, String key, Object value, int limit) {
                return new ArrayList<Demand>();
            }
            @Override
            public Demand getDemand(PersistenceManager pm, Long key, Long consumerKey) {
                assertEquals(demandKey, key);
                Demand demand = new Demand();
                demand.setKey(demandKey);
                return demand;
            }
            @Override
            public Demand createDemand(PersistenceManager pm, Demand demand) {
                assertNull(demand.getKey());
                assertEquals(State.opened, demand.getState());
                demand.setKey(demandKey);
                return demand;
            }
        };
        // LocationOperations mock
        final LocationOperations locationOperations = new LocationOperations() {
            @Override
            public Location createLocation(PersistenceManager pm, JsonObject command) {
                throw new IllegalArgumentException("Done in purpose");
            }
            @Override
            public Location getLocation(PersistenceManager pm, Long key) {
                Location location = new Location();
                location.setKey(locationKey);
                return location;
            }
        };
        // CommandProcessor mock
        CommandProcessor._baseOperations = new MockBaseOperations();
        CommandProcessor.demandOperations = demandOperations;
        CommandProcessor.locationOperations = locationOperations;

        // Command mock
        JsonObject command = new GenericJsonObject();

        // RawCommand mock
        RawCommand rawCommand = new RawCommand(Source.simulated);

        // App Engine Environment mock
        MockAppEngineEnvironment appEnv = new MockAppEngineEnvironment();

        appEnv.setUp();
        DemandCommandProcessor.processDemandCommand(new MockPersistenceManager(), new Consumer(), rawCommand, command, CommandLineParser.localizedPrefixes.get(Locale.ENGLISH), CommandLineParser.localizedActions.get(Locale.ENGLISH));
        appEnv.tearDown();

        String sentText = BaseConnector.getLastCommunicationInSimulatedMode();
        assertNotNull(sentText);
        assertTrue(sentText.contains(demandKey.toString()));
    }

    @Test
    public void testProcessFirstNewDemandIII() throws Exception {
        final Long demandKey = 2222L;
        final Long locationKey = 3333L;

        // DemandOperations mock
        final DemandOperations demandOperations = new DemandOperations() {
            @Override
            public List<Demand> getDemands(PersistenceManager pm, String key, Object value, int limit) {
                return new ArrayList<Demand>();
            }
            @Override
            public Demand getDemand(PersistenceManager pm, Long key, Long consumerKey) {
                assertEquals(demandKey, key);
                Demand demand = new Demand();
                demand.setKey(demandKey);
                demand.setLocationKey(locationKey);
                return demand;
            }
            @Override
            public Demand createDemand(PersistenceManager pm, Demand demand) {
                assertNull(demand.getKey());
                assertEquals(State.opened, demand.getState());
                demand.setKey(demandKey);
                return demand;
            }
        };
        // LocationOperations mock
        final LocationOperations locationOperations = new LocationOperations() {
            @Override
            public Location createLocation(PersistenceManager pm, JsonObject command) {
                Location location = new Location();
                location.setKey(locationKey);
                return location;
            }
        };
        // CommandProcessor mock
        CommandProcessor._baseOperations = new MockBaseOperations();
        CommandProcessor.demandOperations = demandOperations;
        CommandProcessor.locationOperations = locationOperations;

        // Command mock
        JsonObject command = new GenericJsonObject();
        command.put(Demand.LOCATION_KEY, locationKey);

        // RawCommand mock
        RawCommand rawCommand = new RawCommand(Source.simulated);

        // App Engine Environment mock
        MockAppEngineEnvironment appEnv = new MockAppEngineEnvironment();

        appEnv.setUp();
        DemandCommandProcessor.processDemandCommand(new MockPersistenceManager(), new Consumer(), rawCommand, command, CommandLineParser.localizedPrefixes.get(Locale.ENGLISH), CommandLineParser.localizedActions.get(Locale.ENGLISH));
        appEnv.tearDown();

        String sentText = BaseConnector.getLastCommunicationInSimulatedMode();
        assertNotNull(sentText);
        assertTrue(sentText.contains(demandKey.toString()));
    }

    @Test
    public void testProcessAdditionalNewDemandI() throws Exception {
        final Long demandKey = 2222L;
        final Long locationKey = 3333L;

        // DemandOperations mock
        final DemandOperations demandOperations = new DemandOperations() {
            @Override
            public List<Demand> getDemands(PersistenceManager pm, String key, Object value, int limit) {
                Demand demand = new Demand();
                demand.setKey(demandKey);
                demand.setSource(Source.twitter); // Setup to verify it will be reset with the Source.simulated of the rawCommand
                List<Demand> demands = new ArrayList<Demand>();
                demands.add(demand);
                return demands;
            }
            @Override
            public Demand getDemand(PersistenceManager pm, Long key, Long consumerKey) {
                assertEquals(demandKey, key);
                Demand demand = new Demand();
                demand.setKey(demandKey);
                return demand;
            }
            @Override
            public Demand createDemand(PersistenceManager pm, Demand demand) {
                assertNull(demand.getKey());
                assertEquals(Source.simulated, demand.getSource()); // Verify the source attribute reset with the raw Command one
                assertEquals(State.opened, demand.getState());
                demand.setKey(demandKey);
                return demand;
            }
        };
        // LocationOperations mock
        final LocationOperations locationOperations = new LocationOperations() {
            @Override
            public Location createLocation(PersistenceManager pm, JsonObject command) {
                throw new IllegalArgumentException("Done in purpose");
            }
            @Override
            public Location getLocation(PersistenceManager pm, Long key) {
                Location location = new Location();
                location.setKey(locationKey);
                return location;
            }
        };
        // CommandProcessor mock
        CommandProcessor._baseOperations = new MockBaseOperations();
        CommandProcessor.demandOperations = demandOperations;
        CommandProcessor.locationOperations = locationOperations;

        // Command mock
        JsonObject command = new GenericJsonObject();

        // RawCommand mock
        RawCommand rawCommand = new RawCommand(Source.simulated);

        // App Engine Environment mock
        MockAppEngineEnvironment appEnv = new MockAppEngineEnvironment();

        appEnv.setUp();
        DemandCommandProcessor.processDemandCommand(new MockPersistenceManager(), new Consumer(), rawCommand, command, CommandLineParser.localizedPrefixes.get(Locale.ENGLISH), CommandLineParser.localizedActions.get(Locale.ENGLISH));
        appEnv.tearDown();

        String sentText = BaseConnector.getLastCommunicationInSimulatedMode();
        assertNotNull(sentText);
        assertTrue(sentText.contains(demandKey.toString()));
    }

    @Test
    public void testProcessAdditionalNewDemandII() throws Exception {
        final Long demandKey = 2222L;
        final Long locationKey = 3333L;

        // DemandOperations mock
        final DemandOperations demandOperations = new DemandOperations() {
            @Override
            public List<Demand> getDemands(PersistenceManager pm, String key, Object value, int limit) {
                Demand demand = new Demand();
                demand.setKey(demandKey);
                demand.setLocationKey(locationKey);
                demand.setSource(Source.twitter); // Setup to verify it will be reset with the Source.simulated of the rawCommand
                List<Demand> demands = new ArrayList<Demand>();
                demands.add(demand);
                return demands;
            }
            @Override
            public Demand getDemand(PersistenceManager pm, Long key, Long consumerKey) {
                assertEquals(demandKey, key);
                Demand demand = new Demand();
                demand.setKey(demandKey);
                return demand;
            }
            @Override
            public Demand createDemand(PersistenceManager pm, Demand demand) {
                assertNull(demand.getKey());
                assertEquals(Source.simulated, demand.getSource()); // Verify the source attribute reset with the raw Command one
                assertEquals(State.opened, demand.getState());
                demand.setKey(demandKey);
                return demand;
            }
        };
        // LocationOperations mock
        final LocationOperations locationOperations = new LocationOperations() {
            @Override
            public Location createLocation(PersistenceManager pm, JsonObject command) {
                throw new IllegalArgumentException("Done in purpose");
            }
            @Override
            public Location getLocation(PersistenceManager pm, Long key) {
                Location location = new Location();
                location.setKey(locationKey);
                return location;
            }
        };
        // CommandProcessor mock
        CommandProcessor._baseOperations = new MockBaseOperations();
        CommandProcessor.demandOperations = demandOperations;
        CommandProcessor.locationOperations = locationOperations;

        // Command mock
        JsonObject command = new GenericJsonObject();

        // RawCommand mock
        RawCommand rawCommand = new RawCommand(Source.simulated);

        // App Engine Environment mock
        MockAppEngineEnvironment appEnv = new MockAppEngineEnvironment();

        appEnv.setUp();
        DemandCommandProcessor.processDemandCommand(new MockPersistenceManager(), new Consumer(), rawCommand, command, CommandLineParser.localizedPrefixes.get(Locale.ENGLISH), CommandLineParser.localizedActions.get(Locale.ENGLISH));
        appEnv.tearDown();

        String sentText = BaseConnector.getLastCommunicationInSimulatedMode();
        assertNotNull(sentText);
        assertTrue(sentText.contains(demandKey.toString()));
    }

    @Test
    public void testProcessAdditionalNewDemandIII() throws Exception {
        final Long demandKey = 2222L;
        final Long locationKey = 3333L;

        // DemandOperations mock
        final DemandOperations demandOperations = new DemandOperations() {
            @Override
            public List<Demand> getDemands(PersistenceManager pm, String key, Object value, int limit) {
                Demand demand = new Demand();
                demand.setKey(demandKey);
                demand.setLocationKey(locationKey);
                demand.setSource(Source.twitter); // Setup to verify it will be reset with the Source.simulated of the rawCommand
                List<Demand> demands = new ArrayList<Demand>();
                demands.add(demand);
                return demands;
            }
            @Override
            public Demand getDemand(PersistenceManager pm, Long key, Long consumerKey) {
                assertEquals(demandKey, key);
                Demand demand = new Demand();
                demand.setKey(demandKey);
                return demand;
            }
            @Override
            public Demand createDemand(PersistenceManager pm, Demand demand) {
                assertNull(demand.getKey());
                assertEquals(Source.simulated, demand.getSource()); // Verify the source attribute reset with the raw Command one
                assertEquals(State.opened, demand.getState());
                demand.setKey(demandKey);
                return demand;
            }
        };
        // LocationOperations mock
        final LocationOperations locationOperations = new LocationOperations() {
            @Override
            public Location createLocation(PersistenceManager pm, JsonObject command) {
                throw new IllegalArgumentException("Done in purpose");
            }
            @Override
            public Location getLocation(PersistenceManager pm, Long key) {
                Location location = new Location();
                location.setKey(locationKey);
                return location;
            }
        };
        // CommandProcessor mock
        CommandProcessor._baseOperations = new MockBaseOperations();
        CommandProcessor.demandOperations = demandOperations;
        CommandProcessor.locationOperations = locationOperations;

        // Command mock
        JsonObject command = new GenericJsonObject();

        // RawCommand mock
        RawCommand rawCommand = new RawCommand(Source.simulated);

        // Consumer mock
        Consumer consumer = new Consumer();
        consumer.setLocationKey(locationKey);

        // App Engine Environment mock
        MockAppEngineEnvironment appEnv = new MockAppEngineEnvironment();

        appEnv.setUp();
        DemandCommandProcessor.processDemandCommand(new MockPersistenceManager(), consumer, rawCommand, command, CommandLineParser.localizedPrefixes.get(Locale.ENGLISH), CommandLineParser.localizedActions.get(Locale.ENGLISH));
        appEnv.tearDown();

        String sentText = BaseConnector.getLastCommunicationInSimulatedMode();
        assertNotNull(sentText);
        assertTrue(sentText.contains(demandKey.toString()));
    }

    @Test
    public void testProcessExisitingDemandIdWithHashTag() throws Exception {
        final Long demandKey = 2222L;
        final Long locationKey = 3333L;

        // DemandOperations mock
        final DemandOperations demandOperations = new DemandOperations() {
            @Override
            public Demand getDemand(PersistenceManager pm, Long key, Long consumerKey) {
                assertEquals(demandKey, key);
                Demand demand = new Demand();
                // demand.setState(State.opened); // Default state
                demand.setKey(demandKey);
                return demand;
            }
            @Override
            public Demand updateDemand(PersistenceManager pm, Demand demand) {
                assertEquals(demandKey, demand.getKey());
                assertEquals(State.opened, demand.getState());
                return demand;
            }
        };
        // LocationOperations mock
        final LocationOperations locationOperations = new LocationOperations() {
            @Override
            public Location createLocation(PersistenceManager pm, JsonObject command) {
                Location location = new Location();
                location.setKey(locationKey);
                return location;
            }
        };
        // CommandProcessor mock
        CommandProcessor._baseOperations = new MockBaseOperations();
        CommandProcessor.demandOperations = demandOperations;
        CommandProcessor.locationOperations = locationOperations;

        // Command mock
        JsonObject command = new GenericJsonObject();
        command.put(Demand.REFERENCE, demandKey);
        command.put(Demand.QUANTITY, 123L);
        command.put(Command.HASH_TAG, "test");

        // RawCommand mock
        RawCommand rawCommand = new RawCommand(Source.simulated);

        // App Engine Environment mock
        MockAppEngineEnvironment appEnv = new MockAppEngineEnvironment();

        appEnv.setUp();
        DemandCommandProcessor.processDemandCommand(new MockPersistenceManager(), new Consumer(), rawCommand, command, CommandLineParser.localizedPrefixes.get(Locale.ENGLISH), CommandLineParser.localizedActions.get(Locale.ENGLISH));
        appEnv.tearDown();

        String sentText = BaseConnector.getCommunicationForRetroIndexInSimulatedMode(1);
        assertNotNull(sentText);
        assertTrue(sentText.contains(demandKey.toString()));
        sentText = BaseConnector.getCommunicationForRetroIndexInSimulatedMode(0);
        assertNotNull(sentText);
        assertEquals(LabelExtractor.get("cp_command_demand_hashtag_warning", new Object[] { demandKey, "test" }, Locale.ENGLISH), sentText);
    }

    @Test
    public void testProcessExisitingProposalIdWithHashTag() throws Exception {
        final Long consumerKey = 3333L;
        final Long proposalKey = 5555L;
        final Long saleAssociateKey =  6666L;
        final Long storeKey = 7777L;

        // ProposalOperations mock
        final ProposalOperations proposalOperations = new ProposalOperations() {
            @Override
            public Proposal getProposal(PersistenceManager pm, Long key, Long cKey, Long sKey) {
                assertEquals(proposalKey, key);
                Proposal proposal = new Proposal();
                // proposal.setState(State.opened); // Default state
                proposal.setKey(proposalKey);
                return proposal;
            }
            @Override
            public Proposal updateProposal(PersistenceManager pm, Proposal proposal) {
                assertEquals(proposalKey, proposal.getKey());
                assertEquals(State.opened, proposal.getState());
                return proposal;
            }
        };
        // SaleAssociateOperations mock
        final SaleAssociateOperations saleAssociateOperations = new SaleAssociateOperations() {
            @Override
            public List<SaleAssociate> getSaleAssociates(PersistenceManager pm, String key, Object value, int limit) {
                assertEquals(key, SaleAssociate.CONSUMER_KEY);
                assertEquals(consumerKey, (Long) value);
                SaleAssociate saleAssociate = new SaleAssociate();
                saleAssociate.setKey(saleAssociateKey);
                saleAssociate.setConsumerKey(consumerKey);
                saleAssociate.setStoreKey(storeKey);
                List<SaleAssociate> saleAssociates = new ArrayList<SaleAssociate>();
                saleAssociates.add(saleAssociate);
                return saleAssociates;
            }
        };
        // CommandProcessor mock
        CommandProcessor._baseOperations = new MockBaseOperations();
        CommandProcessor.proposalOperations = proposalOperations;
        CommandProcessor.saleAssociateOperations = saleAssociateOperations;

        // Command mock
        JsonObject command = new GenericJsonObject();
        command.put(Command.ACTION, Action.propose.toString());
        command.put(Proposal.PROPOSAL_KEY, proposalKey);
        command.put(Proposal.PRICE, 24.99D);
        command.put(Command.HASH_TAG, "test");

        // RawCommand mock
        RawCommand rawCommand = new RawCommand(Source.simulated);

        // Consumer mock
        Consumer consumer = new Consumer();
        consumer.setKey(consumerKey);

        // App Engine Environment mock
        MockAppEngineEnvironment appEnv = new MockAppEngineEnvironment();

        appEnv.setUp();
        CommandProcessor.processCommand(new MockPersistenceManager(), consumer, rawCommand, command);
        appEnv.tearDown();

        String sentText = BaseConnector.getCommunicationForRetroIndexInSimulatedMode(1);
        assertNotNull(sentText);
        assertTrue(sentText.contains(proposalKey.toString()));
        sentText = BaseConnector.getCommunicationForRetroIndexInSimulatedMode(0);
        assertNotNull(sentText);
        assertEquals(LabelExtractor.get("cp_command_proposal_hashtag_warning", new Object[] { proposalKey, "test" }, Locale.ENGLISH), sentText);
    }

    @Test
    public void testProcessCommandDemandI() throws Exception {
        final Long consumerKey = 3333L;
        final Long locationKey = 4444L;
        final Long demandKey = 5555L;

        // LocationOperations mock
        final LocationOperations locationOperations = new LocationOperations() {
            @Override
            public Location createLocation(PersistenceManager pm, JsonObject parameters) {
                Location location = new Location();
                location.setKey(locationKey);
                return location;
            }
        };
        // DemandOperations mock
        final DemandOperations demandOperations = new DemandOperations() {
            @Override
            public List<Demand> getDemands(PersistenceManager pm, String key, Object value, int limit) {
                List<Demand> demands = new ArrayList<Demand>();
                return demands;
            }
            @Override
            public Demand createDemand(PersistenceManager pm, JsonObject parameters, Long consumerKey) {
                Demand demand = new Demand();
                demand.setKey(demandKey);
                return demand;
            }
        };
        // CommandProcessor mock
        CommandProcessor._baseOperations = new MockBaseOperations();
        CommandProcessor.demandOperations = demandOperations;
        CommandProcessor.locationOperations = locationOperations;

        // Command mock
        JsonObject command = new GenericJsonObject();
        command.put(Command.ACTION, Action.demand.toString());
        command.put(Location.POSTAL_CODE, RobotResponder.ROBOT_POSTAL_CODE);
        command.put(Location.COUNTRY_CODE, RobotResponder.ROBOT_COUNTRY_CODE);

        // RawCommand mock
        RawCommand rawCommand = new RawCommand(Source.simulated);

        // Consumer mock
        Consumer consumer = new Consumer();
        consumer.setKey(consumerKey);
        consumer.setLocationKey(locationKey);

        // App Engine Environment mock
        MockAppEngineEnvironment appEnv = new MockAppEngineEnvironment();

        appEnv.setUp();
        CommandProcessor.processCommand(new MockPersistenceManager(), consumer, rawCommand, command);
        appEnv.tearDown();

        String sentText = BaseConnector.getLastCommunicationInSimulatedMode();
        assertNotNull(sentText);
        assertTrue(sentText.contains(demandKey.toString()));
    }

    @Test
    public void testProcessCommandDemandII() throws Exception {
        final Long consumerKey = 3333L;
        final Long locationKey = 4444L;
        final Long demandKey = 5555L;

        // LocationOperations mock
        final LocationOperations locationOperations = new LocationOperations() {
            @Override
            public Location createLocation(PersistenceManager pm, JsonObject parameters) {
                Location location = new Location();
                location.setKey(locationKey);
                return location;
            }
        };
        // DemandOperations mock
        final DemandOperations demandOperations = new DemandOperations() {
            @Override
            public Demand getDemand(PersistenceManager pm, Long demandKey, Long consumerKey) {
                throw new RuntimeException("Done in purpose");
            }
        };
        // CommandProcessor mock
        CommandProcessor._baseOperations = new MockBaseOperations();
        CommandProcessor.demandOperations = demandOperations;
        CommandProcessor.locationOperations = locationOperations;

        // Command mock
        JsonObject command = new GenericJsonObject();
        command.put(Command.ACTION, Action.demand.toString());
        command.put(Demand.REFERENCE, demandKey);
        command.put(Location.POSTAL_CODE, RobotResponder.ROBOT_POSTAL_CODE);
        command.put(Location.COUNTRY_CODE, RobotResponder.ROBOT_COUNTRY_CODE);

        // RawCommand mock
        RawCommand rawCommand = new RawCommand(Source.simulated);

        // Consumer mock
        Consumer consumer = new Consumer();
        consumer.setKey(consumerKey);
        consumer.setLocationKey(locationKey);

        // App Engine Environment mock
        MockAppEngineEnvironment appEnv = new MockAppEngineEnvironment();

        appEnv.setUp();
        CommandProcessor.processCommand(new MockPersistenceManager(), consumer, rawCommand, command);
        appEnv.tearDown();

        String sentText = BaseConnector.getLastCommunicationInSimulatedMode();
        assertNotNull(sentText);
        assertEquals(LabelExtractor.get("cp_command_demand_invalid_demand_id", Locale.ENGLISH), sentText);
    }
}