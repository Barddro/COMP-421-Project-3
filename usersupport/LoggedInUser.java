package usersupport;

public class LoggedInUser extends UserTemplate {
    private String username;
    private int artistID;

    LoggedInUser(String username, int artistID) {
        this.username = username;
        this.artistID = artistID;
    }

    @Override
    boolean validate() {
        return true;
    }

    @Override
    boolean isArtist() {
        return this.artistID != -1;
    }

    @Override
    String getUsername() {
    	return this.username;
    }

    @Override
    int getArtistID() {
        return this.artistID;
    }
}
