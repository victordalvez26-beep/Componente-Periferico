package uy.edu.tse.hcen.messaging;

/**
 * Small helper to centralize RabbitMQ configuration (env vars + defaults).
 */
public final class RabbitConfig {

    public static final String ENV_HOST = "RABBITMQ_HOST";
    public static final String ENV_PORT = "RABBITMQ_PORT";
    public static final String ENV_USER = "RABBITMQ_USER";
    public static final String ENV_PASS = "RABBITMQ_PASSWORD";
    public static final String ENV_DOCKER_USER = "RABBITMQ_DEFAULT_USER";
    public static final String ENV_DOCKER_PASS = "RABBITMQ_DEFAULT_PASS";
    public static final String ENV_URI = "RABBITMQ_URI";

    private RabbitConfig() {}

    public static String host() {
        return System.getenv().getOrDefault(ENV_HOST, "localhost");
    }

    public static int port() {
        try {
            return Integer.parseInt(System.getenv().getOrDefault(ENV_PORT, "5672"));
        } catch (Exception e) {
            return 5672;
        }
    }

    public static String user() {
        String u = System.getenv(ENV_USER);
        if (u != null && !u.isEmpty()) return u;
        // fallback to docker-compose default env var
        u = System.getenv(ENV_DOCKER_USER);
        if (u != null && !u.isEmpty()) return u;
        throw new IllegalStateException("RABBITMQ_USER no configurado");
    }

    public static String pass() {
        String p = System.getenv(ENV_PASS);
        if (p != null && !p.isEmpty()) return p;
        // fallback to docker-compose default env var
        p = System.getenv(ENV_DOCKER_PASS);
        if (p != null && !p.isEmpty()) return p;
        throw new IllegalStateException("RABBITMQ_PASSWORD no configurado");
    }

    public static String uri() {
        String envUri = System.getenv(ENV_URI);
        if (envUri != null && !envUri.isEmpty()) return envUri;
        // Build amqp URI and include credentials if present
        String user = user();
        String pass = pass();
        String host = host();
        int port = port();
        if (user != null && pass != null) {
            return "amqp://" + urlEncode(user) + ":" + urlEncode(pass) + "@" + host + ":" + port;
        }
        return "amqp://" + host + ":" + port;
    }

    private static String urlEncode(String s) {
        try {
            return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            return s;
        }
    }
}
