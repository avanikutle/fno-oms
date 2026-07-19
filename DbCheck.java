import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DbCheck {
    public static void main(String[] args) throws Exception {
        Class.forName("org.postgresql.Driver");
        String url = "jdbc:postgresql://localhost:5432/fno_oms";
        String user = "fnooms";
        String pass = "fnooms123";
        
        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT key_name, key_value FROM algo_key_value WHERE key_name LIKE '%mstock2%'");
                while (rs.next()) {
                    System.out.println(rs.getString("key_name") + " = " + rs.getString("key_value"));
                }
            }
        }
    }
}
