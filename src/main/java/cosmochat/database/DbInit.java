package cosmochat.database;

public final class DbInit {
    public static void main(String[] args) throws Exception {
        System.out.println("sys.cosmochat.db.url=" + System.getProperty("cosmochat.db.url"));
        System.out.println("sys.cosmochat.db.user=" + System.getProperty("cosmochat.db.user"));
        String pwProp = System.getProperty("cosmochat.db.password");
        System.out.println("sys.cosmochat.db.password=" + (pwProp == null ? "null" : "***"));

        System.out.println("env.COSMOCHAT_DB_URL=" + System.getenv("COSMOCHAT_DB_URL"));
        System.out.println("env.COSMOCHAT_DB_USER=" + System.getenv("COSMOCHAT_DB_USER"));
        String pwEnv = System.getenv("COSMOCHAT_DB_PASSWORD");
        System.out.println("env.COSMOCHAT_DB_PASSWORD=" + (pwEnv == null ? "null" : "***"));

        DatabaseManager.getInstance();
        System.out.println("DB OK");
    }
}
