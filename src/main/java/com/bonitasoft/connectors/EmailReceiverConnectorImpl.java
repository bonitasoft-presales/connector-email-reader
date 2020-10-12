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
			int batchSize = getBatchSize();
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
			emailStore = session.getStore(getProtocol());
			emailStore.connect(getEmailHost(), getEmailPort(), getEmailUsername(), getEmailPassword());
			inboxFolder = emailStore.getFolder(getFolderName());
			inboxFolder.open(Folder.READ_WRITE);
			mailUtils = new MailUtils();
			logger.info("Successfully connected to the email store");
		} catch (NoSuchProviderException e) {
			throw new ConnectorException(e);
		} catch (MessagingException e) {
			throw new ConnectorException(e);
		}
	}

	private Properties getProperties() {
		Properties properties = new Properties();
		properties.setProperty("mail.imap.ssl.enable", isSslEnabled().toString());
		return properties;
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
