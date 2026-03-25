package usersupport;

public class User {

    private UserTemplate user;

    /**
     * Takes in plain-text password and returns salted, hashed version for validation against database.
     * @param password  Plain-text password to salt/hash.
     * @return Salted, hashed password.
     */
    public static String hashPassword(String password) {
        //TODO: Implement function
        return password;
    }


    private static void createAccount(String username, String password) {
        password = hashPassword(password);

        // check if unique id already exists in db
    }

    public User() {
        this.user = new GuestUser();
    }

    public boolean login(String username, String password) {
        boolean valid = false;
        boolean isArtist = false;
        // do the join on artists HERE!
        // -> If user doesn't exist period, remain as guest user and print error/throw error
        // -> If user is a standard user but not an artist, set isArtist to false
        // -> Else if user IS an artist as well, set isArtist to true

        // Now that user info has been verified:
        if (valid) {
            this.user = new LoggedInUser(username, isArtist);
            System.out.println("Logged in! Welcome, " + username);
            return true;
        }
        System.out.println("Failed to log in. Invalid username or password");
        return false;
    }

    public boolean validate() {
        return this.user.validate();
    }

    public boolean isArtist() {
        return this.user.isArtist();
    }
}
