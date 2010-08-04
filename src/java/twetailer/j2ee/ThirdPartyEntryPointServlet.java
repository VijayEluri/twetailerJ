package twetailer.j2ee;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.jdo.PersistenceManager;
import javax.mail.Store;
import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import twetailer.ReservedOperationException;
import twetailer.connector.MailConnector;
import twetailer.connector.BaseConnector.Source;
import twetailer.dto.Consumer;
import twetailer.dto.Demand;
import twetailer.dto.Location;
import twetailer.task.step.BaseSteps;
import twetailer.task.step.DemandSteps;
import twetailer.validator.CommandSettings.Action;
import domderrien.jsontools.GenericJsonObject;
import domderrien.jsontools.JsonObject;
import domderrien.jsontools.JsonParser;

/**
 * Third party entry point where the referralId might be
 * compared to the IP address to avoid spam.
 *
 * Possible operations are fairly limited:
 * - post a demand
 * - get number of stores in a region (can filtered with
 * the given parameters)
 *
 * @see twetailer.j2ee.restlet.ConsumerRestlet
 * @see twetailer.j2ee.restlet.DemandRestlet
 * @see twetailer.j2ee.restlet.LocationRestlet
 * @see twetailer.j2ee.restlet.StoreRestlet
 *
 * @author Dom Derrien
 *
 */
@SuppressWarnings("serial")
public class ThirdPartyEntryPointServlet extends HttpServlet {
    private static Logger log = Logger.getLogger(MaezelServlet.class.getName());

    /** Just made available for test purposes */
    protected static void setLogger(Logger mockLogger) {
        log = mockLogger;
    }

    protected static Logger getLogger() {
        return log;
    }

    private final static String DEMAND_PREFIX = "/Demand";
    private final static String LOCATION_PREFIX = "/Location";
    private final static String STORE_PREFIX = "/Store";

    @Override
    @SuppressWarnings("unchecked")
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ServletUtils.configureHttpParameters(request, response);

        String pathInfo = request.getPathInfo();
        getLogger().fine("Path Info: " + pathInfo);

        JsonObject out = new GenericJsonObject();
        out.put("success", true);
        JsonObject in = null;

        try {
            // TODO: verify Content-type = "application/x-www-form-urlencoded"
            in = new GenericJsonObject(request.getParameterMap());

            if (LOCATION_PREFIX.equals(pathInfo)) {
                verifyReferralId(in, Action.list, Location.class.getName(), request);
                // TODO
            }
            else if (STORE_PREFIX.equals(pathInfo)) {
                verifyReferralId(in, Action.list, Store.class.getName(), request);
                // TODO
            }
            else {
                response.setStatus(404); // Not Found
                out.put("success", false);
                out.put("reason", "URL not supported");
            }
        }
        catch (ReservedOperationException ex) {
            response.setStatus(403); // Forbidden
            out.put("success", false);
            out.put("reason", ex.getMessage());
        }
        catch (Exception ex) {
            response.setStatus(500); // Internal Server Error
            out = BaseRestlet.processException(getLogger(), ex, "doGet", pathInfo);
        }

        out.toStream(response.getOutputStream(), false);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

        String pathInfo = request.getPathInfo();
        getLogger().warning("Path Info: " + pathInfo);

        JsonObject out = new GenericJsonObject();
        out.put("success", true);
        JsonObject in = null;

        try {
            // TODO: verify Content-type == "application/json"
            in = new JsonParser(request.getInputStream()).getJsonObject();

            PersistenceManager pm = BaseSteps.getBaseOperations().getPersistenceManager();
            try {
                if (DEMAND_PREFIX.equals(pathInfo)) {
                    verifyReferralId(in, Action.demand, Demand.class.getName(), request);

                    String email = in.getString(Consumer.EMAIL);
                    if (email == null || email.length() == 0 || !Pattern.matches(Consumer.EMAIL_REGEXP_VALIDATOR, email)) {
                        throw new IllegalArgumentException("Invalid sender email address");
                    }
                    InternetAddress senderAddress = MailConnector.prepareInternetAddress("UTF-8", email, email);
                    Consumer consumer = BaseSteps.getConsumerOperations().createConsumer(pm, senderAddress);

                    in.put(Demand.SOURCE, Source.widget.toString());
                    DemandSteps.createDemand(pm, in, consumer);

                    // TODO: call generateTweet
                }
                else {
                    response.setStatus(404); // Not Found
                    out.put("success", false);
                    out.put("reason", "URL not supported");
                }
            }
            finally {
                pm.close();
            }
        }
        catch (ReservedOperationException ex) {
            response.setStatus(403); // Forbidden
            out.put("success", false);
            out.put("reason", ex.getMessage());
        }
        catch (Exception ex) {
            response.setStatus(500); // Internal Server Error
            out = BaseRestlet.processException(getLogger(), ex, "doPost", pathInfo);
        }

        out.toStream(response.getOutputStream(), false);
    }



    @Override
    public void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {

        JsonObject out = new GenericJsonObject();
        response.setStatus(403); // Forbidden
        out.put("success", false);
        out.put("reason", "URL not supported");
        out.toStream(response.getOutputStream(), false);
    }

    @Override
    public void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {

        JsonObject out = new GenericJsonObject();
        response.setStatus(403); // Forbidden
        out.put("success", false);
        out.put("reason", "URL not supported");
        out.toStream(response.getOutputStream(), false);
    }

    protected void verifyReferralId(JsonObject parameters, Action action, String entityName, HttpServletRequest request) throws ReservedOperationException {
        if (!parameters.containsKey("referralId")) {
            throw new ReservedOperationException(action, entityName);
        }
    }
}
