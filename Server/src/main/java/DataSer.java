import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DataSer {
    private static final String DB_URL = "jdbc:mysql://----------:3306/a2db1";
    private static final String DB_USER = "------------";
    private static final String DB_PASSWORD = "----------------";
    private final HikariDataSource connectionPool;
    public DataSer() {
        this.connectionPool = this.connect();
    }
    public HikariDataSource getConnectionPool() {
        return this.connectionPool;
    }
    private HikariDataSource connect() {
        HikariDataSource connectionPool = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(DB_URL);
            config.setUsername(DB_USER);
            config.setPassword(DB_PASSWORD);
            config.setMinimumIdle(10);
            config.setMaximumPoolSize(75);
            connectionPool = new HikariDataSource(config);
            System.out.println("connected to db");
        } catch (ClassNotFoundException e) {
            System.err.println("JDBC driver not found");
            e.printStackTrace();
        }
        return connectionPool;
    }
    protected void close() {
        if (this.connectionPool != null) {
            this.connectionPool.close();
            System.out.println("Connection closed");
        }
    }
}
