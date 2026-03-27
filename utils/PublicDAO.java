package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DAO for handling database access with a singleton connection
 */
public class PublicDAO {
    private static Connection connection;

    public static Connection getConnection(String url, String user, String pass) throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(url, user, pass);
        }
        return connection;
    }

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            throw new SQLException("Connection cannot be established because no connection information given");
        }
        return connection;
    }

    public static void close() throws SQLException {
        PublicDAO.connection.close();
    }
}
