package usersupport;

import utils.DAO;
import utils.Security;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Wraps a UserTemplate to provide api to interface with user login, validation, etc, as well as neatly handle user login.
 */
public class User {

    private UserTemplate user;

    /**
     * Takes in plain-text password and returns hashed version for validation against database.
     * @param password  Plain-text password to hash.
     * @return Hashed password.
     */


    private static void createAccount(String username, String password) {
        password = Security.hashString(password);

        // check if username already exists in db. If not, insert info into db
    }

    public User() {
        this.user = new GuestUser();
    }

    public void login(String username, String password) throws SQLException {
        password = Security.hashString(password);
        boolean isArtist = false;

        // do a left join on userid, artistid to get all info, then we can check if userid is null or not
        String query = "SELECT u.username, a.artistid FROM User u" +
                "LEFT JOIN Artists a" +
                "ON u.username = a.username" +
                "WHERE u.username = ?" +
                "AND u.password = ?;";
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
}
