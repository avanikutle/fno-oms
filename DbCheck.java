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
                ResultSet rs = stmt.executeQuery("SELECT instrument_token, tradingsymbol FROM mstock_scrip_master WHERE tradingsymbol LIKE 'NIFTY2680418900%' LIMIT 5");
                while (rs.next()) {
                    System.out.println(rs.getString("instrument_token") + " -> " + rs.getString("tradingsymbol"));
                }
            }
        }
    }
}
