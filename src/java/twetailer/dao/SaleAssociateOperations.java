package twetailer.dao;

import java.util.List;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import twetailer.DataSourceException;
import twetailer.dto.Consumer;
import twetailer.dto.SaleAssociate;
import twetailer.dto.Store;

public class SaleAssociateOperations extends BaseOperations {
    private static final Logger log = Logger.getLogger(SaleAssociateOperations.class.getName());

    @Override
    protected Logger getLogger() {
        return log;
    }

    /**
     * Create the SaleAssociate instance with the given parameters
     *
     * @param consumer Existing consumer account to extend
     * @param store storeKey identifier of the store where the sale associate works
     * @return Just created resource
     *
     * @see SaleAssociateOperations#createSaleAssociate(PersistenceManager, Consumer, Store)
     */
    public SaleAssociate createSaleAssociate(Consumer consumer, Long storeKey) {
        PersistenceManager pm = getPersistenceManager();
        try {
            return createSaleAssociate(pm, consumer, storeKey);
        }
        finally {
            pm.close();
        }
    }

    /**
     * Create the SaleAssociate instance with the given parameters
     *
     * @param pm Persistence manager instance to use - let open at the end to allow possible object updates later
     * @param consumer Existing consumer account to extend
     * @param store storeKey identifier of the store where the sale associate works
     * @return Just created resource
     *
     * @see SaleAssociateOperations#createSaleAssociate(PersistenceManager, SaleAssociate)
     */
    public SaleAssociate createSaleAssociate(PersistenceManager pm, Consumer consumer, Long storeKey) {
        SaleAssociate saleAssociate = new SaleAssociate();

        saleAssociate.setName(consumer.getName());
        saleAssociate.setConsumerKey(consumer.getKey());

        // Copy the user's attribute
        saleAssociate.setJabberId(consumer.getJabberId());
        saleAssociate.setEmail(consumer.getEmail());
        saleAssociate.setTwitterId(consumer.getTwitterId());
        saleAssociate.setLanguage(consumer.getLanguage());

        // Attach to the store
        saleAssociate.setStoreKey(storeKey);

        // Persist the account
        return createSaleAssociate(pm, saleAssociate);
    }

    /**
     * Create the SaleAssociate instance with the given parameters
     *
     * @param pm Persistence manager instance to use - let open at the end to allow possible object updates later
     * @param saleAssociate Resource to persist
     * @return Just created resource
     */
    public SaleAssociate createSaleAssociate(PersistenceManager pm, SaleAssociate saleAssociate) {
        pm.makePersistent(saleAssociate);
        return saleAssociate;
    }

    /**
     * Use the given key to get the corresponding SaleAssociate instance
     *
     * @param key Identifier of the sale associate
     * @return First sale associate matching the given criteria or <code>null</code>
     *
     * @throws DataSourceException If the retrieved sale associate does not belong to the specified user
     *
     * @see SaleAssociateOperations#getSaleAssociate(PersistenceManager, Long)
     */
    public SaleAssociate getSaleAssociate(Long key) throws DataSourceException {
        PersistenceManager pm = getPersistenceManager();
        try {
            return getSaleAssociate(pm, key);
        }
        finally {
            pm.close();
        }
    }

    /**
     * Use the given key to get the corresponding SaleAssociate instance
     *
     * @param pm Persistence manager instance to use - let open at the end to allow possible object updates later
     * @param key Identifier of the sale associate
     * @return First sale associate matching the given criteria or <code>null</code>
     *
     * @throws DataSourceException If the sale associate cannot be retrieved
     */
    public SaleAssociate getSaleAssociate(PersistenceManager pm, Long key) throws DataSourceException {
        if (key == null || key == 0L) {
            throw new IllegalArgumentException("Invalid key; cannot retrieve the SaleAssociate instance");
        }
        getLogger().warning("Get SaleAssociate instance with id: " + key);
        try {
            SaleAssociate saleAssociate = pm.getObjectById(SaleAssociate.class, key);
            if (saleAssociate.getCriteria() != null) {
                saleAssociate.getCriteria().size();
            }
            return saleAssociate;
        }
        catch(Exception ex) {
            throw new DataSourceException("Error while retrieving sale associate for identifier: " + key + " -- ex: " + ex.getMessage(), ex);
        }
    }

    /**
     * Use the given pair {attribute; value} to get the corresponding SaleAssociate instances
     *
     * @param attribute Name of the demand attribute used a the search criteria
     * @param value Pattern for the search attribute
     * @param limit Maximum number of expected results, with 0 means the system will use its default limit
     * @return Collection of sale associates matching the given criteria
     *
     * @throws DataSourceException If given value cannot matched a data sale associate type
     *
     * @see SaleAssociatesServlet#getSaleAssociates(PersistenceManager, String, Object)
     */
    public List<SaleAssociate> getSaleAssociates(String attribute, Object value, int limit) throws DataSourceException {
        PersistenceManager pm = getPersistenceManager();
        try {
            return getSaleAssociates(pm, attribute, value, limit);
        }
        finally {
            pm.close();
        }
    }

    /**
     * Use the given pair {attribute; value} to get the corresponding SaleAssociate instances while leaving the given persistence manager open for future updates
     *
     * @param pm Persistence manager instance to use - let open at the end to allow possible object updates later
     * @param attribute Name of the demand attribute used a the search criteria
     * @param value Pattern for the search attribute
     * @param limit Maximum number of expected results, with 0 means the system will use its default limit
     * @return Collection of sale associates matching the given criteria
     *
     * @throws DataSourceException If given value cannot matched a data sale associate type
     */
    @SuppressWarnings("unchecked")
    public List<SaleAssociate> getSaleAssociates(PersistenceManager pm, String attribute, Object value, int limit) throws DataSourceException {
        // Prepare the query
        Query queryObj = pm.newQuery(SaleAssociate.class);
        value = prepareQuery(queryObj, attribute, value, limit);
        getLogger().warning("Select sale associate(s) with: " + queryObj.toString());
        // Select the corresponding resources
        List<SaleAssociate> saleAssociates = (List<SaleAssociate>) queryObj.execute(value);
        saleAssociates.size(); // FIXME: remove workaround for a bug in DataNucleus
        return saleAssociates;
    }

    /**
     * Persist the given (probably updated) resource
     *
     * @param saleAssociate Resource to update
     * @return Updated resource
     *
     * @see SaleAssociateOperations#updateSaleAssociate(PersistenceManager, SaleAssociate)
     */
    public SaleAssociate updateSaleAssociate(SaleAssociate saleAssociate) {
        PersistenceManager pm = getPersistenceManager();
        try {
            // Persist updated sale associate
            return updateSaleAssociate(pm, saleAssociate);
        }
        finally {
            pm.close();
        }
    }

    /**
     * Persist the given (probably updated) resource while leaving the given persistence manager open for future updates
     *
     * @param pm Persistence manager instance to use - let open at the end to allow possible object updates later
     * @param saleAssociate Resource to update
     * @return Updated resource
     */
    public SaleAssociate updateSaleAssociate(PersistenceManager pm, SaleAssociate saleAssociate) {
        getLogger().warning("Updating sale associate with id: " + saleAssociate.getKey());
        saleAssociate = pm.makePersistent(saleAssociate);
        return saleAssociate;
    }
}