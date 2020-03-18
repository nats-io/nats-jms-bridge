package io.nats.bridge;

public class StringMessage implements Message {

    private final String body;

    public StringMessage(String body) {
        this.body = body;
    }


    //TODO implement.
    @Override
    public void reply(final Message reply) {

    }

    public String getBody() {
        return body;
    }
}
