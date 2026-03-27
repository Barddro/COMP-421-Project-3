import usersupport.User;
import utils.DAO;

import java.sql.* ;
import java.time.LocalDate;
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
                "2. \n" +
                "3. Upload a new album\n" +
                "4. \n" +
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
