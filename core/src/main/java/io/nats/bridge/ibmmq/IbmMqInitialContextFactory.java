package io.nats.bridge.ibmmq;

import javax.naming.*;
import javax.naming.spi.InitialContextFactory;
import java.util.Hashtable;

import com.ibm.msg.client.jms.JmsFactoryFactory;
import com.ibm.msg.client.wmq.WMQConstants;
import javax.jms.*;



public class IbmMqInitialContextFactory implements InitialContextFactory {

    @Override
    public Context getInitialContext(Hashtable<?, ?> hashtable) throws NamingException {

        try {
            JmsFactoryFactory instance = JmsFactoryFactory.getInstance(WMQConstants.WMQ_PROVIDER);

            return null;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new NamingException("Name not found"); //TODO something better than this
        }
    }

    private static class MQContext implements Context {


        @Override
        public Object lookup(String s) throws NamingException {
            return null;
        }

        @Override
        public Object lookup(Name name) throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }


        @Override
        public void bind(Name name, Object o) throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public void bind(String s, Object o) throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public void rebind(Name name, Object o) throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public void rebind(String s, Object o) throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public void unbind(Name name) throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public void unbind(String s) throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public void rename(Name name, Name name1) throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public void rename(String s, String s1) throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public NamingEnumeration<NameClassPair> list(String s) throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public NamingEnumeration<Binding> listBindings(String s) throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public void destroySubcontext(Name name) throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public void destroySubcontext(String s) throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public Context createSubcontext(Name name) throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public Context createSubcontext(String s) throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public Object lookupLink(Name name) throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public Object lookupLink(String s) throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public NameParser getNameParser(Name name) throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public NameParser getNameParser(String s) throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public Name composeName(Name name, Name name1) throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public String composeName(String s, String s1) throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public Object addToEnvironment(String s, Object o) throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public Object removeFromEnvironment(String s) throws NamingException {
            return null;
        }

        @Override
        public Hashtable<?, ?> getEnvironment() throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public void close() throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public String getNameInNamespace() throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }
    }
}
