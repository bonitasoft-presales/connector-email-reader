package com.bonitasoft.connectors;

import org.apache.commons.mail.util.MimeMessageParser;
import org.bonitasoft.engine.bpm.document.DocumentValue;
import org.bonitasoft.engine.connector.ConnectorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.DataSource;
import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.search.FlagTerm;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

public class EmailReceiverConnectorImpl extends AbstractEmailReceiverConnectorImpl {

    Logger logger = LoggerFactory.getLogger(EmailReceiverConnectorImpl.class);

    private Store emailStore;
    private Folder inboxFolder;
    private MailUtils mailUtils;

    @Override
    protected void executeBusinessLogic() throws ConnectorException {
        setMails(readMails());
    }

    protected List<Map<String, Object>> readMails() throws ConnectorException {
        List<Map<String, Object>> mails = new ArrayList();

        try {
            int messageCount;
            messageCount = inboxFolder.getMessageCount();

            int unreadMessageCount = inboxFolder.getUnreadMessageCount();
            logger.info("Message count:" + messageCount);
            logger.info("Unread message count:" + unreadMessageCount);

            Message[] messages = inboxFolder.search(new FlagTerm(new Flags(Flag.SEEN), false));
            int count = 0;
            int batchSize = getEmailBatchSize();
            for (Message message : messages) {
                if (count >= batchSize) {
                    break;
                }
                extractMessage(mails, message);
                count++;
            }
            return mails;
        } catch (MessagingException e) {
            throw new ConnectorException(e);
        }
    }

    private void extractMessage(List<Map<String, Object>> mails, Message message) throws ConnectorException {
        try {
            Map<String, Object> map = new HashMap<>();
            if (message instanceof MimeMessage) {
                logger.info("found MimeMessage");
                MimeMessageParser parser = new MimeMessageParser((MimeMessage) message);
                parser.parse();
                String subject = parser.getSubject();
                int messageNumber = parser.getMimeMessage().getMessageNumber();
                String plainContent = parser.getPlainContent();
                String htmlContent = parser.getHtmlContent();

                map.put("subject", subject);
                map.put("messageNumber", messageNumber);
                InternetAddress address = (InternetAddress) parser.getMimeMessage().getFrom()[0];
                map.put("name", address.getPersonal());
                String from = address.getAddress();
                map.put("from", from);
                map.put("receivedDate", message.getReceivedDate());
                map.put("sendDate", message.getSentDate());
                map.put("plainContent", plainContent);
                String removeSignature = mailUtils.removeSignature(plainContent);
                map.put("body", mailUtils.sanitize(removeSignature));
                map.put("htmlContent", htmlContent);
                if (htmlContent != null) {
                    map.put("htmlContentSanitized", mailUtils.sanitize(htmlContent));
                } else {
                    map.put("htmlContentSanitized", "");
                }

                List<DataSource> attachmentList = parser.getAttachmentList();
                List<DocumentValue> attachments = new ArrayList<>();
                for (DataSource dataSource : attachmentList) {
                    Optional<DocumentValue> documentValue = mailUtils.getDocumentValue(dataSource);
                    if (documentValue.isPresent()) {
                        attachments.add(documentValue.get());
                    }
                }
                map.put("attachments", attachments);

                mailUtils.logSummary(subject, messageNumber, from, attachments);
            } else {
                logger.info("found message with type:" + message.getClass().getCanonicalName());

                String subject = message.getSubject();
                int messageNumber = message.getMessageNumber();
                map.put("subject", subject);
                map.put("messageNumber", messageNumber);
                InternetAddress mailAddress = (InternetAddress) message.getFrom()[0];
                map.put("name", mailAddress.getPersonal() == null ? mailAddress.getAddress() : mailAddress.getPersonal());
                map.put("from", mailAddress.getAddress());
                map.put("receivedDate", message.getReceivedDate());
                map.put("sendDate", message.getSentDate());
                List<DocumentValue> attachments = mailUtils.getAttachments(message);
                map.put("attachments", attachments);
                String body = mailUtils.getBody(message);
                map.put("bodyOriginal", body);
                map.put("body", mailUtils.sanitize(body));
                StringBuilder builder = new StringBuilder()
                        .append("add message with id[")
                        .append(messageNumber)
                        .append("] subject[")
                        .append(subject).append("]")
                        .append("content:[")
                        .append(map)
                        .append("] attachments:");
                for (DocumentValue attachment : attachments) {
                    builder.append("[")
                            .append(attachment.getFileName())
                            .append("]");
                }
                logger.info(builder.toString());
            }
            mailUtils.logEmail(map);
            mails.add(map);
            message.setFlag(Flag.SEEN, true);
        } catch (MessagingException | IOException e) {
            e.printStackTrace();
            logger.error("can't read message " + e.getMessage());
            throw new ConnectorException(e);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void connect() throws ConnectorException {
        try {
            Session session = Session.getDefaultInstance(getProperties());
            emailStore = session.getStore(getEmailProtocol());
            emailStore.connect(getEmailHost(), getEmailPort(), getEmailUsername(), getEmailPassword());
            inboxFolder = emailStore.getFolder(getEmailFolderName());
            inboxFolder.open(Folder.READ_WRITE);
            mailUtils = new MailUtils();
            logger.info("Successfully connected to the email store");
        } catch (MessagingException e) {
            throw new ConnectorException(e);
        }
    }

    private Properties getProperties() {
        logParameters();
        Properties properties = new Properties();
        Boolean sslEnabled = isSslEnabled();
        properties.setProperty("mail.imap.ssl.enable", sslEnabled.toString());
        return properties;
    }

    private void logParameters() {
        logParameterValue(EMAIL_HOST_INPUT_PARAMETER, getEmailHost());
        logParameterValue(EMAIL_PORT_INPUT_PARAMETER, getEmailPort());
        logParameterValue(EMAIL_PROTOCOL_INPUT_PARAMETER, getEmailProtocol());
        logParameterValue(EMAIL_USERNAME_INPUT_PARAMETER, getEmailUsername());
        logParameterValue(EMAIL_FOLDER_NAME_INPUT_PARAMETER, getEmailFolderName());
        logParameterValue(EMAIL_BATCH_SIZE_INPUT_PARAMETER, getEmailBatchSize());
        logParameterValue(EMAIL_SSL_ENABLED_INPUT_PARAMETER, isSslEnabled());
    }

    private void logParameterValue(String parameterName, Object parameterValue) {
        String msg = new StringBuilder()
                .append(parameterName)
                .append(":[")
                .append(parameterValue)
                .append("]").toString();
        logger.info(msg);
    }

    @Override
    public void disconnect() throws ConnectorException {
        try {
            if (inboxFolder != null)
                inboxFolder.close(false);
            if (emailStore != null)
                emailStore.close();
            logger.info("Successfully disconnected from the email store");
        } catch (MessagingException e) {
            throw new ConnectorException(e);
        }
    }
}
