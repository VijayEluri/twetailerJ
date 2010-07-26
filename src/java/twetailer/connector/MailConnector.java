package twetailer.connector;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.servlet.http.HttpServletRequest;

import twetailer.j2ee.MailResponderServlet;
import twetailer.validator.ApplicationSettings;
import twetailer.validator.LocaleValidator;
import domderrien.i18n.LabelExtractor;

/**
 * Definition of the methods specific to communication over IMAP
 *
 * @author Dom Derrien
 */
public class MailConnector {
    //
    // Mail properties (transparently handled by App Engine)
    //
    // Common mail properties
    //   mail.transport.protocol: smtp/pop3/imap
    //   mail.host: appspotmail.com
    //   mail.user: twetailer
    //   mail.password: ???
    //

    /**
     * Use the Google App Engine API to get the Mail message carried by the HTTP request
     *
     * @param request Request parameters submitted by the Google App Engine in response to the reception of an mail message sent to maezel@twetailer.appspotmail.com
     * @return Extracted mail message information
     *
     * @throws IOException If the HTTP request stream parsing fails
     */
    public static MimeMessage getMailMessage(HttpServletRequest request) throws IOException, MessagingException {
        // Extract the incoming message
        Properties properties = new Properties();
        Session session = Session.getDefaultInstance(properties, null);
        MimeMessage mailMessage = new MimeMessage(session, request.getInputStream());
        return mailMessage;
    }

    public static InternetAddress twetailer;
    public static InternetAddress twetailer_cc;
    static {
        twetailer = prepareInternetAddress(
                "UTF-8",
                ApplicationSettings.get().getProductName(),
                MailResponderServlet.responderEndpoints.get(0)
        );
        twetailer_cc = prepareInternetAddress(
                "UTF-8",
                ApplicationSettings.get().getProductName(), // TODO: Change the label by "noreply
                MailResponderServlet.responderEndpoints.get(0).replace("@", "-noreply@")
        );
    }

    /**
     * Helper setting up an InternetAddress instance with the given parameters
     *
     * @param name Display name of the e-mail address
     * @param email E-mail address
     * @return address Fetched InternetAddress instance
     */
    protected static InternetAddress prepareInternetAddress(String charsetEncoding, String name, String email) {
        InternetAddress address = new InternetAddress();
        address.setAddress(email);
        if (name != null && 0 < name.length()) {
            try {
                address.setPersonal(name, charsetEncoding);
            }
            catch (UnsupportedEncodingException e) {
                // Too bad! The recipient will only see the e-mail address

                // Note for the testers:
                //   Don't know how to generate a UnsupportedEncodingException by just
                //   injecting a corrupted UTF-8 sequence and/or a wrong character set

            }
        }
        return address;
    }

    /**
     * Use the Google App Engine API to send an mail message to the identified e-mail address
     *
     * @param useCcAccount Indicates that the message should be sent from a CC account, which is going to ignore unexpected replies
     * @param receiverId E-mail address of the recipient
     * @param recipientName recipient display name
     * @param subject Subject of the message that triggered this response
     * @param message Message to send
     * @param locale recipient's locale
     *
     * @throws MessagingException If one of the message attribute is incorrect
     * @throws UnsupportedEncodingException if the e-mail address is invalid
     */
    public static void sendMailMessage(boolean useCcAccount, String receiverId, String recipientName, String subject, String message, Locale locale) throws MessagingException, UnsupportedEncodingException {
        InternetAddress recipient = new InternetAddress(receiverId, recipientName, "UTF-8");

        Properties properties = new Properties();
        Session session = Session.getDefaultInstance(properties, null);

        MimeMessage mailMessage = new MimeMessage(session);
        mailMessage.setFrom(useCcAccount ? twetailer_cc : twetailer);
        mailMessage.setRecipient(Message.RecipientType.TO, recipient);
        String responsePrefix = LabelExtractor.get("mc_mail_subject_response_prefix", locale);
        if (subject == null || subject.length() == 0) {
            mailMessage.setSubject(LabelExtractor.get("mc_mail_subject_response_default", locale), "UTF-8");
        }
        else if (subject.startsWith(responsePrefix)) {
            mailMessage.setSubject(subject, "UTF-8");
        }
        else {
            mailMessage.setSubject(responsePrefix + subject, "UTF-8");
        }
        setContentAsPlainTextAndHtml(mailMessage, message);
        Transport.send(mailMessage);
    }

    /**
     * Utility inserting the given text into the message as a multi-part segment with a plain text and a plain HTML version
     *
     * @param message Message container
     * @param content Text to send
     *
     * @throws MessagingException If the submitted text is not accepted
     */
    public static void setContentAsPlainTextAndHtml(MimeMessage message, String content) throws MessagingException {
        MimeBodyPart textPart = new MimeBodyPart();
        MimeBodyPart htmlPart = new MimeBodyPart();

        // TODO: send a default message in case "content" is null or empty

        textPart.setContent(content, "text/plain; charset=UTF-8");
        if (content != null && 0 < content.length() && content.charAt(0) != '<') {
            content = content.replaceAll(BaseConnector.MESSAGE_SEPARATOR, "<br/>");
        }
        htmlPart.setContent(content, "text/html; charset=UTF-8");

        MimeMultipart multipart = new MimeMultipart("alternative");
        multipart.addBodyPart(textPart);
        multipart.addBodyPart(htmlPart);

        message.setContent(multipart);
    }

    /**
     * Study the given MIME message part to extract the first text content
     *
     * @param message MimeMessage or MimeBodyPart to study
     * @return Extracted piece of text (can be text/plain or text/HTML, on one or many lines)
     *
     * @throws MessagingException When an error occurs while parsing a piece of the message
     * @throws IOException When an error occurs while reading the message stream
     */
    public static String getText(MimeMessage message) throws MessagingException, IOException {
        if (message.isMimeType("text/plain")) {
            return LocaleValidator.toUTF8((String) message.getContent());
            // return convertToString((InputStream) message.getContent());
        }
        if (message.isMimeType("text/html")) {
            return LocaleValidator.toUTF8((String) message.getContent());
            // return convertToString((InputStream) message.getContent());
        }
        if (message.isMimeType("multipart/*")) {
            Multipart multipart = new MimeMultipart(message.getDataHandler().getDataSource());
            return getText(multipart);
        }
        return "";
    }

    /**
     * Study the given MIME part collection to extract the first text content
     *
     * @param multipart Collection to study
     * @return Extracted text or empty string
     *
     * @throws MessagingException When an error occurs while parsing a piece of the message
     * @throws IOException When an error occurs while reading the message stream
     */
    protected static String getText(Multipart multipart) throws MessagingException, IOException {
        int count = multipart.getCount();
        for(int i = 0; i < count; i++) {
            Part part = multipart.getBodyPart(i);
            String partText = getText(part);
            if (!"".equals(partText)) {
                return partText;
            }
        }
        throw new MessagingException("No text found in this message");
    }

    /**
     * Study the given MIME part to extract the first text content.
     * Note that the algorithm excludes "text/*" attachment.
     * Note also that the algorithm does not parse recursively the "multipart/*" elements.
     *
     * @param part Piece of content to study
     * @return Extracted text or empty string
     *
     * @throws MessagingException When an error occurs while parsing a piece of the message
     * @throws IOException When an error occurs while reading the message stream
     */
    protected static String getText(Part part) throws MessagingException, IOException {
        String filename = part.getFileName();
        if (filename == null && part.isMimeType("text/plain")) {
            return LocaleValidator.toUTF8((String) part.getContent());
            // return convertToString((InputStream) part.getContent());
        }
        if (filename == null && part.isMimeType("text/html")) {
            return LocaleValidator.toUTF8((String) part.getContent());
            // return convertToString((InputStream) part.getContent());
        }
        // We don't want to go deeper because this part is probably an attachment or a reply!
        // if (part.isMimeType("multipart/*")) {
            // return getText(new MimeMultipart(part.getDataHandler().getDataSource()));
        // }
        return "";
    }

    /**
     * Utility method dumping the content of an InputStream into a String buffer
     *
     * @param in InputStream to process
     * @return String containing all characters extracted from the InputStream
     *
     * @throws IOException If the InputStream process fails
     */
    /*
    //
    // Not used anymore since 1.2.8 because getContent() return String instance instead of InputStream
    //
    protected static String convertToString(InputStream in) throws IOException {
        int character = in.read();
        StringBuilder out = new StringBuilder();
        while (character != -1) {
            out.append((char) character);
            character = in.read();
        }
        return LocaleValidator.toUTF8(out.toString());
    }
    */
}
