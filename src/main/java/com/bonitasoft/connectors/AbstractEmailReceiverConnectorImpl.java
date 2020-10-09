package com.bonitasoft.connectors;

import org.bonitasoft.engine.connector.AbstractConnector;
import org.bonitasoft.engine.connector.ConnectorValidationException;

import java.util.List;

public abstract class AbstractEmailReceiverConnectorImpl extends AbstractConnector {

	protected final static String EMAIL_HOST_INPUT_PARAMETER = "emailHost";
	protected final static String EMAIL_PORT_INPUT_PARAMETER = "emailPort";
	protected final static String EMAIL_USERNAME_INPUT_PARAMETER = "emailUsername";
	protected final static String EMAIL_PASSWORD_INPUT_PARAMETER = "emailPassword";
	protected final static String MAILS_OUTPUT_PARAMETER = "mails";

	protected final String getEmailHost() {
		return (String) getInputParameter(EMAIL_HOST_INPUT_PARAMETER);
	}

	protected final int getEmailPort() {
		return Integer.parseInt((String) getInputParameter(EMAIL_PORT_INPUT_PARAMETER));
	}

	protected final String getEmailUsername() {
		return (String) getInputParameter(EMAIL_USERNAME_INPUT_PARAMETER);
	}

	protected final String getEmailPassword() {
		return (String) getInputParameter(EMAIL_PASSWORD_INPUT_PARAMETER);
	}

	protected final void setMails(List mails) {
		setOutputParameter(MAILS_OUTPUT_PARAMETER, mails);
	}

	@Override
	public void validateInputParameters() throws ConnectorValidationException {
		try {
			getEmailHost();
		} catch (ClassCastException cce) {
			throw new ConnectorValidationException("emailHost type is invalid");
		}
		try {
			getEmailUsername();
		} catch (ClassCastException cce) {
			throw new ConnectorValidationException("emailUsername type is invalid");
		}
		try {
			getEmailPassword();
		} catch (ClassCastException cce) {
			throw new ConnectorValidationException("emailPassword type is invalid");
		}

	}

}
