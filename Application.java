import usersupport.User;
import utils.DAO;

import java.sql.* ;
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
        if(menuOption < lo || menuOption > hi) {
            System.out.println("Please choose a valid option");
            return false;
        }
        return true;
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


    public static void inputCreateAccount() {
        System.out.println("Please enter your username:");
        String username = s.next();

        String password;
        while(true) {
            System.out.println("Please enter your password:");
            password = s.next();

            System.out.println("Please re-enter your password:");
            String repassword = s.next();

            if (!password.equals(repassword)) {
                System.out.println("Passwords do not match. Please try again");
            }
            else {
                break;
            }
        }

        boolean validEmail = false;
        String email = "";
        while(!validEmail) {
            System.out.println("Please enter your email address:");
            email = s.next();
            validEmail = email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\\\.[a-zA-Z]{2,}$");
            if(!validEmail) {
                System.out.println("Please enter a valid email address.");
            }
        }

        System.out.println("Please enter your name:");
        String name = s.next();

        System.out.println("Please enter your birth year:");
        int birthyear = s.nextInt();

        System.out.println("Please enter your birth month (as a number from 1-12):");
        int birthmonth = s.nextInt();

        System.out.println("Please enter your birth date:");
        int birthdate = s.nextInt();

        LocalDate localDate = LocalDate.of(birthyear, birthmonth, birthdate);
        Date sqlDate = Date.valueOf(localDate);

        // DO SOME VALIDATION ON INPUT HERE

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
    		System.out.println((i+1) + ". " + genres.get(i));
    	}
    	
    	int genreChoice = s.nextInt();
    	s.nextLine();
    	if (!validateInputRange(1, genres.size(), genreChoice)) {
    		return;
    	}
    	String selectedGenre = genres.get(genreChoice - 1);
  
    	execTopAlbumsByGenre(selectedGenre, rankingChoice);
    }
    
    public static void execTopAlbumsByGenre(String genre, int rankingChoice) throws SQLException{
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
    	try (PreparedStatement ps = con.prepareStatement(query)){
    		ps.setString(1, genre);
    		ResultSet rs = ps.executeQuery();
    		System.out.println("\nTop albums for genre: " + genre);
    		
    		int i = 1;
    		while(rs.next()) {
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
    
    public static void inputPurchaceAlbum() throws SQLException {
    	if (!user.validate()) {
    		System.out.println("You must be logged in to make a purchace.");
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
    	
    	switch(choice) {
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
    		System.out.println((i+1) + ". " + albums.get(i));
    	}
    	System.out.println("Select album number to purchace: ");
    	int albumChoice = s.nextInt();
    	s.nextLine();
    	
    	if (!validateInputRange(1, albums.size(), albumChoice)) {
            return;
        }
    	
    	String selectedAlbum = albums.get(albumChoice - 1);
    	executeAlbumPurchace(selectedAlbum);
    }
    public static void executeAlbumPurchace(String selectedAlbum) {
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
	    	
	    	try (PreparedStatement ps = con.prepareStatement(getAlbumQuery)){
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
	    	try (PreparedStatement ps = con.prepareStatement(insertTransaction)){
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
	    	
	    	String insertPurchace = """
	    			INSERT INTO Purchace (transactionID, username, productID)
	    			VALUES (?, ?, ?)
	    			""";
	    	
	    	try (PreparedStatement ps = con.prepareStatement(insertPurchace)){
	    		ps.setInt(1, transactionID);
	    		ps.setString(2, user.getUsername());
	    		ps.setInt(3, productID);
	    		ps.executeUpdate();
	    	}
	    	//Success
	    	con.commit();
	    	System.out.println("Successfully purchaced album: " + selectedAlbum);
    	} catch (SQLException e) {
    		try {
    			if (con != null) con.rollback(); //undo any faults
    		} catch (SQLException ex) {
    			System.err.println("Rollback failed: " + ex.getMessage());
    		}
    		System.err.println("Purchace failed:");
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
    	try(PreparedStatement ps = DAO.getConnection().prepareStatement(query)){
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
    	try(PreparedStatement ps = DAO.getConnection().prepareStatement(query)){
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
    			JOIN Purchace pu ON a.productID = pu.productID
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

    // WE PREVENT SQL INJECTIONS BY USING PREPAREDSTATEMENTS
    public static void main ( String [ ] args ) throws Exception
    {
      // Unique table names.  Either the user supplies a unique identifier as a command line argument, or the program makes one up.
        String tableName = "";
        int sqlCode=0;      // Variable to hold SQLCODE
        String sqlState="00000";  // Variable to hold SQLSTATE

        if ( args.length > 0 )
            tableName += args [ 0 ] ;
        else
          tableName += "exampletbl";

        // Register the driver.  You must register the driver before you can use it.
        try { DriverManager.registerDriver ( new com.ibm.db2.jcc.DB2Driver() ) ; }
        catch (Exception cnfe){ System.out.println("Class not found"); }

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
                "2. Purchace an album" +
                "3. Upload a new album\n" +
                "4. View top albums by genre\n" +
                "5. \n" +
                "6. ";

        final String MENU1 = "Please choose an option from the following:\n" +
                "1. Log in\n" +
                "2. Create an account\n" +
                "3. Back";

        while(true) {
            System.out.println(MENU);
            int menuOption = s.nextInt();

            if(!validateInputRange(1, 6, menuOption)) {
                continue;
            }

            switch(menuOption) {
                case 1:
                    while(true) {
                        System.out.println(MENU1);

                        menuOption = s.nextInt();

                        if(!validateInputRange(1, 3, menuOption)) {
                            continue;
                        }

                        switch(menuOption) {
                            case 1:
                                inputLogin();
                            case 2:

                            case 3:
                                break;
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
                    continue;
                default:
                    break;
            }
        }
        //statement.close();
        DAO.close();

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
        {
          sqlCode = e.getErrorCode(); // Get SQLCODE
          sqlState = e.getSQLState(); // Get SQLSTATE

          // Your code to handle errors comes here;
          // something more meaningful than a print would be good
          System.out.println("Code: " + sqlCode + "  sqlState: " + sqlState);
          System.out.println(e);
        }

      //Updating a table
      try
      {
        String updateSQL = "UPDATE " + tableName + " SET NAME = \'Mimi\' WHERE id = 3";
        System.out.println(updateSQL);
        statement.executeUpdate(updateSQL);
        System.out.println("DONE");

        // Dropping a table
        String dropSQL = "DROP TABLE " + tableName;
        System.out.println ( dropSQL ) ;
        statement.executeUpdate ( dropSQL ) ;
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

      // Finally but importantly close the statement and connection
      statement.close ( ) ;
      con.close ( ) ;
     */
}

