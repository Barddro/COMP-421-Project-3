import utils.DAO;

import java.sql.DriverManager;

class Application {

    // WE PREVENT SQL INJECTIONS BY USING PREPAREDSTATEMENTS
    public static void main(String[] args) throws Exception {
        String tableName = "";

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

        String your_userid = System.getenv("SOCSUSER");
        String your_password = System.getenv("SOCSPASSWD");

        if (your_userid == null || your_password == null) {
            throw new Exception("Must have SOCSUSER and SOCSPASSED env variables set!");
        }

        DAO.getConnection(url, your_userid, your_password);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                DAO.close();
                System.out.println("\nConnection closed. Goodbye!");
            } catch (Exception e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }));

        final String MENU = "Please choose an option from the following:\n" +
                "0. Exit\n" +
                "1. User settings\n" +
                "2. Purchase an album\n" +
                "3. Upload a new album\n" +
                "4. View top albums by genre\n" +
                "5. View, create or modify playlists";

        final String MENU1 = "Please choose an option from the following:\n" +
                "0. Back\n" +
                "1. Log in\n" +
                "2. Create an account";

        final String MENU5 = "Please choose an option from the following:\n" +
                "0. Back\n" +
                "1. List your playlists\n" +
                "2. Create a new playlist\n" +
                "3. Modify an existing playlist\n" +
                "4. Delete an existing playlist";

        while (true) {
            System.out.println(MENU);
            int menuOption = UI.s.nextInt();

            if (!UI.validateInputRange(1, 6, menuOption)) {
                continue;
            }

            switch (menuOption) {
                case 1:
                    menu1_checkpoint:
                    while (true) {
                        System.out.println(MENU1);

                        menuOption = UI.s.nextInt();

                        if (!UI.validateInputRange(0, 2, menuOption)) {
                            continue;
                        }

                        switch (menuOption) {
                            case 0:
                                break menu1_checkpoint;
                            case 1:
                                UI.inputLogin();
                                break menu1_checkpoint;
                            case 2:
                                UI.inputCreateAccount();
                                break menu1_checkpoint;
                        }
                    }
                    continue;
                case 2:
                    Album.inputPurchaseAlbum();
                    break;
                case 3:
                    Album.inputUploadNewAlbum();
                    break;
                case 4:
                    Album.inputTopAlbumsByGenre();
                    break;
                case 5:
                    menu5_checkpoint:
                    while (true) {
                        if (!UI.verifyUserLogin("You must be logged in to manage playlists.")) {
                            break menu5_checkpoint;
                        }
                        System.out.println(MENU5);
                        menuOption = UI.s.nextInt();

                        if (!UI.validateInputRange(0, 5, menuOption)) {
                            continue;
                        }

                        switch (menuOption) {
                            case 0:
                                break menu5_checkpoint;
                            case 1:
                                Playlist.inputListPlaylists();
                                break;
                            case 2:
                                Playlist.inputCreatePlaylist();
                                break;
                            case 3:
                                Playlist.inputModifyPlaylist();
                                break;
                            case 4:
                                Playlist.inputDeletePlaylist();
                                break;
                        }
                    }
                    continue;
                case 0:
                    System.exit(0);
                default:
                    break;
            }
        }
    }
}
