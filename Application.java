import usersupport.User;
import utils.DAO;

import java.sql.* ;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

class Application {
    public static Scanner s = new Scanner(System.in);
    // can we optimize by letting genre be an int, then using a mapping int -> genreName @ the application level?

    public static User user = new User();

    // ------------ Helpers ------------ //

    public static boolean validateInputRange(int lo, int hi, int menuOption) {
        if (menuOption < lo || menuOption > hi) {
            System.out.println("Please choose a valid option");
            return false;
        }
        return true;
    }

    /**
     * Returns true if the user is logged in, false otherwise.
     * If not logged in, prints the provided error message.
     */
    public static boolean verifyUserLogin(String error) {
        if (!user.validate()) {
            System.out.println(error);
            return false;
        }
        return true;
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


    // ------------ Functions representing command line options, organized in the following way: ------------ //
    //  input<OptionName>: gets all necessary data from command line and calls exec method
    //  exec<OptionName>: execs database reads/writes after getting input from user

    public static void inputLogin() throws SQLException {
        System.out.println("Please enter your username:");
        String username = s.next();

        System.out.println("Please enter your password:");
        String password = s.next();

        Application.user.login(username, password);
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

        System.out.println("Please enter your birth date:");
        Date sqlDate = Date.valueOf(getValidInput("birthdate"));

        User.createAccount(username, password, name, email, sqlDate);
    }


    public static void inputUploadNewAlbum() {
        if (!user.isArtist()) {
            System.out.println("Please log in with a valid Artist account before uploading an album");
        }
        System.out.println("Please enter the name of the album:");
        String albumTitle = s.next();

        boolean validGenre = false;
        String albumGenre = "";

        while (!validGenre) {
            System.out.println("Please enter the genre of the album from the following list:\n"
                    + String.join("\n", Genres.getGenres())
            );

            albumGenre = (s.next()).toLowerCase();
            validGenre = Genres.validateGenre(albumGenre);
        }

        boolean validArtist = false;
        String valid;


        //... repeat for each field!
        String artist = ""; // TODO: collect artist input (stub placeholder)
        String[] songs = new String[0]; // TODO: collect songs input (stub placeholder)
        execUploadNewAlbum(albumTitle, albumGenre, artist, songs);

    }

    public static void execUploadNewAlbum(String albumTitle, String genre, String artist, String[] songs) {


    }

    public static void inputTopAlbumsByGenre() throws SQLException {
        System.out.println("Choose ranking type:");
        System.out.println("1. By stream count");
        System.out.println("2. By sales");

        int rankingChoice = s.nextInt();
        s.nextLine();
        if (!validateInputRange(1, 2, rankingChoice)) {
            return;
        }

        ArrayList<String> genres = new ArrayList<>(Genres.getGenres());
        Collections.sort(genres);

        System.out.println("Please choose a genre from the following list:");
        for (int i = 0; i < genres.size(); i++) {
            System.out.println((i + 1) + ". " + genres.get(i));
        }

        int genreChoice = s.nextInt();
        s.nextLine();
        if (!validateInputRange(1, genres.size(), genreChoice)) {
            return;
        }
        String selectedGenre = genres.get(genreChoice - 1);

        execTopAlbumsByGenre(selectedGenre, rankingChoice);
    }

    public static void execTopAlbumsByGenre(String genre, int rankingChoice) throws SQLException {
        Connection con = DAO.getConnection();

        String query = "";
        if (rankingChoice == 1) {
            query = """
                    SELECT a.title, COALESCE(SUM(s.nStreams), 0) AS totalStreams
                    FROM Album a
                    JOIN isOf i ON a.productID = i.productID
                    JOIN Songs so ON a.productID = so.productID
                    LEFT JOIN Streams s
                    	ON so.songTitle = s.songTitle AND so.productID = s.productID
                    WHERE i.name = ?
                    GROUP BY a.productID, a.title
                    ORDER BY totalStreams DESC
                    FETCH FIRST 10 ROWS ONLY
                    """;
        } else {
            query = """
                    SELECT a.title, COALESCE(SUM(p.price), 0) AS totalSales
                    FROM Album a
                    JOIN isOf i ON a.productID = i.productID
                    JOIN Purchase pu ON a.productID = pu.productID
                    JOIN Products p ON a.productID = p.productID
                    WHERE i.name = ?
                    GROUP BY a.productID, a.title
                    ORDER BY totalSales DESC
                    FETCH FIRST 10 ROWS ONLY
                    """;
        }
        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, genre);
            ResultSet rs = ps.executeQuery();
            System.out.println("\nTop albums for genre: " + genre);

            int i = 1;
            while (rs.next()) {
                String title = rs.getString("title");
                if (rankingChoice == 1) {
                    int streams = rs.getInt("totalStreams");
                    System.out.println(i + ". " + title + "-> " + streams + " streams");
                } else {
                    double sales = rs.getDouble("totalSales");
                    System.out.println(i + ". " + title + "-> $" + sales);
                }
                i++;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void inputPurchaseAlbum() throws SQLException {
        if (!user.validate()) {
            System.out.println("You must be logged in to make a purchase.");
            return;
        }

        System.out.println("Choose a filter method:");
        System.out.println("1. Search by name");
        System.out.println("2. Search by artist");
        System.out.println("3. Search by best-sellers");

        int choice = s.nextInt();
        s.nextLine();

        if (!validateInputRange(1, 3, choice)) return;

        ArrayList<String> albums = new ArrayList<>();

        switch (choice) {
            case 1:
                albums = filterAlbumsByName();
            case 2:
                albums = filterAlbumsByArtist();
            case 3:
                albums = filterAlbumsByBestselling();
            default:
                System.out.println("Invalid choice.");
        }
        if (albums.isEmpty()) {
            System.out.println("No albums found.");
            return;
        }
        System.out.println("Filter results:");
        for (int i = 0; i < albums.size(); i++) {
            System.out.println((i + 1) + ". " + albums.get(i));
        }
        System.out.println("Select album number to s: ");
        int albumChoice = s.nextInt();
        s.nextLine();

        if (!validateInputRange(1, albums.size(), albumChoice)) {
            return;
        }

        String selectedAlbum = albums.get(albumChoice - 1);
        executeAlbumPurchase(selectedAlbum);
    }

    public static void executeAlbumPurchase(String selectedAlbum) {
        Connection con = null;

        try {
            con = DAO.getConnection();
            con.setAutoCommit(false);

            String getAlbumQuery = """
                    SELECT a.productID, p.price
                    From Album a
                    JOIN Products p ON a.productID = p.productID
                    WHERE a.title = ?
                    """;
            int transactionID = -1;
            int productID = -1;
            double price = -1;

            try (PreparedStatement ps = con.prepareStatement(getAlbumQuery)) {
                ps.setString(1, selectedAlbum);
                ResultSet rs = ps.executeQuery();

                if (!rs.next()) {
                    throw new SQLException("Album not found.");
                }
                productID = rs.getInt("productID");
                price = rs.getDouble("price");
            }

            //Make the next product ID
            String getMaxID = "SELECT COALESCE(MAX(transactionID), 0) FROM Transactions";
            try (Statement stmt = con.createStatement();
                 ResultSet rs = stmt.executeQuery(getMaxID)) {
                rs.next();
                transactionID = rs.getInt(1) + 1;
            }

            //Now we can insert into Transactions
            String insertTransaction = """
                    INSERT INTO Transactions (transactionID, price, date)
                    VALUES (?, ?, CURRENT_DATE)
                    """;
            try (PreparedStatement ps = con.prepareStatement(insertTransaction)) {
                ps.setInt(1, transactionID);
                ps.setDouble(2, price);
                ps.executeUpdate();

                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) {
                    transactionID = keys.getInt(1);
                } else {
                    throw new SQLException("Failed to retrieve transactionID.");
                }
            }

            String insertpurchase = """
                    INSERT INTO purchase (transactionID, username, productID)
                    VALUES (?, ?, ?)
                    """;

            try (PreparedStatement ps = con.prepareStatement(insertpurchase)) {
                ps.setInt(1, transactionID);
                ps.setString(2, user.getUsername());
                ps.setInt(3, productID);
                ps.executeUpdate();
            }
            //Success
            con.commit();
            System.out.println("Successfully purchased album: " + selectedAlbum);
        } catch (SQLException e) {
            try {
                if (con != null) con.rollback(); //undo any faults
            } catch (SQLException ex) {
                System.err.println("Rollback failed: " + ex.getMessage());
            }
            System.err.println("purchase failed:");
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

    public static ArrayList<String> filterAlbumsByName() throws SQLException {
        System.out.println("Enter a keyword to search album titles");
        String keyword = s.nextLine().toLowerCase();

        ArrayList<String> results = new ArrayList<>();

        String query = """
                SELECT title
                FROM Album
                WHERE LOWER(title) LIKE ?
                ORDER BY title
                """;
        try (PreparedStatement ps = DAO.getConnection().prepareStatement(query)) {
            ps.setString(1, "%" + keyword + "%");

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String title = rs.getString("title");
                results.add(title);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results;
    }

    public static ArrayList<String> filterAlbumsByArtist() throws SQLException {
        System.out.println("Enter artist name to search for:");
        String keyword = s.nextLine().toLowerCase();

        ArrayList<String> results = new ArrayList<>();

        String query = """
                SELECT title
                FROM Album
                WHERE LOWER(artist) LIKE ?
                ORDER BY title 
                """;
        try (PreparedStatement ps = DAO.getConnection().prepareStatement(query)) {
            ps.setString(1, "%" + keyword + "%");

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String title = rs.getString("title");
                results.add(title);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results;

    }

    public static ArrayList<String> filterAlbumsByBestselling() throws SQLException {
        ArrayList<String> results = new ArrayList<>();

        String query = """
                SELECT a.title, COALESCE(SUM(p.price), 0) AS totalSales
                FROM Album a
                JOIN purchase pu ON a.productID = pu.productID
                JOIN Products p ON a.productID = p.productID
                GROUP BY a.productID, a.title
                ORDER BY totalSales DESC
                FETCH FIRST 10 ROWS ONLY
                """;

        try (PreparedStatement ps = DAO.getConnection().prepareStatement(query)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String title = rs.getString("title");
                results.add(title);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results;
    }

    public static void inputCreatePlaylist() {
        s.nextLine(); // consume trailing newline left by menu's nextInt()

        System.out.println("Enter a name for your new playlist:");
        String playlistName = getValidInput("any");

        int playlistID;
        try {
            playlistID = Playlist.create(playlistName, user.getUsername());
            System.out.println("Playlist \"" + playlistName + "\" created successfully!");
        } catch (SQLException e) {
            System.err.println("Failed to create playlist.");
            System.err.println("Code: " + e.getErrorCode() + " SQLState: " + e.getSQLState());
            System.err.println(e.getMessage());
            return;
        }

        if (playlistID == -1) {
            System.err.println("Failed to retrieve new playlist ID.");
            return;
        }

        int songsAdded = 0;
        while (true) {
            System.out.println("Enter a song title to search (or part of a title):");
            String searchTerm = s.nextLine();

            ArrayList<String[]> results;
            try {
                results = Playlist.searchSongs(searchTerm);
            } catch (SQLException e) {
                System.err.println("Error searching for songs.");
                System.err.println("Code: " + e.getErrorCode() + " SQLState: " + e.getSQLState());
                System.out.println("Add another song? (y/n):");
                if (!s.nextLine().trim().equalsIgnoreCase("y")) break;
                continue;
            }

            if (results.isEmpty()) {
                System.out.println("No songs found matching \"" + searchTerm + "\". Try a different search.");
                System.out.println("Add another song? (y/n):");
                if (!s.nextLine().trim().equalsIgnoreCase("y")) break;
                continue;
            }

            System.out.println("Search results:");
            for (int i = 0; i < results.size(); i++) {
                String[] row = results.get(i);
                System.out.println((i + 1) + ". \"" + row[0] + "\" | Album: " + row[2] +
                        " | Artist: " + row[3] + " | Length: " + row[4] + "s");
            }

            System.out.println("Enter the number of the song to add (or 0 to cancel):");
            int choice;
            try {
                choice = Integer.parseInt(s.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
                System.out.println("Add another song? (y/n):");
                if (!s.nextLine().trim().equalsIgnoreCase("y")) break;
                continue;
            }

            if (choice == 0) {
                System.out.println("Add another song? (y/n):");
                if (!s.nextLine().trim().equalsIgnoreCase("y")) break;
                continue;
            }

            if (!validateInputRange(1, results.size(), choice)) {
                System.out.println("Add another song? (y/n):");
                if (!s.nextLine().trim().equalsIgnoreCase("y")) break;
                continue;
            }

            String[] selectedSong = results.get(choice - 1);
            String songTitle = selectedSong[0];
            int productID = Integer.parseInt(selectedSong[1]);

            try {
                if (Playlist.containsSong(playlistID, songTitle, productID)) {
                    System.out.println("Song already in playlist.");
                } else {
                    Playlist.addSong(playlistID, songTitle, productID);
                    System.out.println("Added \"" + songTitle + "\" to your playlist.");
                    songsAdded++;
                }
            } catch (SQLException e) {
                System.err.println("Error adding song to playlist.");
                System.err.println("Code: " + e.getErrorCode() + " SQLState: " + e.getSQLState());
            }

            System.out.println("Add another song? (y/n):");
            if (!s.nextLine().trim().equalsIgnoreCase("y")) break;
        }

        System.out.println("\nDone! Playlist \"" + playlistName + "\" has " + songsAdded + " song(s).");
    }

    // WE PREVENT SQL INJECTIONS BY USING PREPAREDSTATEMENTS
    public static void main(String[] args) throws Exception {
        // Unique table names.  Either the user supplies a unique identifier as a command line argument, or the program makes one up.
        String tableName = "";
        int sqlCode = 0;      // Variable to hold SQLCODE
        String sqlState = "00000";  // Variable to hold SQLSTATE

        if (args.length > 0)
            tableName += args[0];
        else
            tableName += "exampletbl";

        // Register the driver.  You must register the driver before you can use it.
        try {
            DriverManager.registerDriver(new com.ibm.db2.jcc.DB2Driver());
        } catch (Exception cnfe) {
            System.out.println("Class not found");
        }

        // This is the url you must use for DB2.
        //Note: This url may not valid now ! Check for the correct year and semester and server name.
        String url = "jdbc:db2://winter2026-comp421.cs.mcgill.ca:50000/comp421";

        /*
        //REMEMBER to remove your user id and password before submitting your code!!
        String your_userid = null;
        String your_password = null;
        //AS AN ALTERNATIVE, you can just set your password in the shell environment in the Unix (as shown below) and read it from there.
        //$  export SOCSPASSWD=yoursocspasswd 
        if(your_userid == null && (your_userid = System.getenv("SOCSUSER")) == null)
        {
          System.err.println("Error!! do not have a password to connect to the database!");
          System.exit(1);
        }
        if(your_password == null && (your_password = System.getenv("SOCSPASSWD")) == null)
        {
          System.err.println("Error!! do not have a password to connect to the database!");
          System.exit(1);
        }
        */

        String your_userid = System.getenv("SOCSUSER");
        String your_password = System.getenv("SOCSPASSWD");

        if(your_userid == null || your_password == null) {
            throw new Exception("Must have SOCSUSER and SOCSPASSED env variables set!");
        }

        //Connection con = DriverManager.getConnection(url,your_userid,your_password);

        DAO.getConnection(url, your_userid, your_password);

        final String MENU = "Please choose an option from the following:\n" +
                "1. User settings\n" +
                "2. Purchase an album\n" +
                "3. Upload a new album\n" +
                "4. View top albums by genre\n" +
                "5. Create a playlist\n" +
                "6. ";

        final String MENU1 = "Please choose an option from the following:\n" +
                "1. Log in\n" +
                "2. Create an account\n" +
                "3. Back";

		final String MENU5 = "Please choose an option from the following:\n" +
				"1. List your playlists\n" +
				"2. Create a new playlist\n" +
				"3. Modify an existing playlist\n" +
				"4. Delete an existing playlist\n" +
				"5. Back";

        while(true) {
            System.out.println(MENU);
            int menuOption = s.nextInt();

            if(!validateInputRange(1, 6, menuOption)) {
                continue;
            }

            switch(menuOption) {
                case 1:
                    menu1_checkpoint:
                    while(true) {
                        System.out.println(MENU1);

                        menuOption = s.nextInt();

                        if(!validateInputRange(1, 3, menuOption)) {
                            continue;
                        }

                        switch(menuOption) {
                            case 1:
                                inputLogin();
                                break menu1_checkpoint;
                            case 2:
                                inputCreateAccount();
                                break menu1_checkpoint;
                            case 3:
                                break menu1_checkpoint;
                        }
                    }
                case 2:
                    continue;
                case 3:
                    inputUploadNewAlbum();
                case 4:
                    // find top albums by genre:
                    //  -> fulfills requirement of submenu by:
                    //      -> get list of genres using Genres.getGenres.toArrayList()
                    //      -> print out list of genres and have user input corresponding number
                    //      -> based on that number, query db for top albums with that genre
                    inputTopAlbumsByGenre();
                    continue;
                case 5:
                    menu5_checkpoint:
                    while (true) {
                        if (!verifyUserLogin("You must be logged in to manage playlists.")) {
                            break menu5_checkpoint;
                        }
                        System.out.println(MENU5);
                        menuOption = s.nextInt();

                        if (!validateInputRange(1, 5, menuOption)) {
                            continue;
                        }

                        switch (menuOption) {
                            case 1:
                                // TODO: list playlists
								System.out.println("Error: not implemented");
//								inputListPlaylists();
                                break;
                            case 2:
                                inputCreatePlaylist();
                                break;
                            case 3:
                                // TODO: modify playlist
								System.out.println("Error: not implemented");
//								inputModifyPlaylist();
                                break;
                            case 4:
                                // TODO: delete playlist
								System.out.println("Error: not implemented");
//								inputDeletePlaylist();
                                break;
                            case 5:
                                break menu5_checkpoint;
                        }
                    }
                    continue;
                default:
                    break;
            }
        }
        //statement.close();
        //DAO.close(); // unreachable due to while(true) loop above

    }

      /*
        // Creating a table
        try
        {
          String createSQL = "CREATE TABLE " + tableName + " (id INTEGER, name VARCHAR (25)) ";
          System.out.println (createSQL ) ;
          statement.executeUpdate (createSQL ) ;
          System.out.println ("DONE");
        }
        catch (SQLException e)
        {
          sqlCode = e.getErrorCode(); // Get SQLCODE
          sqlState = e.getSQLState(); // Get SQLSTATE

          // Your code to handle errors comes here;
          // something more meaningful than a print would be good
          System.out.println("Code: " + sqlCode + "  sqlState: " + sqlState);
          System.out.println(e);
         }

        // Inserting Data into the table
        try
        {
          String insertSQL = "INSERT INTO " + tableName + " VALUES ( 1 , \'Vicki\' ) " ;
          System.out.println ( insertSQL ) ;
          statement.executeUpdate ( insertSQL ) ;
          System.out.println ( "DONE" ) ;

          insertSQL = "INSERT INTO " + tableName + " VALUES ( 2 , \'Vera\' ) " ;
          System.out.println ( insertSQL ) ;
          statement.executeUpdate ( insertSQL ) ;
          System.out.println ( "DONE" ) ;
          insertSQL = "INSERT INTO " + tableName + " VALUES ( 3 , \'Franca\' ) " ;
          System.out.println ( insertSQL ) ;
          statement.executeUpdate ( insertSQL ) ;
          System.out.println ( "DONE" ) ;

        }
        catch (SQLException e)
        {
          sqlCode = e.getErrorCode(); // Get SQLCODE
          sqlState = e.getSQLState(); // Get SQLSTATE

          // Your code to handle errors comes here;
          // something more meaningful than a print would be good
          System.out.println("Code: " + sqlCode + "  sqlState: " + sqlState);
          System.out.println(e);
        }

        // Querying a table
        try
        {
          String querySQL = "SELECT id, name from " + tableName + " WHERE NAME = \'Vicki\'";
          System.out.println (querySQL) ;
          java.sql.ResultSet rs = statement.executeQuery ( querySQL ) ;

          while ( rs.next ( ) )
          {
            int id = rs.getInt ( 1 ) ;
            String name = rs.getString (2);
            System.out.println ("id:  " + id);
            System.out.println ("name:  " + name);
          }
         System.out.println ("DONE");
        }
        catch (SQLException e)


       */
}