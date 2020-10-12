package com.bonitasoft.connectors;

import org.bonitasoft.engine.connector.AbstractConnector;
import org.bonitasoft.engine.connector.ConnectorValidationException;

import java.util.List;

public abstract class AbstractEmailReceiverConnectorImpl extends AbstractConnector {

	protected static final String EMAIL_PROTOCOL_INPUT_PARAMETER = "emailProtocol";
	protected static final String EMAIL_SSL_ENABLED_INPUT_PARAMETER = "emailSSL";
	protected static final String EMAIL_FOLDER_NAME_INPUT_PARAMETER = "emailFolderName";
	protected final static String EMAIL_HOST_INPUT_PARAMETER = "emailHost";
	protected final static String EMAIL_PORT_INPUT_PARAMETER = "emailPort";
	protected final static String EMAIL_USERNAME_INPUT_PARAMETER = "emailUsername";
	protected final static String EMAIL_PASSWORD_INPUT_PARAMETER = "emailPassword";
	protected final static String EMAIL_BATCH_SIZE_INPUT_PARAMETER = "emailBatchSize";


	protected final static String MAILS_OUTPUT_PARAMETER = "mails";

	protected final String getEmailHost() {
		return getStringInputParameter(EMAIL_HOST_INPUT_PARAMETER);
	}

	protected final Integer getEmailBatchSize() {
		return getIntegerInputParameter(EMAIL_BATCH_SIZE_INPUT_PARAMETER);
	}

	private Integer getIntegerInputParameter(String emailBatchSizeInputParameter) {
		return (Integer) getInputParameter(emailBatchSizeInputParameter);
	}

	protected final String getEmailProtocol() {
		return getStringInputParameter(EMAIL_PROTOCOL_INPUT_PARAMETER);
	}

	private String getStringInputParameter(String parameterName) {
		return (String) getInputParameter(parameterName);
	}

	protected final String getEmailFolderName() {
		return getStringInputParameter(EMAIL_FOLDER_NAME_INPUT_PARAMETER);
	}

	protected final Boolean isEmailSslEnabled() {
		return getBooleanInputParameter(EMAIL_SSL_ENABLED_INPUT_PARAMETER);
	}

	private Boolean getBooleanInputParameter(String parameterName) {
		return (Boolean) getInputParameter(parameterName);
	}

	protected final Integer getEmailPort() {
		return getIntegerInputParameter(EMAIL_PORT_INPUT_PARAMETER);
	}


	protected final String getEmailUsername() {
		return getStringInputParameter(EMAIL_USERNAME_INPUT_PARAMETER);
	}

	protected final String getEmailPassword() {
		return getStringInputParameter(EMAIL_PASSWORD_INPUT_PARAMETER);
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
		try {
			isEmailSslEnabled();
		} catch (ClassCastException cce) {
			throw new ConnectorValidationException("emailPassword type is invalid");
		}

	}

}
