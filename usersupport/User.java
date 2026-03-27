package usersupport;

import utils.PublicDAO;
import utils.Security;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class User {

    private UserTemplate user;

    /**
     * Takes in plain-text password and returns hashed version for validation against database.
     * @param password  Plain-text password to hash.
     * @return Hashed password.
     */


    private static void createAccount(String username, String password) {
        password = Security.hashString(password);


        // check if unique id already exists in db
    }

    public User() {
        this.user = new GuestUser();
    }

    public void login(String username, String password) throws SQLException {
        password = Security.hashString(password);
        boolean isArtist = false;
        // do the join on artists HERE!
        // -> If user doesn't exist period, remain as guest user and print error/throw error
        // -> If user is a standard user but not an artist, set isArtist to false
        // -> Else if user IS an artist as well, set isArtist to true

        // do a left join on userid, artistid to get all info, then we can check if userid is null or not
        String query = "SELECT u.username, a.artistid FROM User u" +
                "LEFT JOIN Artists a" +
                "ON u.username = a.username" +
                "WHERE u.username = ?" +
                "AND u.password = ?";
        try (PreparedStatement stmt = PublicDAO.getConnection().prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet myRs = stmt.executeQuery();

            if (myRs.next()) {
                // user exists
                int artistId = myRs.getInt("artistid");
                if (!myRs.wasNull()) {
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
