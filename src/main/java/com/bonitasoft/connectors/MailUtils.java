package com.bonitasoft.connectors;

import com.sun.mail.imap.IMAPBodyPart;
import org.apache.commons.lang3.StringUtils;
import org.bonitasoft.engine.bpm.document.DocumentValue;

import javax.mail.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class MailUtils {

	Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

	protected String getBody(Message message) throws MessagingException, IOException {
		Object content = message.getContent();
		if (content instanceof String) {
			return (String) content;
		}
		if (content instanceof Multipart) {
			Multipart multipart = (Multipart) content;
			for (int i = 0; i < multipart.getCount(); i++) {
				BodyPart part = multipart.getBodyPart(i);
				if (part instanceof IMAPBodyPart) {
					String contentType = part.getContentType();
					String bodyContent = part.getContent().toString();
					logger.info("body content type:" + contentType);
					logger.info("body content :" + bodyContent);
					return bodyContent;
				}
			}
		}
		return null;
	}

	protected List<DocumentValue> getAttachments(Message message) throws Exception {
		List<DocumentValue> result = new ArrayList<>();
		Object content = message.getContent();
		if (content instanceof Multipart) {
			Multipart multipart = (Multipart) content;
			for (int i = 0; i < multipart.getCount(); i++) {
				result.addAll(getAttachments(multipart.getBodyPart(i)));
			}
		}
		return result;
	}

	private List<DocumentValue> getAttachments(BodyPart part) throws Exception {
		List<DocumentValue> result = new ArrayList<>();
		Object content = part.getContent();
		if (content instanceof InputStream || content instanceof String) {
			if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()) || StringUtils.isNotBlank(part.getFileName())) {
				String mimeType = part.getContentType().split(";")[0];
				DocumentValue documentValue = new DocumentValue(toByteArray(part.getInputStream()), mimeType, part.getFileName());
				result.add(documentValue);
				return result;
			} else {
				return result;
			}
		}

		if (content instanceof Multipart) {
			Multipart multipart = (Multipart) content;
			for (int i = 0; i < multipart.getCount(); i++) {
				BodyPart bodyPart = multipart.getBodyPart(i);
				result.addAll(getAttachments(bodyPart));
			}
		}
		return result;
	}

	private byte[] toByteArray(InputStream inputStream) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		int nRead;
		byte[] data = new byte[1024];
		while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
			buffer.write(data, 0, nRead);
		}
		buffer.flush();
		byte[] byteArray = buffer.toByteArray();
		return byteArray;
	}
}
