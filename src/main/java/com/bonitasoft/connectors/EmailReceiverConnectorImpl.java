package com.bonitasoft.connectors;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

import org.bonitasoft.engine.connector.ConnectorException;

import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.InternetAddress;
import javax.mail.search.FlagTerm;

public class EmailReceiverConnectorImpl extends AbstractEmailReceiverConnectorImpl {

	Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	private Store emailStore;
	private Folder inbox;

	@Override
	protected void executeBusinessLogic() throws ConnectorException {
		receiveEmail(getEmailHost(), getEmailUsername(), getEmailPassword());
	}


	protected void receiveEmail(String pop3Host, String user, String password) throws ConnectorException {
		List mails = new ArrayList();
		try {
			Message[] messages = inbox.search(new FlagTerm(new Flags(Flag.SEEN), false));
			for (Message message : messages) {
				Map<String, String> map = new HashMap<String, String>();
				map.put("subject", message.getSubject());
				InternetAddress mailAddress = (InternetAddress) message.getFrom()[0];
				map.put("name", mailAddress.getPersonal() == null ? mailAddress.getAddress() : mailAddress.getPersonal());
				map.put("from", mailAddress.getAddress());
				if (message.isMimeType("text/*")) {
					map.put("body", extractBodyFromText(message));
				} else if (message.isMimeType("multipart/*")) {
					map.put("body", extractBodyFromMultiPart(message));
				}
				mails.add(map);
				message.setFlag(Flags.Flag.SEEN, true); // delete the message so it doesn't get processed again
			}
		} catch (Exception e) {
			logger.severe("Failed to read mails - " + e.getMessage());
			throw new ConnectorException(e);
		} finally {
			setMails(mails);
		}
	}

	private String extractBodyFromText(Message message) throws IOException, MessagingException {
		return removeSignature(message.getContent().toString());
	}

	private String extractBodyFromMultiPart(Message message) throws IOException, MessagingException {
		Multipart multipart = (Multipart) message.getContent();
		for (int j = 0; j < multipart.getCount(); j++) {
			if (multipart.getBodyPart(j).isMimeType("text/plain")) {
				String content = (String) multipart.getBodyPart(j).getContent();
				return removeSignature(content);
			}
		}
		return "";
	}


	private String removeSignature(String value) {
		return value.split("-- ")[0];
	}


	@Override
	public void connect() throws ConnectorException {
		try {
			Properties properties = new Properties();
			Session session = Session.getDefaultInstance(properties);
			emailStore = session.getStore("imap");
			emailStore.connect(getEmailHost(), getEmailPort(), getEmailUsername(), getEmailPassword());
			inbox = emailStore.getFolder("Inbox");
			inbox.open(Folder.READ_WRITE);
			logger.info("Successfully connected to the email store");
		} catch (NoSuchProviderException e) {
			throw new ConnectorException(e);
		} catch (MessagingException e) {
			throw new ConnectorException(e);
		}
	}


	@Override
	public void disconnect() throws ConnectorException {
		try {
			if (inbox != null)
				inbox.close(false);
			if (emailStore != null)
				emailStore.close();
			logger.info("Successfully disconnected from the email store");
		} catch (MessagingException e) {
			throw new ConnectorException(e);
		}
	}
}
