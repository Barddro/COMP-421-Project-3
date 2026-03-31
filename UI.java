import usersupport.User;

import java.sql.Date;
import java.sql.SQLException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Scanner;

class UI {
    public static Scanner s = new Scanner(System.in);
    public static User user = new User();

    public static boolean validateInputRange(int lo, int hi, int menuOption) {
        if (menuOption < lo || menuOption > hi) {
            System.out.println("Please choose a valid option");
            return false;
        }
        return true;
    }

    // Returns true if the user is logged in, false otherwise
    // If not logged in, prints the provided error message
    public static boolean verifyUserLogin(String error) {
        if (!user.validate()) {
            System.out.println(error);
            return false;
        }
        return true;
    }

    // useful for managing ux flow
    public static void waitForEnter() {
        System.out.println("Press ENTER to continue...");
        s.nextLine();
    }

    public static String getValidInput(String type) {
        while (true) {
            // Birthdate reads its own fields inside the case; all other types read a single line here
            String input = type.equals("birthdate") ? null : s.nextLine().trim();

            if (input != null && input.isEmpty()) {
                System.out.println("Input cannot be empty.");
                continue;
            }

            switch (type) {
                case "password":
                    if (input.matches("^\\S+$")) {
                        return input;
                    }
                    System.out.println("Password cannot contain spaces.");
                    break;

                case "email":
                    if (input.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
                        return input;
                    }
                    System.out.println("Please enter a valid email address.");
                    break;

                case "integer":
                    if (input.matches("^-?\\d+$")) {
                        return input;
                    }
                    System.out.println("Please enter a valid integer.");
                    break;

                case "birthdate":
                    try {
                        System.out.println("Please enter your birth year:");
                        int year = Integer.parseInt(s.nextLine().trim());

                        System.out.println("Please enter your birth month (1-12):");
                        int month = Integer.parseInt(s.nextLine().trim());

                        System.out.println("Please enter your birth day:");
                        int day = Integer.parseInt(s.nextLine().trim());

                        LocalDate date = LocalDate.of(year, month, day);

                        if (date.isAfter(LocalDate.now())) {
                            System.out.println("Date of birth cannot be in the future.");
                            break;
                        }

                        return date.toString();

                    } catch (NumberFormatException e) {
                        System.out.println("Please enter valid numbers for the date.");
                        break;
                    } catch (DateTimeException e) {
                        System.out.println("Invalid date. Please try again.");
                        break;
                    }

                default:
                    if (input.matches("^[a-zA-Z0-9 .,'&!?-]+$")) {
                        return input;
                    }
                    System.out.println("Input contains invalid characters. Only letters, numbers, spaces, and basic punctuation are allowed.");
                    break;
            }
        }
    }

    public static void inputLogin() throws SQLException {
        System.out.println("Please enter your username:");
        String username = s.next();

        System.out.println("Please enter your password:");
        String password = s.next();
        s.nextLine();

        UI.user.login(username, password);
        waitForEnter();
    }

    public static void inputCreateAccount() throws SQLException {
        System.out.println("Please enter your username:");
        String username = s.next();
        s.nextLine(); // consume trailing newline after s.next()

        String password;
        while (true) {
            System.out.println("Please enter your password:");
            password = getValidInput("password");

            System.out.println("Please re-enter your password:");
            String repassword = getValidInput("password");

            if (!password.equals(repassword)) {
                System.out.println("Passwords do not match. Please try again");
            } else {
                break;
            }
        }

        System.out.println("Please enter your email address:");
        String email = getValidInput("email");

        System.out.println("Please enter your name:");
        String name = getValidInput("any");

        Date sqlDate = Date.valueOf(getValidInput("birthdate"));

        User.createAccount(username, password, name, email, sqlDate);
    }

    public static void inputBecomeArtist() {
        if (!user.validate()) {
            System.out.println("You must be logged in to become an artist.");
            waitForEnter();
            return;
        }
        if (user.isArtist()) {
            System.out.println("Your account is already an artist account.");
            UI.s.nextLine();
            waitForEnter();
            return;
        }
        System.out.println("Please enter your artist name:");
        String artistName = getValidInput("any");
        user.becomeArtist(artistName);
        waitForEnter();
    }
}
