import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DbFix2 {
    public static void main(String[] args) throws Exception {
        Class.forName("org.postgresql.Driver");
        String url = "jdbc:postgresql://localhost:5432/fno_oms";
        String user = "fnooms";
        String pass = "fnooms123";
        
        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("INSERT INTO algo_key_value (key_name, key_value, updated_by) VALUES " +
                    "('mstock2.api_key', 'mock_api_key_mstock2', 'SYSTEM'), " +
                    "('mstock2.userid', 'mock_user_mstock2', 'SYSTEM'), " +
                    "('mstock2.pdcred', 'mock_pwd_mstock2', 'SYSTEM'), " +
                    "('mstock2.jwt_token', 'mock_jwt_mstock2', 'SYSTEM'), " +
                    "('mstock2.refresh_token', 'mock_rt_mstock2', 'SYSTEM') " +
                    "ON CONFLICT (key_name) DO UPDATE SET key_value = EXCLUDED.key_value");
                System.out.println("Set mstock2 credentials.");
            }
        }
    }
}
