package com.bonitasoft.connectors;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.mail.internet.MimeMessage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.bonitasoft.connectors.AbstractEmailReceiverConnectorImpl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class EmailReceiverConnectorImplTest {

	@RegisterExtension
	static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.ALL);


	EmailReceiverConnectorImpl connector;

	@BeforeEach
	void setUp() {
		connector = new EmailReceiverConnectorImpl();
	}

	@Test
	@DisplayName("Should read imap email")
	void shouldReadMail() throws Exception {
		//given
		String to = "to@localhost";
		String body = "some body";
		GreenMailUtil.sendTextEmailTest(to, "from@localhost", "some subject", body);
		final MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
		final MimeMessage receivedMessage = receivedMessages[0];

		//when
		Map<String, Object> parameters = new HashMap<>();
		parameters.put(EMAIL_HOST_INPUT_PARAMETER, "127.0.0.1");
		int port = greenMail.getImap().getPort();
		parameters.put(EMAIL_PORT_INPUT_PARAMETER, Integer.toString(port));
		parameters.put(EMAIL_USERNAME_INPUT_PARAMETER, to);
		parameters.put(EMAIL_PASSWORD_INPUT_PARAMETER, to);
		connector.setInputParameters(parameters);

		connector.connect();
		Map<String, Object> execute = connector.execute();

		//then
		assert execute.size() == 1;
		List<Map> mails = (List<Map>) execute.get("mails");
		assert mails.size() == 1;

		Map<String, Object> firstMessage = mails.get(0);
		assertEquals(firstMessage.get("subject"), receivedMessage.getSubject());
		assertEquals(firstMessage.get("from"), receivedMessage.getFrom()[0].toString());
		assertEquals(firstMessage.get("body"), body);

		assertEquals(body, GreenMailUtil.getBody(receivedMessage));
	}


}