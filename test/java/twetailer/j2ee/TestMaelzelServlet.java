package twetailer.j2ee;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javamocks.util.logging.MockLogger;

import javax.jdo.PersistenceManager;
import javax.servlet.MockServletInputStream;
import javax.servlet.MockServletOutputStream;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.MockHttpServletRequest;
import javax.servlet.http.MockHttpServletResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import twetailer.ClientException;
import twetailer.InvalidIdentifierException;
import twetailer.connector.JabberConnector;
import twetailer.connector.MockTwitterConnector;
import twetailer.connector.TwitterConnector;
import twetailer.dao.DemandOperations;
import twetailer.dao.LocationOperations;
import twetailer.dao.MockBaseOperations;
import twetailer.dao.ProposalOperations;
import twetailer.dao.RawCommandOperations;
import twetailer.dao.SettingsOperations;
import twetailer.dto.Command;
import twetailer.dto.Consumer;
import twetailer.dto.Demand;
import twetailer.dto.Location;
import twetailer.dto.Proposal;
import twetailer.dto.RawCommand;
import twetailer.dto.Settings;
import twetailer.task.step.BaseSteps;
import twetailer.validator.LocaleValidator;
import twetailer.validator.CommandSettings.State;
import twitter4j.DirectMessage;
import twitter4j.MockDirectMessage;
import twitter4j.MockTwitter;
import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Twitter;
import twitter4j.TwitterException;

import com.dyuproject.openid.OpenIdUser;
import com.dyuproject.openid.YadisDiscovery;
import com.google.appengine.api.datastore.DatastoreTimeoutException;
import com.google.appengine.api.labs.taskqueue.MockQueue;
import com.google.appengine.api.xmpp.MockXMPPService;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;

import domderrien.i18n.LabelExtractor;
import domderrien.i18n.LabelExtractor.ResourceFileId;

public class TestMaelzelServlet {

    MaelzelServlet servlet;

    private static LocalServiceTestHelper  helper;

    @BeforeClass
    public static void setUpBeforeClass() {
        MaelzelServlet.setLogger(new MockLogger("test", null));
        helper = new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
    }

    @Before
    public void setUp() throws Exception {
        servlet = new MaelzelServlet();
        BaseSteps.resetOperationControllers(true);
        BaseSteps.setMockBaseOperations(new MockBaseOperations());
        helper.setUp();
    }

    @After
    public void tearDown() throws Exception {
        helper.tearDown();
        JabberConnector.injectMockXMPPService(null);
    }

    @Test
    public void testConstructor() {
        new MaelzelServlet();
    }

    @Test
    public void testDoGetI() throws IOException {
        HttpServletRequest mockRequest = new MockHttpServletRequest() {
            @Override
            public String getPathInfo() {
                return null;
            }
        };
        final MockServletOutputStream stream = new MockServletOutputStream();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse() {
            @Override
            public ServletOutputStream getOutputStream() {
                return stream;
            }
        };
        servlet.doGet(mockRequest, mockResponse);
        assertTrue(stream.contains("'success':true"));
    }

    @Test
    public void testDoGetII() throws IOException {
        HttpServletRequest mockRequest = new MockHttpServletRequest() {
            @Override
            public String getPathInfo() {
                return "";
            }
        };
        final MockServletOutputStream stream = new MockServletOutputStream();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse() {
            @Override
            public ServletOutputStream getOutputStream() {
                return stream;
            }
        };
        servlet.doGet(mockRequest, mockResponse);
        assertTrue(stream.contains("'success':true"));
    }

    @Test
    public void testDoGetIII() throws IOException {
        HttpServletRequest mockRequest = new MockHttpServletRequest() {
            @Override
            public String getPathInfo() {
                return "/zzz";
            }
            @Override
            public Map<String, ?> getParameterMap() {
                return new HashMap<String, Object>();
            }
        };
        final MockServletOutputStream stream = new MockServletOutputStream();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse() {
            @Override
            public ServletOutputStream getOutputStream() {
                return stream;
            }
        };
        servlet.doGet(mockRequest, mockResponse);
        assertTrue(stream.contains("'success':false"));
    }

    @Test
    @SuppressWarnings("serial")
    public void testDoGetLoadTweets() throws IOException {
        // Inject a mock Twitter account
        final Twitter mockTwitterAccount = new MockTwitter(TwitterConnector.TWETAILER_TWITTER_SCREEN_NAME) {
            @Override
            public ResponseList<DirectMessage> getDirectMessages(Paging paging) {
                return null;
            }
        };
        MockTwitterConnector.injectMockTwitterAccount(mockTwitterAccount);

        BaseSteps.setMockSettingsOperations(new SettingsOperations() {
            @Override
            public Settings getSettings(PersistenceManager pm) {
                return new Settings();
            }
            @Override
            public Settings updateSettings(PersistenceManager pm, Settings settings) {
                return settings;
            }
        });

        // Prepare mock servlet parameters
        HttpServletRequest mockRequest = new MockHttpServletRequest() {
            @Override
            public String getPathInfo() {
                return "/loadTweets";
            }
        };
        final MockServletOutputStream stream = new MockServletOutputStream();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse() {
            @Override
            public ServletOutputStream getOutputStream() {
                return stream;
            }
        };

        servlet.doGet(mockRequest, mockResponse);
        assertTrue(stream.contains(Settings.LAST_PROCESSED_DIRECT_MESSAGE_ID));
        assertTrue(stream.contains("1"));
        assertTrue(stream.contains("'success':true"));

        // Clean-up
        MockTwitterConnector.restoreTwitterConnector();
    }

    @Test
    public void testDoGetProcessCommand() throws IOException {
        // Inject RawCommandOperations mock
        final Long commandKey = 12345L;
        BaseSteps.setMockRawCommandOperations(new RawCommandOperations() {
            @Override
            public RawCommand getRawCommand(PersistenceManager pm, Long key) throws InvalidIdentifierException {
                assertEquals(commandKey, key);
                throw new InvalidIdentifierException("Done in purpose");
            }
        });
        BaseSteps.setMockSettingsOperations(new SettingsOperations() {
            @Override
            public Settings getSettings(PersistenceManager pm) {
                return new Settings();
            }
            @Override
            public Settings updateSettings(PersistenceManager pm, Settings settings) {
                return settings;
            }
        });

        // Prepare mock servlet parameters
        HttpServletRequest mockRequest = new MockHttpServletRequest() {
            @Override
            public String getPathInfo() {
                return "/processCommand";
            }
            @Override
            public String getParameter(String name) {
                assertEquals(Command.KEY, name);
                return commandKey.toString();
            }
            @Override
            public Map<String, ?> getParameterMap() {
                return new HashMap<String, Object>();
            }
        };
        final MockServletOutputStream stream = new MockServletOutputStream();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse() {
            @Override
            public ServletOutputStream getOutputStream() {
                return stream;
            }
        };

        servlet.doGet(mockRequest, mockResponse);
        assertTrue(stream.contains("'success':false"));

        // Clean-up
    }

    @Test
    public void testDoGetValidateLocation() throws Exception {
        final String postalCode = "H2N3C6";
        final String countryCode = "CA";
        final Long consumerKey = 111L;
        final Long rawCommandKey = 227L;
        final Double longitude = -73.3D;

        // Inject LocationOperations mock
        BaseSteps.setMockLocationOperations(new LocationOperations() {
            @Override
            public List<Location> getLocations(PersistenceManager pm, String postalCode, String countryCode) {
                Location location = new Location();
                location.setLongitude(longitude);
                List<Location> locations = new ArrayList<Location>();
                locations.add(location);
                return locations;
            }
        });

        // Prepare mock servlet parameters
        HttpServletRequest mockRequest = new MockHttpServletRequest() {
            @Override
            public String getPathInfo() {
                return "/validateLocation";
            }
            @Override
            public String getParameter(String name) {
                if (Location.POSTAL_CODE.equals(name)) {
                    return postalCode;
                }
                if (Location.COUNTRY_CODE.equals(name)) {
                    return countryCode;
                }
                if (Consumer.CONSUMER_KEY.equals(name)) {
                    return consumerKey.toString();
                }
                if (Command.KEY.equals(name)) {
                    return rawCommandKey.toString();
                }
                fail("Parameter query for " + name + " not expected");
                return null;
            }
        };
        final MockServletOutputStream stream = new MockServletOutputStream();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse() {
            @Override
            public ServletOutputStream getOutputStream() {
                return stream;
            }
        };

        servlet.doGet(mockRequest, mockResponse);
        assertTrue(stream.contains("'success':true"));

        // Clean-up
    }

    @Test
    public void testDoGetValidateOpenDemand() throws IOException {
        final Long demandKey= 12345L;
        // Inject DemandOperations mock
        BaseSteps.setMockDemandOperations(new DemandOperations() {
            @Override
            public Demand getDemand(PersistenceManager pm, Long key, Long cKey) {
                assertEquals(demandKey, key);
                Demand demand = new Demand();
                demand.setKey(demandKey);
                demand.setState(State.invalid);
                return demand;
            }
        });

        // Prepare mock servlet parameters
        HttpServletRequest mockRequest = new MockHttpServletRequest() {
            @Override
            public String getPathInfo() {
                return "/validateOpenDemand";
            }
            @Override
            public String getParameter(String name) {
                assertEquals(Demand.KEY, name);
                return demandKey.toString();
            }
        };
        final MockServletOutputStream stream = new MockServletOutputStream();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse() {
            @Override
            public ServletOutputStream getOutputStream() {
                return stream;
            }
        };

        servlet.doGet(mockRequest, mockResponse);
        assertTrue(stream.contains("'success':true"));

        // Clean-up
    }

    @Test
    public void testDoGetValidateOpenProposal() throws IOException {
        final Long proposalKey= 12345L;
        // Inject ProposalOperations mock
        BaseSteps.setMockProposalOperations(new ProposalOperations() {
            @Override
            public Proposal getProposal(PersistenceManager pm, Long key, Long cKey, Long sKey) {
                assertEquals(proposalKey, key);
                Proposal proposal = new Proposal();
                proposal.setKey(proposalKey);
                proposal.setState(State.invalid);
                return proposal;
            }
        });

        // Prepare mock servlet parameters
        HttpServletRequest mockRequest = new MockHttpServletRequest() {
            @Override
            public String getPathInfo() {
                return "/validateOpenProposal";
            }
            @Override
            public String getParameter(String name) {
                assertEquals(Proposal.KEY, name);
                return proposalKey.toString();
            }
        };
        final MockServletOutputStream stream = new MockServletOutputStream();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse() {
            @Override
            public ServletOutputStream getOutputStream() {
                return stream;
            }
        };

        servlet.doGet(mockRequest, mockResponse);
        assertTrue(stream.contains("'success':true"));

        // Clean-up
    }

    @Test
    public void testDoGetValidatePublishedDemand() throws IOException {
        final Long demandKey= 12345L;
        // Inject DemandOperations mock
        BaseSteps.setMockDemandOperations(new DemandOperations() {
            @Override
            public Demand getDemand(PersistenceManager pm, Long key, Long cKey) {
                assertEquals(demandKey, key);
                Demand demand = new Demand();
                demand.setKey(demandKey);
                demand.setState(State.invalid);
                return demand;
            }
        });

        // Prepare mock servlet parameters
        HttpServletRequest mockRequest = new MockHttpServletRequest() {
            @Override
            public String getPathInfo() {
                return "/processPublishedDemand";
            }
            @Override
            public String getParameter(String name) {
                if(Demand.KEY.equals(name)) {
                    return demandKey.toString();
                }
                if("cronJob".equals(name)) {
                    return "true";
                }
                fail("Call not expected for: " + name);
                return null;
            }
        };
        final MockServletOutputStream stream = new MockServletOutputStream();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse() {
            @Override
            public ServletOutputStream getOutputStream() {
                return stream;
            }
        };

        servlet.doGet(mockRequest, mockResponse);
        assertTrue(stream.contains("'success':true"));

        // Clean-up
    }

    @Test
    public void testDoGetValidatePublishedDemands() throws IOException {
        final Long demandKey= 12345L;
        // Inject DemandOperations mock
        BaseSteps.setMockDemandOperations(new DemandOperations() {
            @Override
            public List<Demand> getDemands(PersistenceManager pm, Map<String, Object> parameters, int limit) {
                return new ArrayList<Demand>();
            }
        });

        // Prepare mock servlet parameters
        HttpServletRequest mockRequest = new MockHttpServletRequest() {
            @Override
            public String getPathInfo() {
                return "/processPublishedDemands";
            }
            @Override
            public String getParameter(String name) {
                assertEquals(Demand.KEY, name);
                return demandKey.toString();
            }
        };
        final MockServletOutputStream stream = new MockServletOutputStream();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse() {
            @Override
            public ServletOutputStream getOutputStream() {
                return stream;
            }
        };

        servlet.doGet(mockRequest, mockResponse);
        assertTrue(stream.contains("'success':true"));

        // Clean-up
    }

    @Test
    public void testDoGetValidatePublishedProposal() throws IOException {
        final Long proposalKey= 12345L;
        // Inject ProposalOperations mock
        BaseSteps.setMockProposalOperations(new ProposalOperations() {
            @Override
            public Proposal getProposal(PersistenceManager pm, Long key, Long cKey, Long sKey) {
                assertEquals(proposalKey, key);
                Proposal proposal = new Proposal();
                proposal.setKey(proposalKey);
                proposal.setState(State.invalid);
                return proposal;
            }
        });

        // Prepare mock servlet parameters
        HttpServletRequest mockRequest = new MockHttpServletRequest() {
            @Override
            public String getPathInfo() {
                return "/processPublishedProposal";
            }
            @Override
            public String getParameter(String name) {
                assertEquals(Proposal.KEY, name);
                return proposalKey.toString();
            }
        };
        final MockServletOutputStream stream = new MockServletOutputStream();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse() {
            @Override
            public ServletOutputStream getOutputStream() {
                return stream;
            }
        };

        servlet.doGet(mockRequest, mockResponse);
        assertTrue(stream.contains("'success':true"));

        // Clean-up
    }

    @Test
    public void testDoGetProcessDemandForRobot() throws IOException {
        final Long proposalKey= 12345L;
        // Inject SaleAssociateOperations mock
        BaseSteps.setMockSettingsOperations(new SettingsOperations() {
            @Override
            public Settings getSettings(PersistenceManager pm) {
                return new Settings();
            }
        });

        // Prepare mock servlet parameters
        HttpServletRequest mockRequest = new MockHttpServletRequest() {
            @Override
            public String getPathInfo() {
                return "/processDemandForRobot";
            }
            @Override
            public String getParameter(String name) {
                assertEquals(Proposal.KEY, name);
                return proposalKey.toString();
            }
        };
        final MockServletOutputStream stream = new MockServletOutputStream();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse() {
            @Override
            public ServletOutputStream getOutputStream() {
                return stream;
            }
        };

        servlet.doGet(mockRequest, mockResponse);
        assertTrue(stream.contains("'success':true"));

        // Clean-up
    }

    @Test
    public void testDoGetProcessCommandWithDatastoreTimeoutExceptionI() throws IOException {
        // Prepare mock servlet parameters
        HttpServletRequest mockRequest = new MockHttpServletRequest() {
            @Override
            public String getPathInfo() {
                return "/processCommand";
            }
            @Override
            public String getParameter(String name) {
                return "12345";
            }
            @Override
            public Map<String, ?> getParameterMap() {
                return new HashMap<String, Object>();
            }
        };
        final MockServletOutputStream stream = new MockServletOutputStream();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse() {
            @Override
            public ServletOutputStream getOutputStream() {
                return stream;
            }
        };

        BaseSteps.setMockBaseOperations(new MockBaseOperations() {
            @Override
            public PersistenceManager getPersistenceManager() {
                throw new DatastoreTimeoutException("Done in purpose!");
            }
        });

        servlet.doGet(mockRequest, mockResponse);
        assertTrue(stream.contains("'success':false"));
    }

    @Test
    public void testDoGetProcessCommandWithDatastoreTimeoutExceptionII() throws IOException {
        // Prepare mock servlet parameters
        HttpServletRequest mockRequest = new MockHttpServletRequest() {
            @Override
            public String getPathInfo() {
                return "/processCommand";
            }
            @Override
            public String getParameter(String name) {
                throw new DatastoreTimeoutException("Done in purpose!");
            }
            @Override
            public Map<String, ?> getParameterMap() {
                return new HashMap<String, Object>();
            }
        };
        final MockServletOutputStream stream = new MockServletOutputStream();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse() {
            @Override
            public ServletOutputStream getOutputStream() {
                return stream;
            }
        };

        servlet.doGet(mockRequest, mockResponse);
        assertTrue(stream.contains("'success':false"));
    }

    @Test
    public void testDoGetProcessCommandWithDatastoreTimeoutExceptionIII() throws IOException {
        // Prepare mock servlet parameters
        HttpServletRequest mockRequest = new MockHttpServletRequest() {
            @Override
            public String getPathInfo() {
                return "/processCommand";
            }
            @Override
            public String getParameter(String name) {
                throw new DatastoreTimeoutException("Done in purpose!");
            }
            @Override
            public Map<String, ?> getParameterMap() {
                return new HashMap<String, Object>();
            }
        };
        final MockServletOutputStream stream = new MockServletOutputStream();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse() {
            @Override
            public ServletOutputStream getOutputStream() {
                return stream;
            }
        };

        CatchAllMailHandlerServlet.foolNextMessagePost(); // Will make CatchAllMailHandlerServlet.composeAndPostMailMessage() throwing a MessagingException!

        servlet.doGet(mockRequest, mockResponse);
        assertTrue(stream.contains("'success':false"));
    }

    @Test
    public void testDoPostI() throws IOException {
        HttpServletRequest mockRequest = new MockHttpServletRequest() {
            @Override
            public String getPathInfo() {
                return null;
            }
        };
        final MockServletOutputStream stream = new MockServletOutputStream();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse() {
            @Override
            public ServletOutputStream getOutputStream() {
                return stream;
            }
        };
        servlet.doPost(mockRequest, mockResponse);
        assertTrue(stream.contains("'success':true"));
    }

    @Test
    public void testDoPostII() throws IOException {
        HttpServletRequest mockRequest = new MockHttpServletRequest() {
            @Override
            public String getPathInfo() {
                return "";
            }
        };
        final MockServletOutputStream stream = new MockServletOutputStream();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse() {
            @Override
            public ServletOutputStream getOutputStream() {
                return stream;
            }
        };
        servlet.doPost(mockRequest, mockResponse);
        assertTrue(stream.contains("'success':true"));
    }

    @Test
    public void testDoPostIII() throws IOException {
        HttpServletRequest mockRequest = new MockHttpServletRequest() {
            @Override
            public String getPathInfo() {
                return "/zzz";
            }
        };
        final MockServletOutputStream stream = new MockServletOutputStream();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse() {
            @Override
            public ServletOutputStream getOutputStream() {
                return stream;
            }
        };
        servlet.doPost(mockRequest, mockResponse);
        assertTrue(stream.contains("'success':false"));
    }

    @Test
    public void testDoPostProcessCommandWithDatastoreTimeoutExceptionI() throws IOException {
        // Prepare mock servlet parameters
        HttpServletRequest mockRequest = new MockHttpServletRequest() {
            @Override
            public String getPathInfo() {
                return "/processVerificationCode";
            }
            @Override
            public String getParameter(String name) {
                return "12345";
            }
            @Override
            public ServletInputStream getInputStream() {
                throw new IllegalArgumentException("Done in purpose!");
            }
        };
        final MockServletOutputStream stream = new MockServletOutputStream();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse() {
            @Override
            public ServletOutputStream getOutputStream() {
                return stream;
            }
        };

        servlet.doPost(mockRequest, mockResponse);
        assertTrue(stream.contains("'success':false"));
    }

    @Test
    public void testDoPostProcessCommandWithDatastoreTimeoutExceptionII() throws IOException {
        // Prepare mock servlet parameters
        HttpServletRequest mockRequest = new MockHttpServletRequest() {
            @Override
            public String getPathInfo() {
                return "/processVerificationCode";
            }
            @Override
            public String getParameter(String name) {
                return "12345";
            }
            @Override
            public ServletInputStream getInputStream() {
                throw new IllegalArgumentException("Done in purpose!");
            }
        };
        final MockServletOutputStream stream = new MockServletOutputStream();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse() {
            @Override
            public ServletOutputStream getOutputStream() {
                return stream;
            }
        };

        CatchAllMailHandlerServlet.foolNextMessagePost(); // Will make CatchAllMailHandlerServlet.composeAndPostMailMessage() throwing a MessagingException!

        servlet.doPost(mockRequest, mockResponse);
        assertTrue(stream.contains("'success':false"));
    }

    @Test(expected=RuntimeException.class)
    public void testGetVerificationCodeI() throws IOException, ClientException {
        String topic = "zzz";
        String identifier = "unit@test.ca";
        String openId = "http://openId";

        MaelzelServlet.getCode(topic, identifier, openId);
    }

    @Test
    public void testGetVerificationCodeII() throws IOException, ClientException {
        String topic = Consumer.EMAIL;
        String identifier = "unit@test.ca";
        String openId = "http://openId";

        long code = MaelzelServlet.getCode(topic, identifier, openId);

        assertEquals(code, MaelzelServlet.getCode(topic, identifier, openId));
    }

    @Test
    public void testGetVerificationCodeIII() throws IOException, ClientException {
        String topic = Consumer.JABBER_ID;
        String identifier = "unit@test.ca";
        String openId = "http://openId";

        long code = MaelzelServlet.getCode(topic, identifier, openId);

        assertEquals(code, MaelzelServlet.getCode(topic, identifier, openId));
    }

    @Test
    public void testGetVerificationCodeIV() throws IOException, ClientException {
        String topic = Consumer.TWITTER_ID;
        String identifier = "unit_test_ca";
        String openId = "http://openId";

        long code = MaelzelServlet.getCode(topic, identifier, openId);

        assertEquals(code, MaelzelServlet.getCode(topic, identifier, openId));
    }

    @Test
    public void testGetVerificationCodeV() throws IOException, ClientException {
        String identifier1 = "unit@test.ca";
        String identifier2 = "unit_test_ca";
        String openId = "http://openId";

        long codeEmail = MaelzelServlet.getCode(Consumer.EMAIL, identifier1, openId);
        long codeJabber = MaelzelServlet.getCode(Consumer.JABBER_ID, identifier1, openId);
        long codeTwitter = MaelzelServlet.getCode(Consumer.TWITTER_ID, identifier2, openId);

        assertNotSame(codeEmail, codeJabber);
        assertNotSame(codeEmail, codeTwitter);
        assertNotSame(codeJabber, codeTwitter);
    }

    @Test
    public void testGetVerificationCodeVI() throws IOException, ClientException {
        String openId = "http://openId";

        long codeNull = MaelzelServlet.getCode("Not important", null, openId);
        long codeEmpty = MaelzelServlet.getCode("Not important", "", openId);

        assertNotSame(9999999999L, codeNull);
        assertNotSame(9999999999L, codeEmpty);
        assertNotSame(codeNull, codeEmpty);
    }

    @Test(expected=ClientException.class)
    public void testGetVerificationCodeVII() throws IOException, ClientException {
        String identifier = "invalid e-mail address";
        String openId = "http://openId";

        MaelzelServlet.getCode(Consumer.EMAIL, identifier, openId);
    }

    @Test(expected=ClientException.class)
    public void testGetVerificationCodeVIII() throws IOException, ClientException {
        String identifier = "@invalid +Twitter+ identifier (format)";
        String openId = "http://openId";

        MaelzelServlet.getCode(Consumer.TWITTER_ID, identifier, openId);
    }

    @Test
    public void testWaitForVerificationCodeI() throws IOException {
        final String openId = "http://openId";
        final String identifier = "unit@test.ca";
        final String topic = Consumer.EMAIL;

        HttpServletRequest mockRequest = new MockHttpServletRequest() {
            @Override
            public String getPathInfo() {
                return "/processVerificationCode";
            }
            @Override
            public ServletInputStream getInputStream() {
                return new MockServletInputStream(
                        "{'topic':'" + topic + "'" +
                        ",'waitForCode':" + Boolean.TRUE +
                        ",'" + Consumer.LANGUAGE + "':'" + LocaleValidator.DEFAULT_LANGUAGE + "'" +
                        ",'" + topic + "':'" + identifier + "'" +
                        "}"
                );
            }
            @Override
            public Object getAttribute(String name) {
                if (OpenIdUser.ATTR_NAME.equals(name)) {
                    OpenIdUser user = OpenIdUser.populate(
                            "http://www.yahoo.com",
                            YadisDiscovery.IDENTIFIER_SELECT,
                            LoginServlet.YAHOO_OPENID_SERVER_URL
                    );
                    Map<String, Object> json = new HashMap<String, Object>();
                    // {a: "claimId", b: "identity", c: "assocHandle", d: associationData, e: "openIdServer", f: "openIdDelegate", g: attributes, h: "identifier"}
                    json.put("a", openId);
                    user.fromJSON(json);
                    return user;
                }
                fail("Attribute access not expected for: " + name);
                return null;
            }
        };

        final MockServletOutputStream stream = new MockServletOutputStream();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse() {
            @Override
            public ServletOutputStream getOutputStream() {
                return stream;
            }
        };

        servlet.doPost(mockRequest, mockResponse);
        assertTrue(stream.contains("'success':true"));
        // No way to check what's send by e-mail...
        // See test done for topic == Twitter
    }

    @Test
    public void testWaitForVerificationCodeII() throws IOException, ClientException {
        final String openId = "http://openId";
        final String identifier = "unit@test.ca";
        final String topic = Consumer.JABBER_ID;

        HttpServletRequest mockRequest = new MockHttpServletRequest() {
            @Override
            public String getPathInfo() {
                return "/processVerificationCode";
            }
            @Override
            public ServletInputStream getInputStream() {
                return new MockServletInputStream(
                        "{'topic':'" + topic + "'" +
                        ",'waitForCode':" + Boolean.TRUE +
                        ",'" + Consumer.LANGUAGE + "':'" + LocaleValidator.DEFAULT_LANGUAGE + "'" +
                        ",'" + topic + "':'" + identifier + "'" +
                        "}"
                );
            }
            @Override
            public Object getAttribute(String name) {
                if (OpenIdUser.ATTR_NAME.equals(name)) {
                    OpenIdUser user = OpenIdUser.populate(
                            "http://www.yahoo.com",
                            YadisDiscovery.IDENTIFIER_SELECT,
                            LoginServlet.YAHOO_OPENID_SERVER_URL
                    );
                    Map<String, Object> json = new HashMap<String, Object>();
                    // {a: "claimId", b: "identity", c: "assocHandle", d: associationData, e: "openIdServer", f: "openIdDelegate", g: attributes, h: "identifier"}
                    json.put("a", openId);
                    user.fromJSON(json);
                    return user;
                }
                fail("Attribute access not expected for: " + name);
                return null;
            }
        };

        final MockServletOutputStream stream = new MockServletOutputStream();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse() {
            @Override
            public ServletOutputStream getOutputStream() {
                return stream;
            }
        };

        MockXMPPService mock = new MockXMPPService();
        mock.setPresence(identifier, true);
        JabberConnector.injectMockXMPPService(mock);

        servlet.doPost(mockRequest, mockResponse);
        assertTrue(stream.contains("'success':true"));
    }

    @Test
    @SuppressWarnings("serial")
    public void testWaitForVerificationCodeIII() throws IOException, TwitterException, ClientException {
        final String openId = "http://openId";
        final String identifier = "unit_test_ca";
        final String topic = Consumer.TWITTER_ID;

        HttpServletRequest mockRequest = new MockHttpServletRequest() {
            @Override
            public String getPathInfo() {
                return "/processVerificationCode";
            }
            @Override
            public ServletInputStream getInputStream() {
                return new MockServletInputStream(
                        "{'topic':'" + topic + "'" +
                        ",'waitForCode':" + Boolean.TRUE +
                        ",'" + Consumer.LANGUAGE + "':'" + LocaleValidator.DEFAULT_LANGUAGE + "'" +
                        ",'" + topic + "':'" + identifier + "'" +
                        "}"
                );
            }
            @Override
            public Object getAttribute(String name) {
                if (OpenIdUser.ATTR_NAME.equals(name)) {
                    OpenIdUser user = OpenIdUser.populate(
                            "http://www.yahoo.com",
                            YadisDiscovery.IDENTIFIER_SELECT,
                            LoginServlet.YAHOO_OPENID_SERVER_URL
                    );
                    Map<String, Object> json = new HashMap<String, Object>();
                    // {a: "claimId", b: "identity", c: "assocHandle", d: associationData, e: "openIdServer", f: "openIdDelegate", g: attributes, h: "identifier"}
                    json.put("a", openId);
                    user.fromJSON(json);
                    return user;
                }
                fail("Attribute access not expected for: " + name);
                return null;
            }
        };

        final MockServletOutputStream stream = new MockServletOutputStream();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse() {
            @Override
            public ServletOutputStream getOutputStream() {
                return stream;
            }
        };

        // Expected messages
        final long code = MaelzelServlet.getCode(topic, identifier, openId);
        final String msg1 = LabelExtractor.get(ResourceFileId.third, "consumer_info_verification_notification_title", Locale.ENGLISH);
        final String msg2 = LabelExtractor.get(ResourceFileId.third, "consumer_info_verification_notification_body", new Object[] { code }, Locale.ENGLISH);

        // To inject the mock account
        TwitterConnector.releaseTwetailerAccount(new MockTwitter(TwitterConnector.TWETAILER_TWITTER_SCREEN_NAME) {
            boolean firstMessage = true;
            @Override
            public DirectMessage sendDirectMessage(String screenName, String text) throws TwitterException {
                assertEquals(identifier, screenName);
                assertEquals(firstMessage ? msg1 : msg2, text);
                firstMessage = !firstMessage;
                return new MockDirectMessage(getScreenName(), screenName, text);
            }
        });

        servlet.doPost(mockRequest, mockResponse);
        assertTrue(stream.contains("'success':true"));

        TwitterConnector.getTwetailerAccount(); // To remove the injected TwitterAccount from the connector pool
    }

    @Test
    @SuppressWarnings("serial")
    public void testWaitForVerificationCodeIV() throws IOException, TwitterException, ClientException {
        final String openId = "http://openId";
        final String identifier = "unit_test_ca";
        final String topic = Consumer.TWITTER_ID;

        HttpServletRequest mockRequest = new MockHttpServletRequest() {
            @Override
            public String getPathInfo() {
                return "/processVerificationCode";
            }
            @Override
            public ServletInputStream getInputStream() {
                return new MockServletInputStream(
                        "{'topic':'" + topic + "'" +
                        ",'waitForCode':" + Boolean.TRUE +
                        ",'" + Consumer.LANGUAGE + "':'" + LocaleValidator.DEFAULT_LANGUAGE + "'" +
                        ",'" + topic + "':'" + identifier + "'" +
                        "}"
                );
            }
            @Override
            public Object getAttribute(String name) {
                if (OpenIdUser.ATTR_NAME.equals(name)) {
                    OpenIdUser user = OpenIdUser.populate(
                            "http://www.yahoo.com",
                            YadisDiscovery.IDENTIFIER_SELECT,
                            LoginServlet.YAHOO_OPENID_SERVER_URL
                    );
                    Map<String, Object> json = new HashMap<String, Object>();
                    // {a: "claimId", b: "identity", c: "assocHandle", d: associationData, e: "openIdServer", f: "openIdDelegate", g: attributes, h: "identifier"}
                    json.put("a", openId);
                    user.fromJSON(json);
                    return user;
                }
                fail("Attribute access not expected for: " + name);
                return null;
            }
        };

        final MockServletOutputStream stream = new MockServletOutputStream();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse() {
            @Override
            public ServletOutputStream getOutputStream() {
                return stream;
            }
        };

        // Expected messages
        final String msg1 = LabelExtractor.get(ResourceFileId.third, "consumer_info_verification_notification_title", Locale.ENGLISH);

        // To inject the mock account
        TwitterConnector.releaseTwetailerAccount(new MockTwitter(TwitterConnector.TWETAILER_TWITTER_SCREEN_NAME) {
            @Override
            public DirectMessage sendDirectMessage(String screenName, String text) throws TwitterException {
                assertEquals(identifier, screenName);
                assertEquals(msg1, text);
                throw new TwitterException("Done in purpose!");
            }
        });

        servlet.doPost(mockRequest, mockResponse);
        assertTrue(stream.contains("'success':false"));

        TwitterConnector.getTwetailerAccount(); // To remove the injected TwitterAccount from the connector pool
    }

    @Test
    @Ignore
    @SuppressWarnings("serial")
    public void testWaitForVerificationCodeV() throws IOException, TwitterException, ClientException {
        final String openId = "http://openId";
        final String identifier = "unit_test_ca";
        final String topic = Consumer.TWITTER_ID;

        HttpServletRequest mockRequest = new MockHttpServletRequest() {
            @Override
            public String getPathInfo() {
                return "/processVerificationCode";
            }
            @Override
            public ServletInputStream getInputStream() {
                return new MockServletInputStream(
                        "{'topic':'" + topic + "'" +
                        ",'waitForCode':" + Boolean.TRUE +
                        ",'" + Consumer.LANGUAGE + "':'" + LocaleValidator.DEFAULT_LANGUAGE + "'" +
                        ",'" + topic + "':'" + identifier + "'" +
                        "}"
                );
            }
            @Override
            public Object getAttribute(String name) {
                if (OpenIdUser.ATTR_NAME.equals(name)) {
                    OpenIdUser user = OpenIdUser.populate(
                            "http://www.yahoo.com",
                            YadisDiscovery.IDENTIFIER_SELECT,
                            LoginServlet.YAHOO_OPENID_SERVER_URL
                    );
                    Map<String, Object> json = new HashMap<String, Object>();
                    // {a: "claimId", b: "identity", c: "assocHandle", d: associationData, e: "openIdServer", f: "openIdDelegate", g: attributes, h: "identifier"}
                    json.put("a", openId);
                    user.fromJSON(json);
                    return user;
                }
                fail("Attribute access not expected for: " + name);
                return null;
            }
        };

        final MockServletOutputStream stream = new MockServletOutputStream();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse() {
            @Override
            public ServletOutputStream getOutputStream() {
                return stream;
            }
        };

        // Exception to inject
        /* FIXME: enable the piece of code again when TwitterException is not final, and remove the @Ignore instruction
        final TwitterException injectedException = new TwitterException("blah-blah-blah.") {
            @Override
            public int getStatusCode() {
                return 403;
            }
        };
        */

        // Expected messages
        final String msg1 = LabelExtractor.get(ResourceFileId.third, "consumer_info_verification_notification_title", Locale.ENGLISH);

        // To inject the mock account
        TwitterConnector.releaseTwetailerAccount(new MockTwitter(TwitterConnector.TWETAILER_TWITTER_SCREEN_NAME) {
            @Override
            public DirectMessage sendDirectMessage(String screenName, String text) throws TwitterException {
                assertEquals(identifier, screenName);
                assertEquals(msg1, text);
                /* FIXME: enable the piece of code again when TwitterException is not final, and remove the @Ignore instruction
                throw injectedException;
                */
                throw new NullPointerException();
            }
        });

        servlet.doPost(mockRequest, mockResponse);
        assertTrue(stream.contains("'success':false"));

        TwitterConnector.getTwetailerAccount(); // To remove the injected TwitterAccount from the connector pool
    }

    @Test
    @Ignore
    @SuppressWarnings("serial")
    public void testWaitForVerificationCodeVI() throws IOException, TwitterException, ClientException {
        final String openId = "http://openId";
        final String identifier = "unit_test_ca";
        final String topic = Consumer.TWITTER_ID;

        HttpServletRequest mockRequest = new MockHttpServletRequest() {
            @Override
            public String getPathInfo() {
                return "/processVerificationCode";
            }
            @Override
            public ServletInputStream getInputStream() {
                return new MockServletInputStream(
                        "{'topic':'" + topic + "'" +
                        ",'waitForCode':" + Boolean.TRUE +
                        ",'" + Consumer.LANGUAGE + "':'" + LocaleValidator.DEFAULT_LANGUAGE + "'" +
                        ",'" + topic + "':'" + identifier + "'" +
                        "}"
                );
            }
            @Override
            public Object getAttribute(String name) {
                if (OpenIdUser.ATTR_NAME.equals(name)) {
                    OpenIdUser user = OpenIdUser.populate(
                            "http://www.yahoo.com",
                            YadisDiscovery.IDENTIFIER_SELECT,
                            LoginServlet.YAHOO_OPENID_SERVER_URL
                    );
                    Map<String, Object> json = new HashMap<String, Object>();
                    // {a: "claimId", b: "identity", c: "assocHandle", d: associationData, e: "openIdServer", f: "openIdDelegate", g: attributes, h: "identifier"}
                    json.put("a", openId);
                    user.fromJSON(json);
                    return user;
                }
                fail("Attribute access not expected for: " + name);
                return null;
            }
        };

        final MockServletOutputStream stream = new MockServletOutputStream();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse() {
            @Override
            public ServletOutputStream getOutputStream() {
                return stream;
            }
        };

        // Exception to inject
        /* FIXME: enable the piece of code again when TwitterException is not final, and remove the @Ignore instruction
        final TwitterException injectedException = new TwitterException("blah-blah-blah. <error>You cannot send messages to users who are not following you.</error>") {
            @Override
            public int getStatusCode() {
                return 403;
            }
        };
        */

        // Expected messages
        final String msg1 = LabelExtractor.get(ResourceFileId.third, "consumer_info_verification_notification_title", Locale.ENGLISH);

        // To inject the mock account
        TwitterConnector.releaseTwetailerAccount(new MockTwitter(TwitterConnector.TWETAILER_TWITTER_SCREEN_NAME) {
            @Override
            public DirectMessage sendDirectMessage(String screenName, String text) throws TwitterException {
                assertEquals(identifier, screenName);
                assertEquals(msg1, text);
                /* FIXME: enable the piece of code again when TwitterException is not final, and remove the @Ignore instruction
                throw injectedException;
                */
                throw new NullPointerException();
            }
        });

        servlet.doPost(mockRequest, mockResponse);
        assertTrue(stream.contains("'success':false"));

        TwitterConnector.getTwetailerAccount(); // To remove the injected TwitterAccount from the connector pool
    }

    @Test
    public void testGoodVerificationCodeI() throws IOException, ClientException {
        final String openId = "http://openId";
        final String identifier = "unit@test.ca";
        final String topic = Consumer.EMAIL;
        final long code = MaelzelServlet.getCode(topic, identifier, openId);

        HttpServletRequest mockRequest = new MockHttpServletRequest() {
            @Override
            public String getPathInfo() {
                return "/processVerificationCode";
            }
            @Override
            public ServletInputStream getInputStream() {
                return new MockServletInputStream(
                        "{'topic':'" + topic + "'" +
                        ",'waitForCode':" + Boolean.FALSE +
                        ",'" + Consumer.LANGUAGE + "':'" + LocaleValidator.DEFAULT_LANGUAGE + "'" +
                        ",'" + topic + "':'" + identifier + "'" +
                        ",'" + topic + "Code':" + code +
                        "}"
                );
            }
            @Override
            public Object getAttribute(String name) {
                if (OpenIdUser.ATTR_NAME.equals(name)) {
                    OpenIdUser user = OpenIdUser.populate(
                            "http://www.yahoo.com",
                            YadisDiscovery.IDENTIFIER_SELECT,
                            LoginServlet.YAHOO_OPENID_SERVER_URL
                    );
                    Map<String, Object> json = new HashMap<String, Object>();
                    // {a: "claimId", b: "identity", c: "assocHandle", d: associationData, e: "openIdServer", f: "openIdDelegate", g: attributes, h: "identifier"}
                    json.put("a", openId);
                    user.fromJSON(json);
                    return user;
                }
                fail("Attribute access not expected for: " + name);
                return null;
            }
        };

        final MockServletOutputStream stream = new MockServletOutputStream();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse() {
            @Override
            public ServletOutputStream getOutputStream() {
                return stream;
            }
        };

        servlet.doPost(mockRequest, mockResponse);
        assertTrue(stream.contains("'success':true"));
        assertTrue(stream.contains("'codeValidity':true"));
    }

    @Test
    public void testGoodVerificationCodeII() throws IOException, ClientException {
        final String openId = "http://openId";
        final String identifier = "unit@test.ca";
        final String topic = Consumer.JABBER_ID;
        final long code = MaelzelServlet.getCode(topic, identifier, openId);

        HttpServletRequest mockRequest = new MockHttpServletRequest() {
            @Override
            public String getPathInfo() {
                return "/processVerificationCode";
            }
            @Override
            public ServletInputStream getInputStream() {
                return new MockServletInputStream(
                        "{'topic':'" + topic + "'" +
                        ",'waitForCode':" + Boolean.FALSE +
                        ",'" + Consumer.LANGUAGE + "':'" + LocaleValidator.DEFAULT_LANGUAGE + "'" +
                        ",'" + topic + "':'" + identifier + "'" +
                        ",'" + topic + "Code':" + code +
                        "}"
                );
            }
            @Override
            public Object getAttribute(String name) {
                if (OpenIdUser.ATTR_NAME.equals(name)) {
                    OpenIdUser user = OpenIdUser.populate(
                            "http://www.yahoo.com",
                            YadisDiscovery.IDENTIFIER_SELECT,
                            LoginServlet.YAHOO_OPENID_SERVER_URL
                    );
                    Map<String, Object> json = new HashMap<String, Object>();
                    // {a: "claimId", b: "identity", c: "assocHandle", d: associationData, e: "openIdServer", f: "openIdDelegate", g: attributes, h: "identifier"}
                    json.put("a", openId);
                    user.fromJSON(json);
                    return user;
                }
                fail("Attribute access not expected for: " + name);
                return null;
            }
        };

        final MockServletOutputStream stream = new MockServletOutputStream();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse() {
            @Override
            public ServletOutputStream getOutputStream() {
                return stream;
            }
        };

        servlet.doPost(mockRequest, mockResponse);
        assertTrue(stream.contains("'success':true"));
        assertTrue(stream.contains("'codeValidity':true"));
    }

    @Test
    public void testGoodVerificationCodeIII() throws IOException, ClientException {
        final String openId = "http://openId";
        final String identifier = "unit_test_ca";
        final String topic = Consumer.TWITTER_ID;
        final long code = MaelzelServlet.getCode(topic, identifier, openId);

        HttpServletRequest mockRequest = new MockHttpServletRequest() {
            @Override
            public String getPathInfo() {
                return "/processVerificationCode";
            }
            @Override
            public ServletInputStream getInputStream() {
                return new MockServletInputStream(
                        "{'topic':'" + topic + "'" +
                        ",'waitForCode':" + Boolean.FALSE +
                        ",'" + Consumer.LANGUAGE + "':'" + LocaleValidator.DEFAULT_LANGUAGE + "'" +
                        ",'" + topic + "':'" + identifier + "'" +
                        ",'" + topic + "Code':" + code +
                        "}"
                );
            }
            @Override
            public Object getAttribute(String name) {
                if (OpenIdUser.ATTR_NAME.equals(name)) {
                    OpenIdUser user = OpenIdUser.populate(
                            "http://www.yahoo.com",
                            YadisDiscovery.IDENTIFIER_SELECT,
                            LoginServlet.YAHOO_OPENID_SERVER_URL
                    );
                    Map<String, Object> json = new HashMap<String, Object>();
                    // {a: "claimId", b: "identity", c: "assocHandle", d: associationData, e: "openIdServer", f: "openIdDelegate", g: attributes, h: "identifier"}
                    json.put("a", openId);
                    user.fromJSON(json);
                    return user;
                }
                fail("Attribute access not expected for: " + name);
                return null;
            }
        };

        final MockServletOutputStream stream = new MockServletOutputStream();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse() {
            @Override
            public ServletOutputStream getOutputStream() {
                return stream;
            }
        };

        servlet.doPost(mockRequest, mockResponse);
        assertTrue(stream.contains("'success':true"));
        assertTrue(stream.contains("'codeValidity':true"));
    }

    @Test
    public void testBadVerificationCodeI() throws IOException {
        final String openId = "http://openId";
        final String identifier = "unit@test.ca";
        final String topic = Consumer.EMAIL;
        final long code = 0;

        HttpServletRequest mockRequest = new MockHttpServletRequest() {
            @Override
            public String getPathInfo() {
                return "/processVerificationCode";
            }
            @Override
            public ServletInputStream getInputStream() {
                return new MockServletInputStream(
                        "{'topic':'" + topic + "'" +
                        ",'waitForCode':" + Boolean.FALSE +
                        ",'" + Consumer.LANGUAGE + "':'" + LocaleValidator.DEFAULT_LANGUAGE + "'" +
                        ",'" + topic + "':'" + identifier + "'" +
                        ",'" + topic + "Code':" + code +
                        "}"
                );
            }
            @Override
            public Object getAttribute(String name) {
                if (OpenIdUser.ATTR_NAME.equals(name)) {
                    OpenIdUser user = OpenIdUser.populate(
                            "http://www.yahoo.com",
                            YadisDiscovery.IDENTIFIER_SELECT,
                            LoginServlet.YAHOO_OPENID_SERVER_URL
                    );
                    Map<String, Object> json = new HashMap<String, Object>();
                    // {a: "claimId", b: "identity", c: "assocHandle", d: associationData, e: "openIdServer", f: "openIdDelegate", g: attributes, h: "identifier"}
                    json.put("a", openId);
                    user.fromJSON(json);
                    return user;
                }
                fail("Attribute access not expected for: " + name);
                return null;
            }
        };

        final MockServletOutputStream stream = new MockServletOutputStream();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse() {
            @Override
            public ServletOutputStream getOutputStream() {
                return stream;
            }
        };

        servlet.doPost(mockRequest, mockResponse);
        assertTrue(stream.contains("'success':true"));
        assertTrue(stream.contains("'codeValidity':false"));
    }

    @Test
    public void testBadVerificationCodeII() throws IOException {
        final String openId = "http://openId";
        final String identifier = "unit@test.ca";
        final String topic = Consumer.JABBER_ID;
        final long code = 0;

        HttpServletRequest mockRequest = new MockHttpServletRequest() {
            @Override
            public String getPathInfo() {
                return "/processVerificationCode";
            }
            @Override
            public ServletInputStream getInputStream() {
                return new MockServletInputStream(
                        "{'topic':'" + topic + "'" +
                        ",'waitForCode':" + Boolean.FALSE +
                        ",'" + Consumer.LANGUAGE + "':'" + LocaleValidator.DEFAULT_LANGUAGE + "'" +
                        ",'" + topic + "':'" + identifier + "'" +
                        ",'" + topic + "Code':" + code +
                        "}"
                );
            }
            @Override
            public Object getAttribute(String name) {
                if (OpenIdUser.ATTR_NAME.equals(name)) {
                    OpenIdUser user = OpenIdUser.populate(
                            "http://www.yahoo.com",
                            YadisDiscovery.IDENTIFIER_SELECT,
                            LoginServlet.YAHOO_OPENID_SERVER_URL
                    );
                    Map<String, Object> json = new HashMap<String, Object>();
                    // {a: "claimId", b: "identity", c: "assocHandle", d: associationData, e: "openIdServer", f: "openIdDelegate", g: attributes, h: "identifier"}
                    json.put("a", openId);
                    user.fromJSON(json);
                    return user;
                }
                fail("Attribute access not expected for: " + name);
                return null;
            }
        };

        final MockServletOutputStream stream = new MockServletOutputStream();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse() {
            @Override
            public ServletOutputStream getOutputStream() {
                return stream;
            }
        };

        servlet.doPost(mockRequest, mockResponse);
        assertTrue(stream.contains("'success':true"));
        assertTrue(stream.contains("'codeValidity':false"));
    }

    @Test
    public void testBadVerificationCodeIII() throws IOException {
        final String openId = "http://openId";
        final String identifier = "unit_test_ca";
        final String topic = Consumer.TWITTER_ID;
        final long code = 0;

        HttpServletRequest mockRequest = new MockHttpServletRequest() {
            @Override
            public String getPathInfo() {
                return "/processVerificationCode";
            }
            @Override
            public ServletInputStream getInputStream() {
                return new MockServletInputStream(
                        "{'topic':'" + topic + "'" +
                        ",'waitForCode':" + Boolean.FALSE +
                        ",'" + Consumer.LANGUAGE + "':'" + LocaleValidator.DEFAULT_LANGUAGE + "'" +
                        ",'" + topic + "':'" + identifier + "'" +
                        ",'" + topic + "Code':" + code +
                        "}"
                );
            }
            @Override
            public Object getAttribute(String name) {
                if (OpenIdUser.ATTR_NAME.equals(name)) {
                    OpenIdUser user = OpenIdUser.populate(
                            "http://www.yahoo.com",
                            YadisDiscovery.IDENTIFIER_SELECT,
                            LoginServlet.YAHOO_OPENID_SERVER_URL
                    );
                    Map<String, Object> json = new HashMap<String, Object>();
                    // {a: "claimId", b: "identity", c: "assocHandle", d: associationData, e: "openIdServer", f: "openIdDelegate", g: attributes, h: "identifier"}
                    json.put("a", openId);
                    user.fromJSON(json);
                    return user;
                }
                fail("Attribute access not expected for: " + name);
                return null;
            }
        };

        final MockServletOutputStream stream = new MockServletOutputStream();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse() {
            @Override
            public ServletOutputStream getOutputStream() {
                return stream;
            }
        };

        servlet.doPost(mockRequest, mockResponse);
        assertTrue(stream.contains("'success':true"));
        assertTrue(stream.contains("'codeValidity':false"));
    }

    @Test
    public void testBadTopic() throws IOException {
        final String openId = "http://openId";
        final String identifier = "unit@test.ca";
        final String topic = "zzz";
        final long code = 0;

        HttpServletRequest mockRequest = new MockHttpServletRequest() {
            @Override
            public String getPathInfo() {
                return "/processVerificationCode";
            }
            @Override
            public ServletInputStream getInputStream() {
                return new MockServletInputStream(
                        "{'topic':'" + topic + "'" +
                        ",'waitForCode':" + Boolean.FALSE +
                        ",'" + Consumer.LANGUAGE + "':'" + LocaleValidator.DEFAULT_LANGUAGE + "'" +
                        ",'" + topic + "':'" + identifier + "'" +
                        ",'" + topic + "Code':" + code +
                        "}"
                );
            }
            @Override
            public Object getAttribute(String name) {
                if (OpenIdUser.ATTR_NAME.equals(name)) {
                    OpenIdUser user = OpenIdUser.populate(
                            "http://www.yahoo.com",
                            YadisDiscovery.IDENTIFIER_SELECT,
                            LoginServlet.YAHOO_OPENID_SERVER_URL
                    );
                    Map<String, Object> json = new HashMap<String, Object>();
                    // {a: "claimId", b: "identity", c: "assocHandle", d: associationData, e: "openIdServer", f: "openIdDelegate", g: attributes, h: "identifier"}
                    json.put("a", openId);
                    user.fromJSON(json);
                    return user;
                }
                fail("Attribute access not expected for: " + name);
                return null;
            }
        };

        final MockServletOutputStream stream = new MockServletOutputStream();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse() {
            @Override
            public ServletOutputStream getOutputStream() {
                return stream;
            }
        };

        servlet.doPost(mockRequest, mockResponse);
        assertTrue(stream.contains("'success':false"));
    }

    @Test
    public void testDoGetSetupRobotCoordinatesI() throws IOException {
        final Long consumerKey = 12321L;
        final Long saleAssociateKey = 45654L;
        final Settings settings = new Settings();

        // Prepare mock SettingsOperations
        BaseSteps.setMockSettingsOperations(new SettingsOperations() {
            @Override
            public Settings getSettings(PersistenceManager pm) {
                return settings;
            }
            @Override
            public Settings updateSettings(PersistenceManager pm, Settings updatedSettings) {
                assertEquals(settings, updatedSettings);
                assertEquals(consumerKey, updatedSettings.getRobotConsumerKey());
                assertEquals(saleAssociateKey, updatedSettings.getRobotSaleAssociateKey());
                return updatedSettings;
            }
        });

        // Prepare mock servlet parameters
        HttpServletRequest mockRequest = new MockHttpServletRequest() {
            @Override
            public String getPathInfo() {
                return "/setupRobotCoordinates";
            }
            @Override
            public String getParameter(String name) {
                if ("consumerKey".equals(name)) {
                    return consumerKey.toString();
                }
                if ("saleAssociateKey".equals(name)) {
                    return saleAssociateKey.toString();
                }
                fail("Parameter query for " + name + " not expected");
                return null;
            }
        };
        final MockServletOutputStream stream = new MockServletOutputStream();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse() {
            @Override
            public ServletOutputStream getOutputStream() {
                return stream;
            }
        };

        servlet.doGet(mockRequest, mockResponse);
        assertTrue(stream.contains("'success':true"));

        // Clean-up
    }

    @Test
    public void testDoGetSetupRobotCoordinatesII() throws IOException {
        final Long consumerKey = 12321L;
        final Long saleAssociateKey = 45654L;

        // Prepare mock SettingsOperations
        BaseSteps.setMockSettingsOperations(new SettingsOperations() {
            @Override
            public Settings getSettings(PersistenceManager pm) {
                throw new RuntimeException("To exercise the 'finally { pm.close(); }' sentence.");
            }
            @Override
            public Settings updateSettings(PersistenceManager pm, Settings updatedSettings) {
                fail("Call not expected");
                return null;
            }
        });

        // Prepare mock servlet parameters
        HttpServletRequest mockRequest = new MockHttpServletRequest() {
            @Override
            public String getPathInfo() {
                return "/setupRobotCoordinates";
            }
            @Override
            public String getParameter(String name) {
                if ("consumerKey".equals(name)) {
                    return consumerKey.toString();
                }
                if ("saleAssociateKey".equals(name)) {
                    return saleAssociateKey.toString();
                }
                fail("Parameter query for " + name + " not expected");
                return null;
            }
            @Override
            public Map<String, ?> getParameterMap() {
                return new HashMap<String, Object>();
            }
        };
        final MockServletOutputStream stream = new MockServletOutputStream();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse() {
            @Override
            public ServletOutputStream getOutputStream() {
                return stream;
            }
        };

        CatchAllMailHandlerServlet.foolNextMessagePost();

        servlet.doGet(mockRequest, mockResponse);
        assertTrue(stream.contains("'success':false"));

        // Clean-up
    }

    @Test
    public void testDoGetConsolidateConsumerAccountsI() throws IOException {
        final Long consumerKey = 12321L;
        final Long demandKey = 45654L;
        final Demand demand = new Demand();
        demand.setKey(demandKey);

        // Prepare mock DemandOperations
        BaseSteps.setMockDemandOperations(new DemandOperations() {
            @Override
            public Demand getDemand(PersistenceManager pm, Long key, Long ownerKey) {
                assertEquals(demandKey, key);
                return demand;
            }
            @Override
            public Demand updateDemand(PersistenceManager pm, Demand updatedDemand) {
                assertEquals(demand, updatedDemand);
                assertEquals(consumerKey, updatedDemand.getOwnerKey());
                return updatedDemand;
            }
        });

        // Prepare mock servlet parameters
        HttpServletRequest mockRequest = new MockHttpServletRequest() {
            @Override
            public String getPathInfo() {
                return "/consolidateConsumerAccounts";
            }
            @Override
            public String getParameter(String name) {
                if ("ownerKey".equals(name)) {
                    return consumerKey.toString();
                }
                if ("key".equals(name)) {
                    return demandKey.toString();
                }
                fail("Parameter query for " + name + " not expected");
                return null;
            }
        };
        final MockServletOutputStream stream = new MockServletOutputStream();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse() {
            @Override
            public ServletOutputStream getOutputStream() {
                return stream;
            }
        };

        servlet.doGet(mockRequest, mockResponse);
        assertTrue(stream.contains("'success':true"));

        // Clean-up
    }

    @Test
    public void testDoGetConsolidateConsumerAccountsII() throws IOException {
        final Long consumerKey = 12321L;
        final Long demandKey = 45654L;
        final Demand demand = new Demand();
        demand.setKey(demandKey);

        // Prepare mock DemandOperations
        BaseSteps.setMockDemandOperations(new DemandOperations() {
            @Override
            public Demand getDemand(PersistenceManager pm, Long key, Long ownerKey) {
                throw new RuntimeException("To exercise the 'finally { pm.close(); }' sentence.");
            }
            @Override
            public Demand updateDemand(PersistenceManager pm, Demand updatedDemand) {
                fail("Call not expected");
                return null;
            }
        });

        // Prepare mock servlet parameters
        HttpServletRequest mockRequest = new MockHttpServletRequest() {
            @Override
            public String getPathInfo() {
                return "/consolidateConsumerAccounts";
            }
            @Override
            public String getParameter(String name) {
                if ("ownerKey".equals(name)) {
                    return consumerKey.toString();
                }
                if ("key".equals(name)) {
                    return demandKey.toString();
                }
                fail("Parameter query for " + name + " not expected");
                return null;
            }
            @Override
            public Map<String, ?> getParameterMap() {
                return new HashMap<String, Object>();
            }
        };
        final MockServletOutputStream stream = new MockServletOutputStream();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse() {
            @Override
            public ServletOutputStream getOutputStream() {
                return stream;
            }
        };

        servlet.doGet(mockRequest, mockResponse);
        assertTrue(stream.contains("'success':false"));

        // Clean-up
    }

    @Test
    public void testDoGetSpeedUpLoadTweetsI() throws IOException {
        final String duration = null;

        // Prepare mock servlet parameters
        HttpServletRequest mockRequest = new MockHttpServletRequest() {
            @Override
            public String getPathInfo() {
                return "/speedUpLoadTweets";
            }
            @Override
            public String getParameter(String name) {
                if ("duration".equals(name)) {
                    return duration;
                }
                fail("Parameter query for " + name + " not expected");
                return null;
            }
        };
        final MockServletOutputStream stream = new MockServletOutputStream();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse() {
            @Override
            public ServletOutputStream getOutputStream() {
                return stream;
            }
        };

        servlet.doGet(mockRequest, mockResponse);
        assertTrue(stream.contains("'success':true"));

        MockQueue queue = (MockQueue) ((MockBaseOperations) BaseSteps.getBaseOperations()).getPreviousQueue();
        assertEquals(300 / 20, queue.getHistory().size());
    }

    @Test
    public void testDoGetSpeedUpLoadTweetsII() throws IOException {
        final String duration = "0";

        // Prepare mock servlet parameters
        HttpServletRequest mockRequest = new MockHttpServletRequest() {
            @Override
            public String getPathInfo() {
                return "/speedUpLoadTweets";
            }
            @Override
            public String getParameter(String name) {
                if ("duration".equals(name)) {
                    return duration;
                }
                fail("Parameter query for " + name + " not expected");
                return null;
            }
        };
        final MockServletOutputStream stream = new MockServletOutputStream();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse() {
            @Override
            public ServletOutputStream getOutputStream() {
                return stream;
            }
        };

        servlet.doGet(mockRequest, mockResponse);
        assertTrue(stream.contains("'success':true"));

        MockQueue queue = (MockQueue) ((MockBaseOperations) BaseSteps.getBaseOperations()).getPreviousQueue();
        assertEquals(0, queue.getHistory().size());
    }

    @Test
    public void testDoGetSpeedUpLoadTweetsIII() throws IOException {
        final String duration = "12";

        // Prepare mock servlet parameters
        HttpServletRequest mockRequest = new MockHttpServletRequest() {
            @Override
            public String getPathInfo() {
                return "/speedUpLoadTweets";
            }
            @Override
            public String getParameter(String name) {
                if ("duration".equals(name)) {
                    return duration;
                }
                fail("Parameter query for " + name + " not expected");
                return null;
            }
        };
        final MockServletOutputStream stream = new MockServletOutputStream();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse() {
            @Override
            public ServletOutputStream getOutputStream() {
                return stream;
            }
        };

        servlet.doGet(mockRequest, mockResponse);
        assertTrue(stream.contains("'success':true"));

        MockQueue queue = (MockQueue) ((MockBaseOperations) BaseSteps.getBaseOperations()).getPreviousQueue();
        assertEquals(1, queue.getHistory().size());
    }

    @Test
    public void testDoGetSpeedUpLoadTweetsIV() throws IOException {
        final String duration = "100";

        // Prepare mock servlet parameters
        HttpServletRequest mockRequest = new MockHttpServletRequest() {
            @Override
            public String getPathInfo() {
                return "/speedUpLoadTweets";
            }
            @Override
            public String getParameter(String name) {
                if ("duration".equals(name)) {
                    return duration;
                }
                fail("Parameter query for " + name + " not expected");
                return null;
            }
        };
        final MockServletOutputStream stream = new MockServletOutputStream();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse() {
            @Override
            public ServletOutputStream getOutputStream() {
                return stream;
            }
        };

        servlet.doGet(mockRequest, mockResponse);
        assertTrue(stream.contains("'success':true"));

        MockQueue queue = (MockQueue) ((MockBaseOperations) BaseSteps.getBaseOperations()).getPreviousQueue();
        assertEquals(100 / 20, queue.getHistory().size());
    }

    @Test
    public void testDoGetSpeedUpLoadTweets() throws IOException {
        final String duration = "1000";

        // Prepare mock servlet parameters
        HttpServletRequest mockRequest = new MockHttpServletRequest() {
            @Override
            public String getPathInfo() {
                return "/speedUpLoadTweets";
            }
            @Override
            public String getParameter(String name) {
                if ("duration".equals(name)) {
                    return duration;
                }
                fail("Parameter query for " + name + " not expected");
                return null;
            }
        };
        final MockServletOutputStream stream = new MockServletOutputStream();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse() {
            @Override
            public ServletOutputStream getOutputStream() {
                return stream;
            }
        };

        servlet.doGet(mockRequest, mockResponse);
        assertTrue(stream.contains("'success':true"));

        MockQueue queue = (MockQueue) ((MockBaseOperations) BaseSteps.getBaseOperations()).getPreviousQueue();
        assertEquals(1000 / 20, queue.getHistory().size());
    }
}