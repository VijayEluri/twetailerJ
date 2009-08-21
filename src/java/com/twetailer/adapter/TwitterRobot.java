package com.twetailer.adapter;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jdo.PersistenceManager;

import twitter4j.DirectMessage;
import twitter4j.TwitterException;

import com.twetailer.DataSourceException;
import com.twetailer.dto.Settings;
import com.twetailer.dto.Store;
import com.twetailer.rest.BaseOperations;
import com.twetailer.rest.SettingsOperations;
import com.twetailer.rest.StoreOperations;

import domderrien.i18n.LabelExtractor;

public class TwitterRobot {
    
    protected BaseOperations _baseOperations = new BaseOperations();
    protected StoreOperations storesOperations = _baseOperations.getStoreOperations();
    protected SettingsOperations settingsOperations = _baseOperations.getSettingsOperations();

    public Long processDirectMessages() throws TwitterException, DataSourceException {
        PersistenceManager pm = _baseOperations.getPersistenceManager();
        try {
            Settings settings = settingsOperations.getSettings(pm);
            Long sinceId = settings.getLastRobotDirectMessageId();
            Long lastId = processDirectMessages(pm, sinceId);
            if (!lastId.equals(sinceId)) {
                settings.setLastRobotDirectMessageId(lastId);
                settingsOperations.updateSettings(pm, settings);
            }
            return lastId;
        }
        finally {
            pm.close();
        }
    }
    public Long processDirectMessages(PersistenceManager pm, Long sinceId) throws TwitterException, DataSourceException {
        Long lastId = sinceId;
        List<DirectMessage> messages = TwitterUtils.getDirectMessages(TwitterUtils.getRobotAccount(), sinceId);
        for (DirectMessage message: messages) {
            // Get the demand reference
            Pattern pattern = Pattern.compile("reference:(\\d+)");
            Matcher matcher = pattern.matcher(message.getText());
            matcher.find();
            Long reference = Long.valueOf(matcher.group(1));
            // Get the tags
            pattern = Pattern.compile("tags:({?:\\w+\\s*)+)\\?");
            matcher = pattern.matcher(message.getText());
            matcher.find();
            String tags = matcher.group(1);
            // Get the robot storeKey
            List<Store> stores = storesOperations.getStores(pm, Store.NAME, "Toys Factory", 1);
            Long storeKey = stores == null || stores.size() == 0 ? 0L : stores.get(0).getKey();
            // Replies with the generic proposal
            TwitterUtils.sendDirectMessage(
                    TwitterUtils.getRobotAccount(),
                    message.getSenderScreenName(),
                    LabelExtractor.get(
                            "robot_sendDefaultProposal",
                            new Object[] { reference, tags, storeKey },
                            Locale.ENGLISH
                    )
            );
        }
        return lastId;
    }
}