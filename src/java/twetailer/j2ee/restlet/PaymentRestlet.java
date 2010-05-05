package twetailer.j2ee.restlet;

import java.util.logging.Logger;

import javax.jdo.PersistenceManager;

import twetailer.ClientException;
import twetailer.DataSourceException;
import twetailer.dao.BaseOperations;
import twetailer.dao.PaymentOperations;
import twetailer.dto.Seed;
import twetailer.j2ee.BaseRestlet;

import com.dyuproject.openid.OpenIdUser;

import domderrien.jsontools.JsonArray;
import domderrien.jsontools.JsonObject;

@SuppressWarnings("serial")
public class PaymentRestlet extends BaseRestlet {
    private static Logger log = Logger.getLogger(StoreRestlet.class.getName());

    protected static SaleAssociateRestlet saleAssociateRestlet = new SaleAssociateRestlet();

    protected static BaseOperations _baseOperations = new BaseOperations();
    protected static PaymentOperations paymentOperations = _baseOperations.getPaymentOperations();

    // Setter for injection of a MockLogger at test time
    protected static void setLogger(Logger mock) {
        log = mock;
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

    @Override
    protected JsonObject createResource(JsonObject parameters, OpenIdUser loggedUser) throws DataSourceException, ClientException {
        throw new ClientException("Restricted access!");
    }

    @Override
    protected void deleteResource(String resourceId, OpenIdUser loggedUser) throws DataSourceException, ClientException {
        throw new ClientException("Restricted access!");
    }

    @Override
    protected JsonObject getResource(JsonObject parameters, String resourceId, OpenIdUser loggedUser) throws DataSourceException {
        if (isAPrivilegedUser(loggedUser)) {
            return paymentOperations.getPayment(Long.valueOf(resourceId)).toJson();
        }
        throw new RuntimeException("Not yet implemented!");
    }

    @Override
    protected JsonArray selectResources(JsonObject parameters, OpenIdUser loggedUser) throws DataSourceException {
        throw new RuntimeException("Not yet implemented!");
    }

    @Override
    protected JsonObject updateResource(JsonObject parameters, String resourceId, OpenIdUser loggedUser) throws DataSourceException {
        throw new RuntimeException("Not yet implemented!");
    }
}