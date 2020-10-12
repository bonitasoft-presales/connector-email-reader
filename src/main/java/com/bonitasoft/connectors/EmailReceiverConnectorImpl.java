package com.bonitasoft.connectors;

import java.util.*;
import java.util.logging.Logger;

import org.bonitasoft.engine.connector.ConnectorException;

import javax.mail.*;
import javax.mail.Flags.Flag;
import javax.mail.internet.InternetAddress;
import javax.mail.search.FlagTerm;

public class EmailReceiverConnectorImpl extends AbstractEmailReceiverConnectorImpl {

	Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	private Store emailStore;
	private Folder inboxFolder;
	private MailUtils mailUtils;

	@Override
	protected void executeBusinessLogic() throws ConnectorException {
		receiveEmail();
	}

	protected void receiveEmail() throws ConnectorException {
		List<Map<String, Object>> mails = new ArrayList();
		try {
			int messageCount = inboxFolder.getMessageCount();
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
		} catch (Exception e) {
			logger.severe("Failed to read mails - " + e.getMessage());
			throw new ConnectorException(e);
		} finally {
			setMails(mails);
		}
	}

	private void extractMessage(List<Map<String, Object>> mails, Message message) throws Exception {
		Map<String, Object> map = new HashMap<>();
		String subject = message.getSubject();
		int messageNumber = message.getMessageNumber();
		logger.info("add message with id[" + messageNumber + "] and subject[" + subject + "]");
		map.put("subject", subject);
		map.put("messageNumber", messageNumber);
		InternetAddress mailAddress = (InternetAddress) message.getFrom()[0];
		map.put("name", mailAddress.getPersonal() == null ? mailAddress.getAddress() : mailAddress.getPersonal());
		map.put("from", mailAddress.getAddress());
		map.put("receivedDate", message.getReceivedDate());
		map.put("sendDate", message.getSentDate());
		map.put("attachments", mailUtils.getAttachments(message));
		map.put("body", mailUtils.getBody(message));
		mails.add(map);
		message.setFlag(Flag.SEEN, true);
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
		Boolean sslEnabled = isEmailSslEnabled();
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
		logParameterValue(EMAIL_SSL_ENABLED_INPUT_PARAMETER, isEmailSslEnabled());
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
