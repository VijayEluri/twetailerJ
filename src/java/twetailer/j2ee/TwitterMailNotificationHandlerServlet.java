package twetailer.j2ee;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jdo.PersistenceManager;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import twetailer.DataSourceException;
import twetailer.connector.MailConnector;
import twetailer.connector.TwitterConnector;
import twetailer.dao.BaseOperations;
import twetailer.dao.ConsumerOperations;
import twetailer.dto.Consumer;
import twitter4j.TwitterException;

@SuppressWarnings("serial")
public class TwitterMailNotificationHandlerServlet extends HttpServlet {
    private static Logger log = Logger.getLogger(TwitterMailNotificationHandlerServlet.class.getName());

    protected BaseOperations _baseOperations = new BaseOperations();
    protected ConsumerOperations consumerOperations = _baseOperations.getConsumerOperations();

    public static final String TWITTER_INTRODUCTION_MESSAGE_RATTERN = "\\(([a-zA-Z0-9_]+)\\) is now following your tweets on Twitter\\.";
    public static final String TWITTER_NOTIFICATION_SUBJECT_SUFFIX = " is now following you on Twitter!";

    private static Pattern introductionPattern = Pattern.compile(TWITTER_INTRODUCTION_MESSAGE_RATTERN);

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            log.warning("Path Info: " + request.getPathInfo());

            // Extract the incoming message
            MimeMessage mailMessage = MailConnector.getMailMessage(request);
            log.warning("Message from: " + mailMessage.getFrom()[0]);

            // Check the message to persist
            boolean isAFollowingNotification = false;
            String subject = mailMessage.getSubject();
            String body = MailConnector.getText(mailMessage);
            String followerName = "unknown";
            if (subject != null) {
                int subjectPrefixPos = subject.indexOf(TWITTER_NOTIFICATION_SUBJECT_SUFFIX);
                if (subjectPrefixPos != -1) {
                    followerName = subject.substring(0, subjectPrefixPos).trim();
                    Matcher matcher = introductionPattern.matcher(body);
                    if (matcher.find()) { // Runs the matcher once
                        isAFollowingNotification = true;
                        String followerScreenName = matcher.group(1).trim();
                        PersistenceManager pm = _baseOperations.getPersistenceManager();
                        try {
                            List<Consumer> consumers = consumerOperations.getConsumers(pm, Consumer.TWITTER_ID, followerScreenName, 1);
                            if (consumers.size() == 0) {
                                // 1. Follow the user
                                TwitterConnector.getTwetailerAccount().enableNotification(followerScreenName);
                                // 2. Create his record
                                Consumer consumer = new Consumer();
                                consumer.setName(followerName);
                                consumer.setTwitterId(followerScreenName);
                                consumer = consumerOperations.createConsumer(pm, consumer);
                                log.warning("Consumer account created for the new Twitter follower: " + followerScreenName);
                            }
                        }
                        catch (TwitterException ex) {
                            subject += "[TwitterException:" + ex.getMessage() + "]";
                            isAFollowingNotification = false;
                            ex.printStackTrace();
                        }
                        catch(DataSourceException ex) {
                            subject += "[DataSourceException:" + ex.getMessage() + "]";
                            isAFollowingNotification = false;
                            ex.printStackTrace();
                        }
                        finally {
                            pm.close();
                        }
                    }
                }
            }

            if (!isAFollowingNotification) {
                CatchAllMailHandlerServlet.composeAndPostMailMessage(followerName, subject, body);
            }
        }
        catch (MessagingException ex) {
            // Nothing to do with a corrupted message...
        }
    }
}
