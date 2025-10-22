package uy.edu.tse.hcen.messaging;

public final class RabbitConfig {
    private RabbitConfig() {}

    public static String host() {
        return System.getenv().getOrDefault("RABBITMQ_HOST", "localhost");
    }

    public static int port() {
        try {
            return Integer.parseInt(System.getenv().getOrDefault("RABBITMQ_PORT", "5672"));
        } catch (Exception e) {
            return 5672;
        }
    }

    public static String user() {
        String u = System.getenv("RABBITMQ_USER");
        if (u == null || u.isEmpty()) {
            u = System.getenv("RABBITMQ_DEFAULT_USER");
        }
        if (u == null || u.isEmpty()) {
            throw new IllegalStateException("RABBITMQ_USER no configurado");
        }
        return u;
    }

    public static String pass() {
        String p = System.getenv("RABBITMQ_PASSWORD");
        if (p == null || p.isEmpty()) {
            p = System.getenv("RABBITMQ_DEFAULT_PASS");
        }
        if (p == null || p.isEmpty()) {
            throw new IllegalStateException("RABBITMQ_PASSWORD no configurado");
        }
        return p;
    }
}
