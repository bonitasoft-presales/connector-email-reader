package com.bonitasoft.connectors;

import org.apache.commons.lang3.StringUtils;
import org.bonitasoft.engine.bpm.document.DocumentValue;
import org.bonitasoft.engine.connector.ConnectorException;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.DataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MailUtils {

    Logger logger = LoggerFactory.getLogger(EmailReceiverConnectorImpl.class);

    private String getTextFromMimeMultipart(
            MimeMultipart mimeMultipart) throws MessagingException, IOException {
        String result = "";
        int count = mimeMultipart.getCount();
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain")) {
                result = result + "\n" + bodyPart.getContent();
                break; // without break same text appears twice in my tests
            } else if (bodyPart.isMimeType("text/html")) {
                String html = (String) bodyPart.getContent();
                //result = result + "\n" + org.jsoup.Jsoup.parse(html).text();
                result += html;
            } else if (bodyPart.getContent() instanceof MimeMultipart) {
                result = result + getTextFromMimeMultipart((MimeMultipart) bodyPart.getContent());
            }
        }
        return result;
    }

    protected String sanitize(String htmlContent) {
        String messageWithoutSignature = removeSignature(htmlContent);

        PolicyFactory policy = new HtmlPolicyBuilder()
                .allowAttributes("src").onElements("img")
                .allowAttributes("href").onElements("a")
                .allowStandardUrlProtocols()
                .allowElements(
                        "a", "img"
                ).toFactory();

        return policy.sanitize(messageWithoutSignature);
    }

    protected String removeSignature(String htmlContent) {
        String[] split = htmlContent.split("--");
        return split[0];
    }

    protected String getBody(Message message) throws MessagingException, IOException, ConnectorException {
        StringBuffer buffer = new StringBuffer();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Object content = getMessageContent(message);
        if (content instanceof String) {
            return (String) content;
        }
        if (content instanceof Multipart) {
            Multipart multipart = (Multipart) content;
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart part = multipart.getBodyPart(i);
                String contentType = part.getContentType();
                if (part.getFileName() != null) {
                    logger.info("skip part:[" + part.getFileName() + "]");
                } else {
                    logger.info("body content type:[" + contentType + "] - [" + part.getClass().getCanonicalName() + "]");
                    part.writeTo(byteArrayOutputStream);
                }
            }
        }
        byteArrayOutputStream.flush();
        byteArrayOutputStream.close();
        String result = new String(byteArrayOutputStream.toByteArray());
        logger.info("message:" + result);
        return result;
    }

    private Object getMessageContent(Message message) throws IOException, MessagingException {
        try {
            return message.getContent();
        } catch (MessagingException e) {
            // handling the bug
            if (message instanceof MimeMessage && "Unable to load BODYSTRUCTURE".equalsIgnoreCase(e.getMessage())) {
                return new MimeMessage((MimeMessage) message).getContent();
            } else {
                throw e;
            }
        }
    }


    protected List<DocumentValue> getAttachments(Message message) throws Exception {
        List<DocumentValue> result = new ArrayList<>();
        Object content = getMessageContent(message);
        if (content instanceof Multipart) {
            Multipart multipart = (Multipart) content;
            for (int i = 0; i < multipart.getCount(); i++) {
                result.addAll(getAttachments(multipart.getBodyPart(i)));
            }
        }
        return result;
    }

    protected Optional<DocumentValue> getDocumentValue(DataSource dataSource) throws IOException {
        DocumentValue documentValue = null;
        if (dataSource.getName() != null) {
            documentValue = new DocumentValue(toByteArray(dataSource.getInputStream()), dataSource.getContentType(), dataSource.getName());
        }
        Optional<DocumentValue> a = Optional.ofNullable(documentValue);
        return a;
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

    public void logSummary(String subject, int messageNumber, String from, List<DocumentValue> attachments) {
        StringBuilder builder = new StringBuilder()
                .append("add message with id[")
                .append(messageNumber)
                .append("] | subject[")
                .append(subject)
                .append("] |")
                .append("from:[")
                .append(from)
                .append("] | attachments:");
        for (DocumentValue attachment : attachments) {
            builder.append("[")
                    .append(attachment.getMimeType())
                    .append(" | ")
                    .append(attachment.getFileName())
                    .append("]");
        }
        logger.info(builder.toString());
    }

    public void logEmail(Map<String, Object> map) {
        logger.debug("email content:");
        for (String key : map.keySet()) {
            StringBuilder builder = getMailField(key, map.get(key));
            logger.debug(builder.toString());
        }
    }

    private StringBuilder getMailField(String key, Object value) {
        Object displayed = value;
        switch (key) {
            case "plainContent":
            case "htmlContentSanitized":
            case "body":
            case "htmlContent":
                displayed = " - not displayed - ";
                break;
        }

        StringBuilder builder = new StringBuilder().
                append("[")
                .append(key)
                .append("] : [")
                .append(displayed)
                .append("]");
        return builder;
    }


}
