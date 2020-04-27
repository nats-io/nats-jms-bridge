package io.nats.bridge.mock;

import javax.jms.JMSException;
import javax.jms.TextMessage;
import java.nio.charset.StandardCharsets;

public class JMSTextMessage extends JMSMessage implements TextMessage {

    private String text;

    public JMSTextMessage(String text) {
        super(text.getBytes(StandardCharsets.UTF_8));
        this.text = text;
    }

    @Override
    public String getText() throws JMSException {
        return text;
    }

    @Override
    public void setText(String string) throws JMSException {
        super.setBody(text.getBytes(StandardCharsets.UTF_8));
        text = string;
    }
}
