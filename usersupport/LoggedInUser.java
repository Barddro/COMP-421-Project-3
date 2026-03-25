package usersupport;

public class LoggedInUser extends UserTemplate {
    private String username;
    private boolean isArtist;

    LoggedInUser(String username, boolean isArtist) {
        this.username = username;
        this.isArtist = isArtist;
    }

    @Override
    boolean validate() {
        return true;
    }

    @Override
    boolean isArtist() {
        return this.isArtist;
    }

    // may want to move artist functionality here for simplicity, and just have a validateArtist method, or an account-type field
}
