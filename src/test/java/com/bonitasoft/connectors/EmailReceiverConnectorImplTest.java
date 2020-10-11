package com.bonitasoft.connectors;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.bonitasoft.engine.bpm.document.DocumentValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.mail.internet.MimeMessage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.bonitasoft.connectors.AbstractEmailReceiverConnectorImpl.*;
import static org.assertj.core.api.Assertions.*;


public class EmailReceiverConnectorImplTest {

	@RegisterExtension
	static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.ALL);


	EmailReceiverConnectorImpl connector;

	@BeforeEach
	void setUp() {
		connector = new EmailReceiverConnectorImpl();
	}

	@Test
	@DisplayName("Should read imap email with attachment")
	void shouldReadMailWithAttachments() throws Exception {
		//given
		String to = "to@localhost";
		String body = "some body";
		String from = "from@localhost";
		String subject = "some subject";
		byte[] attachmentByteArray = "attachmentByteArray content".getBytes();
		String contentType = "text/plain";
		String filename = "attachmentByteArray.txt";
		String attachmentDescription = "attachmentByteArray description";
		GreenMailUtil.sendAttachmentEmail(to, from, subject, body, attachmentByteArray, contentType, filename, attachmentDescription, greenMail.getSmtp().getServerSetup());
		final MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
		final MimeMessage expectedMessage = receivedMessages[0];

		//when
		Map<String, Object> parameters = getParameters(to, 10);
		connector.setInputParameters(parameters);

		connector.connect();
		Map<String, Object> execute = connector.execute();

		//then
		assertThat(execute).as("should have a response ").hasSize(1);

		List<Map> mails = (List<Map>) execute.get("mails");
		assertThat(mails).as("should have 1 email ").hasSize(1);

		Map<String, Object> firstMessage = mails.get(0);
		assertThat(firstMessage.get("subject")).as("should have a subject").isEqualTo(expectedMessage.getSubject());
		assertThat(firstMessage.get("from")).as("should have a sender").isEqualTo(expectedMessage.getFrom()[0].toString());
		assertThat(firstMessage.get("body")).as("should have a body message").isEqualTo(body);

		List<DocumentValue> docs = (List<DocumentValue>) firstMessage.get("attachments");
		assertThat(docs).hasSize(1);
		assertThat(docs.get(0).getFileName()).as("should have a file name").isEqualTo(filename);
		assertThat(docs.get(0).getMimeType()).as("should have a mime type").isEqualToIgnoringCase(contentType);
		assertThat(docs.get(0).getContent()).as("should have a content").isEqualTo(attachmentByteArray);
	}

	private Map<String, Object> getParameters(String to, Integer batchSize) {
		Map<String, Object> parameters = new HashMap<>();
		parameters.put(EMAIL_HOST_INPUT_PARAMETER, "127.0.0.1");
		parameters.put(EMAIL_PORT_INPUT_PARAMETER, greenMail.getImap().getPort());
		parameters.put(EMAIL_USERNAME_INPUT_PARAMETER, to);
		parameters.put(EMAIL_PASSWORD_INPUT_PARAMETER, to);
		parameters.put(EMAIL_FOLDER_NAME_INPUT_PARAMETER, "Inbox");
		parameters.put(EMAIL_PROTOCOL_INPUT_PARAMETER, "imap");
		parameters.put(EMAIL_SSL_ENABLED_INPUT_PARAMETER, Boolean.FALSE);
		parameters.put(EMAIL_BATCH_SIZE_INPUT_PARAMETER, batchSize);

		return parameters;
	}

	@Test
	@DisplayName("Should read imap email")
	void shouldReadMail() throws Exception {
		//given
		String to = "to@localhost";
		String body = "some body";
		String from = "from@localhost";
		String subject = "some subject";
		byte[] attachmentByteArray = "attachmentByteArray content".getBytes();
		String contentType = "text/plain";
		String filename = "attachmentByteArray.txt";
		String attachmentDescription = "attachmentByteArray description";
		GreenMailUtil.sendTextEmail(to, from, subject, body, greenMail.getSmtp().getServerSetup());
		final MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
		final MimeMessage expectedMessage = receivedMessages[0];

		//when
		Map<String, Object> parameters = getParameters(to, 10);
		connector.setInputParameters(parameters);

		connector.connect();
		Map<String, Object> execute = connector.execute();

		//then
		assertThat(execute).as("should have a response ").hasSize(1);

		List<Map> mails = (List<Map>) execute.get("mails");
		assertThat(mails).as("should have 1 email ").hasSize(1);

		Map<String, Object> firstMessage = mails.get(0);
		assertThat(firstMessage.get("subject")).as("should have a subject").isEqualTo(expectedMessage.getSubject());
		assertThat(firstMessage.get("from")).as("should have a sender").isEqualTo(expectedMessage.getFrom()[0].toString());
		assertThat(firstMessage.get("body")).as("should have a body message").isEqualTo(body);
	}

	@Test
	@DisplayName("Should limit to batch size")
	void shouldReadMailLimitedToBatchSize() throws Exception {
		//given
		String to = "to@localhost";
		String body = "some body";
		String from = "from@localhost";
		String subject = "some subject";
		byte[] attachmentByteArray = "attachmentByteArray content".getBytes();
		String contentType = "text/plain";
		String filename = "attachmentByteArray.txt";
		String attachmentDescription = "attachmentByteArray description";

		for (int i=0;i<15;i++){
			GreenMailUtil.sendTextEmail(to, from, new StringBuilder().append(subject).append(i).toString(), body, greenMail.getSmtp().getServerSetup());
		}
		final MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
		final MimeMessage expectedMessage = receivedMessages[0];

		//when
		Map<String, Object> parameters = getParameters(to, 5);
		connector.setInputParameters(parameters);

		connector.connect();
		Map<String, Object> execute = connector.execute();

		//then
		assertThat(execute).as("should have a response ").hasSize(1);

		List<Map> mails = (List<Map>) execute.get("mails");
		assertThat(mails).as("should have 1 email ").hasSize(5);

		Map<String, Object> firstMessage = mails.get(0);
		assertThat(firstMessage.get("subject")).as("should have a subject").isEqualTo(expectedMessage.getSubject());
		assertThat(firstMessage.get("from")).as("should have a sender").isEqualTo(expectedMessage.getFrom()[0].toString());
		assertThat(firstMessage.get("body")).as("should have a body message").isEqualTo(body);
	}

}