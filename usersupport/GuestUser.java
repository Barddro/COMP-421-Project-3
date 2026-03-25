package usersupport;

public class GuestUser extends UserTemplate {
    // here, we implement the 'null pattern'. When a user tries to validate, we automatically return false
    boolean validate() {
        return false;
    }

    @Override
    boolean isArtist() {
        return false;
    }

}
