package usersupport;

import utils.DAO;
import utils.Security;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Wraps a UserTemplate to provide api to interface with user login, validation, etc, as well as neatly handle user login.
 */
public class User {

    private UserTemplate user;

    public static void createAccount(String username, String password, String name, String email, Date sqlDate) throws SQLException {
        password = Security.hashString(password);
        String query = "INSERT INTO User (username, password, name, email, dateOfBirth) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = DAO.getConnection().prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setString(3, name);
            stmt.setString(4, email);
            stmt.setDate(5, sqlDate);
            stmt.executeUpdate();
        }
        catch (SQLException e) {
            if(!DAO.checkDuplicateInsertion(e, "Account with username " + username + "already exists")) {
                System.out.println("Error: Failed to create account.");
            }
        }
    }

    public User() {
        this.user = new GuestUser();
    }

    public void login(String username, String password) throws SQLException {
        password = Security.hashString(password);
        boolean isArtist = false;

        // do a left join on userid, artistid to get all info, then we can check if userid is null or not
        String query = "SELECT u.username, a.artistid FROM User u " +
                "LEFT JOIN Artists a " +
                "ON u.username = a.username " +
                "WHERE u.username = ? " +
                "AND u.password = ?";
        try (PreparedStatement stmt = DAO.getConnection().prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet results = stmt.executeQuery();

            if (results.next()) {
                // user exists
                int artistId = results.getInt("artistid");
                if (!results.wasNull()) {
                    // artistid was not null, so user is an artist
                    isArtist = true;
                }
                this.user = new LoggedInUser(username, isArtist);
                System.out.println("Logged in! Welcome, " + username);
            } else {
                // no user found (invalid username/password)
                System.out.println("Incorrect username or password. Please try again");
            }
        }
        catch (SQLException e) {
            System.out.println("Error occurred when trying to log in. Please try again");
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
}
