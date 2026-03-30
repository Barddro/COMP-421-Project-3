package usersupport;

import utils.DAO;
import utils.Security;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Wraps a UserTemplate to provide api to interface with user login, validation, etc, as well as neatly handle user login.
 */
public class User {

    private UserTemplate user;

    public static void createAccount(String username, String password, String name, String email, Date sqlDate) throws SQLException {
        String hashedPassword = Security.hashString(password);
        String query = "INSERT INTO User (username, password, name, email, dateOfBirth) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = DAO.getConnection().prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, hashedPassword);
            stmt.setString(3, name);
            stmt.setString(4, email);
            stmt.setDate(5, sqlDate);
            stmt.executeUpdate();
            System.out.println("Account created successfully! You can now log in.");
        }
        catch (SQLException e) {
            if(!DAO.checkDuplicateInsertion(e, "Account with username " + username + " already exists")) {
                System.err.println("Error: Failed to create account.");
                System.err.println("Code: " + e.getErrorCode() + " SQLState: " + e.getSQLState());
                System.err.println(e.getMessage());
            }
        }
    }

    public User() {
        this.user = new GuestUser();
    }

    public void login(String username, String password) throws SQLException {
        String hashedPassword = Security.hashString(password);
//        boolean isArtist = false;
        
        // previously written presumably for artist logins
//        String query = "SELECT u.username, a.artistid FROM User u " +
//                "LEFT JOIN Artists a " +
//                "ON u.username = a.username " +
//                "WHERE u.username = ? " +
//                "AND u.password = ?";

        String query = "SELECT username, artistID FROM User " +
                "WHERE username = ? " +
                "AND password = ?";
        try (PreparedStatement stmt = DAO.getConnection().prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, hashedPassword);
            ResultSet results = stmt.executeQuery();

            if (results.next()) {
                int artistID = results.getInt("artistID");
                if (results.wasNull()) artistID = -1;
                this.user = new LoggedInUser(username, artistID);
                System.out.println("Logged in! Welcome, " + username);
            } else {
                // no user found (invalid username/password)
                System.out.println("Incorrect username or password. Please try again");
            }

        }
        catch (SQLException e) {
            System.err.println("Error occurred when trying to log in. Please try again");
            System.err.println("Code: " + e.getErrorCode() + " SQLState: " + e.getSQLState());
            System.err.println(e.getMessage());
        }

    }

    public boolean validate() {
        return this.user.validate();
    }

    public boolean isArtist() {
        return this.user.isArtist();
    }
    
    public String getUsername() {
    	return this.user.getUsername();
    }

    public int getArtistID() {
        return this.user.getArtistID();
    }

    public void becomeArtist(String artistName) {
        Connection con = null;
        try {
            con = DAO.getConnection();
            con.setAutoCommit(false);

            String insertArtist = "INSERT INTO Artists(artistid, name) " +
                    "VALUES ((SELECT COALESCE(MAX(artistid), 0)+1 FROM Artists), ?)";
            try (PreparedStatement ps = con.prepareStatement(insertArtist)) {
                ps.setString(1, artistName);
                ps.executeUpdate();
            }

            int newArtistID;
            try (Statement stmt = con.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT MAX(artistid) FROM Artists")) {
                rs.next();
                newArtistID = rs.getInt(1);
            }

            String updateUser = "UPDATE User SET artistID = ? WHERE username = ?";
            try (PreparedStatement ps = con.prepareStatement(updateUser)) {
                ps.setInt(1, newArtistID);
                ps.setString(2, this.user.getUsername());
                ps.executeUpdate();
            }

            con.commit();
            this.user = new LoggedInUser(this.user.getUsername(), newArtistID);
            System.out.println("Your account is now an artist account!");

        } catch (SQLException e) {
            try {
                if (con != null) con.rollback();
            } catch (SQLException ex) {
                System.err.println("Rollback failed: " + ex.getMessage());
            }
            System.err.println("Failed to become an artist:");
            System.err.println("Code: " + e.getErrorCode() + " SQLState: " + e.getSQLState());
            System.err.println(e.getMessage());
        } finally {
            try {
                if (con != null) con.setAutoCommit(true);
            } catch (SQLException e) {
                System.err.println("Error resetting auto-commit.");
            }
        }
    }
}
