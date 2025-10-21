package uy.edu.tse.hcen.cli;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.Queue;
import jakarta.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Hashtable;

/**
 * Simple JMS producer that looks up a ConnectionFactory and Queue via JNDI
 * and sends a TextMessage to java:/jms/queue/NodoRegistrationQueue.
 *
 * To send a message to a running WildFly instance remotely, run with:
 *  -Dwildfly.remote=true -Dremote.host=localhost -Dremote.port=8080 -Dremote.user=<user> -Dremote.password=<pass>
 *
 * Example usage from the module:
 * mvn -f ejb/pom.xml exec:java -Dexec.mainClass=uy.edu.tse.hcen.cli.JmsProducer -Dexec.classpathScope=compile -Dexec.args="RUT:123;NOMBRE:Clinica X;URL:https://clinica-x/simulate-ok"
 */
public class JmsProducer {

    public static void main(String[] args) throws Exception {
        String messageText = "RUT:123;NOMBRE:Clinica X;URL:https://clinica-x/simulate-ok;DEPARTAMENTO:MONTEVIDEO";
        if (args != null && args.length > 0) {
            messageText = String.join(" ", args);
        }

        String connFactoryJndi = System.getProperty("jms.connectionFactory", "java:/ConnectionFactory");
        String queueJndi = System.getProperty("jms.queue", "java:/jms/queue/NodoRegistrationQueue");

        boolean remote = Boolean.parseBoolean(System.getProperty("wildfly.remote", "false"));
        Hashtable<String, Object> env = new Hashtable<>();
        if (remote) {
            String host = System.getProperty("remote.host", "localhost");
            String port = System.getProperty("remote.port", "8080");
            env.put(Context.INITIAL_CONTEXT_FACTORY, "org.wildfly.naming.client.WildFlyInitialContextFactory");
            env.put(Context.PROVIDER_URL, "http-remoting://" + host + ":" + port);
            String user = System.getProperty("remote.user");
            String password = System.getProperty("remote.password");
            if (user != null) env.put(Context.SECURITY_PRINCIPAL, user);
            if (password != null) env.put(Context.SECURITY_CREDENTIALS, password);
        }

        InitialContext ic = new InitialContext(env);
        try {
            Object cfObj = ic.lookup(connFactoryJndi);
            Object qObj = ic.lookup(queueJndi);
            if (!(cfObj instanceof ConnectionFactory)) {
                System.err.println("Lookup did not return ConnectionFactory for: " + connFactoryJndi + " (got: " + cfObj + ")");
            }
            if (!(qObj instanceof Queue)) {
                System.err.println("Lookup did not return Queue for: " + queueJndi + " (got: " + qObj + ")");
            }
            ConnectionFactory cf = (ConnectionFactory) cfObj;
            Queue queue = (Queue) qObj;

            try (JMSContext jmsCtx = createContext(cf)) {
                TextMessage tm = jmsCtx.createTextMessage(messageText);
                jmsCtx.createProducer().send(queue, tm);
                System.out.println("Sent message to " + queueJndi + ": " + messageText);
            }
        } finally {
            try { ic.close(); } catch (Exception ignored) {}
        }
    }

    private static JMSContext createContext(ConnectionFactory cf) {
        String user = System.getProperty("remote.user");
        String password = System.getProperty("remote.password");
        if (user != null && password != null) {
            return cf.createContext(user, password);
        }
        return cf.createContext();
    }
}