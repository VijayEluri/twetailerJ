package twetailer.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.logging.Logger;

import javax.cache.Cache;
import javax.cache.CacheException;
import javax.jdo.PersistenceManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import twetailer.DataSourceException;
import twetailer.dto.Settings;

public class TestSettingsOperations {

    private MockAppEngineEnvironment mockAppEngineEnvironment;

    @Before
    public void setUp() throws Exception {
        mockAppEngineEnvironment = new MockAppEngineEnvironment();
        mockAppEngineEnvironment.setUp();

        BaseOperations.setPersistenceManagerFactory(mockAppEngineEnvironment.getPersistenceManagerFactory());
    }

    @After
    public void tearDown() throws Exception {
        mockAppEngineEnvironment.tearDown();
    }

    @Test
    public void testGetLogger() throws IOException {
        Logger log1 = new RetailerOperations().getLogger();
        assertNotNull(log1);
        Logger log2 = new RetailerOperations().getLogger();
        assertNotNull(log2);
        assertEquals(log1, log2);
    }

    @Test
    public void testGetSettingsII() throws DataSourceException {
        SettingsOperations ops = new SettingsOperations() {
            @Override
            public Settings getSettingsFromCache() {
                return null;
            }
        };
        Settings settings = ops.getSettings();
        assertEquals(Long.valueOf(1L), settings.getLastProcessDirectMessageId());
    }

    @Test
    public void testGetSettingsFromCacheI() throws DataSourceException {
        SettingsOperations ops = new SettingsOperations();
        Settings settings = ops.getSettingsFromCache();
        assertNull(settings);
    }

    @Test
    public void testGetSettingsFromCacheII() throws DataSourceException {
        SettingsOperations ops = new SettingsOperations() {
            @Override
            protected Cache getCache() throws CacheException {
                throw new CacheException("done in purpose");
            }
        };
        Settings settings = ops.getSettingsFromCache();
        assertNull(settings);
    }

    @Test
    public void testUpdateSettings() throws DataSourceException {
        // Retrieve default settings and update one field
        SettingsOperations ops = new SettingsOperations();
        PersistenceManager pm = ops.getPersistenceManager();
        try {
            Settings settings = ops.getSettings(pm);
            settings.setLastProcessDirectMessageId(111L);
            ops.updateSettings(pm, settings);
        }
        finally {
            pm.close();
        }

        // Verify the default settings has persisted
        Settings updated = ops.getSettings(false);
        assertEquals(Long.valueOf(111L), updated.getLastProcessDirectMessageId());
    }

    @Test
    public void testCreateManySettingsI() throws DataSourceException {
        // Retrieve default settings and update one field
        SettingsOperations ops = new SettingsOperations();
        PersistenceManager pm = ops.getPersistenceManager();
        try {
            Settings settings = new Settings();
            settings.setLastProcessDirectMessageId(111L);
            ops.updateSettings(pm, settings);

            settings = new Settings();
            settings.setLastProcessDirectMessageId(222L);
            ops.updateSettings(pm, settings); // Save but does not replace the first one

            settings = new Settings();
            settings.setLastProcessDirectMessageId(333L);
            ops.updateSettings(pm, settings); // Save but does not replace the first one
        }
        finally {
            pm.close();
        }

        // Verify the default settings has persisted
        Settings updated = ops.getSettings(false);
        assertEquals(Long.valueOf(111L), updated.getLastProcessDirectMessageId());
    }

    @Test
    public void testCreateManySettingsII() throws DataSourceException {
        // Retrieve default settings and update one field
        SettingsOperations ops = new SettingsOperations();

        Settings settings = new Settings();
        settings.setLastProcessDirectMessageId(111L);
        ops.updateSettings(settings);

        settings = new Settings();
        settings.setLastProcessDirectMessageId(222L);
        ops.updateSettings(settings); // The first one is reloaded, updated, and persisted

        settings = new Settings();
        settings.setLastProcessDirectMessageId(333L);
        ops.updateSettings(settings); // The first one is reloaded, updated, and persisted

        // Verify the default settings has persisted
        Settings updated = ops.getSettings(false);
        assertEquals(Long.valueOf(333L), updated.getLastProcessDirectMessageId());
    }

    @Test
    public void testGetSettingFromCache() throws DataSourceException {
        SettingsOperations ops = new SettingsOperations();

        ops.getSettings(); // No data in cache

        Settings settings = new Settings();
        settings.setLastProcessDirectMessageId(111L);
        ops.updateSettings(settings);

        ops.getSettings(); // Data loaded from the cache
    }
}