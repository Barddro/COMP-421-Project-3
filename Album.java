import utils.DAO;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;

public class Album {
    String genre;
    String artist;
    ArrayList<Song> songs;

    public static void inputUploadNewAlbum() {
        if (!UI.user.validate()) {
            System.out.println("You must be logged in to upload an album.");
            UI.s.nextLine();
            UI.waitForEnter();
            return;
        }

        if (!UI.user.isArtist()) {
            System.out.println("Please log in with a valid Artist account before uploading an album");
            UI.s.nextLine();
            UI.waitForEnter();
            return;
        }

        UI.s.nextLine(); // consume trailing newline from menu selection

        System.out.println("Please enter the name of the album:");
        String albumTitle = UI.s.nextLine().trim();

        ArrayList<String> genreList = new ArrayList<>(Genres.getGenres());
        Collections.sort(genreList);

        System.out.println("Please choose a genre:");
        for (int i = 0; i < genreList.size(); i++) {
            System.out.println((i + 1) + ". " + genreList.get(i));
        }

        int genreChoice = -1;
        while (genreChoice < 1 || genreChoice > genreList.size()) {
            try {
                genreChoice = Integer.parseInt(UI.s.nextLine().trim());
                if (genreChoice < 1 || genreChoice > genreList.size())
                    System.out.println("Please choose a number between 1 and " + genreList.size() + ".");
            } catch (NumberFormatException e) {
                System.out.println("Please enter a number.");
            }
        }
        String albumGenre = genreList.get(genreChoice - 1);

        double price = -1;
        while (price <= 0) {
            System.out.println("Please enter the price of the album:");
            try {
                price = Double.parseDouble(UI.s.nextLine().trim());
                if (price <= 0) System.out.println("Price must be greater than 0.");
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }

        int numSongs = -1;
        while (numSongs <= 0) {
            System.out.println("How many songs does this album have?");
            try {
                numSongs = Integer.parseInt(UI.s.nextLine().trim());
                if (numSongs <= 0) System.out.println("Must have at least one song.");
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }

        ArrayList<String[]> songs = new ArrayList<>();
        for (int i = 0; i < numSongs; i++) {
            System.out.println("Song " + (i + 1) + " title:");
            String songTitle = UI.s.nextLine().trim();

            int length = -1;
            while (length <= 0) {
                System.out.println("Song " + (i + 1) + " length (in seconds):");
                try {
                    length = Integer.parseInt(UI.s.nextLine().trim());
                    if (length <= 0) System.out.println("Length must be greater than 0.");
                } catch (NumberFormatException e) {
                    System.out.println("Please enter a valid number.");
                }
            }

            System.out.println("Song " + (i + 1) + " audio quality (low/medium/high):");
            String audioQuality = UI.s.nextLine().trim();

            songs.add(new String[]{songTitle, String.valueOf(length), audioQuality});
        }

        execUploadNewAlbum(albumTitle, albumGenre, price, songs, UI.user.getArtistID());
        UI.waitForEnter();
    }

    public static void execUploadNewAlbum(String albumTitle, String genre, double price, ArrayList<String[]> songs, int artistID) {
        Connection con = null;
        try {
            con = DAO.getConnection();
            con.setAutoCommit(false);

            String insertProduct = """
                    INSERT INTO Products(productID, price, artistID)
                    VALUES ((SELECT COALESCE(MAX(productID), 0)+1 FROM Products), ?, ?)
                    """;
            try (PreparedStatement ps = con.prepareStatement(insertProduct)) {
                ps.setDouble(1, price);
                ps.setInt(2, artistID);
                ps.executeUpdate();
            }

            String insertAlbum = """
                    INSERT INTO Album(productID, releaseDate, title, artistID)
                    VALUES ((SELECT MAX(productID) FROM Products), CURRENT_DATE, ?, ?)
                    """;
            try (PreparedStatement ps = con.prepareStatement(insertAlbum)) {
                ps.setString(1, albumTitle);
                ps.setInt(2, artistID);
                ps.executeUpdate();
            }

            String insertSong = """
                    INSERT INTO Songs(songTitle, productID, length, audioQuality)
                    VALUES (?, (SELECT MAX(productID) FROM Products), ?, ?)
                    """;
            for (String[] song : songs) {
                try (PreparedStatement ps = con.prepareStatement(insertSong)) {
                    ps.setString(1, song[0]);
                    ps.setInt(2, Integer.parseInt(song[1]));
                    ps.setString(3, song[2]);
                    ps.executeUpdate();
                }
            }

            String insertIsOf = """
                    INSERT INTO isOf(productID, name)
                    VALUES ((SELECT MAX(productID) FROM Products), ?)
                    """;
            try (PreparedStatement ps = con.prepareStatement(insertIsOf)) {
                ps.setString(1, genre);
                ps.executeUpdate();
            }

            con.commit();
            System.out.println("Album \"" + albumTitle + "\" uploaded successfully!");

        } catch (SQLException e) {
            try {
                if (con != null) con.rollback();
            } catch (SQLException ex) {
                System.err.println("Rollback failed: " + ex.getMessage());
            }
            System.err.println("Upload failed:");
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

    public static void inputTopAlbumsByGenre() throws SQLException {
        System.out.println("Choose ranking type:");
        System.out.println("1. By stream count");
        System.out.println("2. By sales");

        int rankingChoice = UI.s.nextInt();
        UI.s.nextLine();
        if (!UI.validateInputRange(1, 2, rankingChoice)) {
            return;
        }

        ArrayList<String> genres = new ArrayList<>(Genres.getGenres());
        Collections.sort(genres);

        System.out.println("Please choose a genre from the following list:");
        for (int i = 0; i < genres.size(); i++) {
            System.out.println((i + 1) + ". " + genres.get(i));
        }

        int genreChoice = UI.s.nextInt();
        UI.s.nextLine();
        if (!UI.validateInputRange(1, genres.size(), genreChoice)) {
            return;
        }
        String selectedGenre = genres.get(genreChoice - 1);

        execTopAlbumsByGenre(selectedGenre, rankingChoice);

        UI.waitForEnter();
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
                    \tON so.songTitle = s.songTitle AND so.productID = s.productID
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
        if (!UI.user.validate()) {
            System.out.println("You must be logged in to make a purchase.");
            UI.s.nextLine();
            UI.waitForEnter();
            return;
        }

        System.out.println("Choose a filter method:");
        System.out.println("1. Search by name");
        System.out.println("2. Search by artist");
        System.out.println("3. Search by best-sellers");

        int choice = UI.s.nextInt();
        UI.s.nextLine();

        if (!UI.validateInputRange(1, 3, choice)) return;

        ArrayList<String> albumNames = new ArrayList<>();
        ArrayList<Integer> albumIDs = new ArrayList<>();

        switch (choice) {
            case 1:
                filterAlbumsByName(albumNames, albumIDs);
                break;
            case 2:
                filterAlbumsByArtist(albumNames, albumIDs);
                break;
            case 3:
                filterAlbumsByBestselling(albumNames, albumIDs);
                break;
        }

        if (albumNames.isEmpty()) {
            System.out.println("No albums found.");
            UI.waitForEnter();
            return;
        }

        System.out.println("\nFilter results:");
        for (int i = 0; i < albumNames.size(); i++) {
            System.out.println((i + 1) + ". " + albumNames.get(i));
        }

        System.out.println("Select album number to purchase:");
        int albumChoice = UI.s.nextInt();
        UI.s.nextLine();

        if (!UI.validateInputRange(1, albumNames.size(), albumChoice)) return;

        executeAlbumPurchase(albumIDs.get(albumChoice - 1));
        UI.waitForEnter();
    }

    public static void filterAlbumsByName(ArrayList<String> albumNames, ArrayList<Integer> albumIDs) {
        System.out.println("Enter a keyword to search album titles:");
        String keyword = UI.s.nextLine().toLowerCase();

        String query = """
                SELECT a.productID, a.title, ar.name AS artistName, p.price
                FROM Album a
                JOIN Products p ON a.productID = p.productID
                JOIN Artists ar ON a.artistID = ar.artistid
                WHERE LOWER(a.title) LIKE ?
                ORDER BY a.title
                """;
        try (PreparedStatement ps = DAO.getConnection().prepareStatement(query)) {
            ps.setString(1, "%" + keyword + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                albumIDs.add(rs.getInt("productID"));
                albumNames.add(rs.getString("title") + " - " + rs.getString("artistName") + " ($" + String.format("%.2f", rs.getDouble("price")) + ")");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void filterAlbumsByArtist(ArrayList<String> albumNames, ArrayList<Integer> albumIDs) {
        System.out.println("Enter artist name to search for:");
        String keyword = UI.s.nextLine().toLowerCase();

        String query = """
                SELECT a.productID, a.title, ar.name AS artistName, p.price
                FROM Album a
                JOIN Products p ON a.productID = p.productID
                JOIN Artists ar ON a.artistID = ar.artistid
                WHERE LOWER(ar.name) LIKE ?
                ORDER BY a.title
                """;
        try (PreparedStatement ps = DAO.getConnection().prepareStatement(query)) {
            ps.setString(1, "%" + keyword + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                albumIDs.add(rs.getInt("productID"));
                albumNames.add(rs.getString("title") + " - " + rs.getString("artistName") + " ($" + String.format("%.2f", rs.getDouble("price")) + ")");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void filterAlbumsByBestselling(ArrayList<String> albumNames, ArrayList<Integer> albumIDs) {
        String query = """
                SELECT a.productID, a.title, ar.name AS artistName, p.price, COALESCE(SUM(pu.productID), 0) AS sales
                FROM Album a
                JOIN Products p ON a.productID = p.productID
                JOIN Artists ar ON a.artistID = ar.artistid
                LEFT JOIN Purchase pu ON a.productID = pu.productID
                GROUP BY a.productID, a.title, ar.name, p.price
                ORDER BY sales DESC
                FETCH FIRST 10 ROWS ONLY
                """;
        try (PreparedStatement ps = DAO.getConnection().prepareStatement(query)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                albumIDs.add(rs.getInt("productID"));
                albumNames.add(rs.getString("title") + " - " + rs.getString("artistName") + " ($" + String.format("%.2f", rs.getDouble("price")) + ")");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void inputSearchAlbums() {
        UI.s.nextLine(); // consume trailing newline from menu selection
        System.out.println("Search by:\n1. Album name\n2. Artist name\n3. Best-sellers");

        int choice = -1;
        while (choice < 1 || choice > 3) {
            try {
                choice = Integer.parseInt(UI.s.nextLine().trim());
                if (choice < 1 || choice > 3) System.out.println("Please choose 1, 2, or 3.");
            } catch (NumberFormatException e) {
                System.out.println("Please enter a number.");
            }
        }

        ArrayList<String> albumNames = new ArrayList<>();
        ArrayList<Integer> albumIDs = new ArrayList<>();

        switch (choice) {
            case 1: filterAlbumsByName(albumNames, albumIDs); break;
            case 2: filterAlbumsByArtist(albumNames, albumIDs); break;
            case 3: filterAlbumsByBestselling(albumNames, albumIDs); break;
        }

        if (albumNames.isEmpty()) {
            System.out.println("No albums found.");
        } else {
            System.out.println("\nResults:");
            for (int i = 0; i < albumNames.size(); i++) {
                System.out.println((i + 1) + ". " + albumNames.get(i));
            }
        }

        UI.waitForEnter();
    }

    public static void executeAlbumPurchase(int productID) {
    Connection con = null;

    try {
        con = DAO.getConnection();
        con.setAutoCommit(false);

        // Get album price and title
        String getAlbumQuery = """
                SELECT a.title, p.price
                FROM Album a
                JOIN Products p ON a.productID = p.productID
                WHERE a.productID = ?
                """;

        String albumTitle = "";
        double price = -1;

        try (PreparedStatement ps = con.prepareStatement(getAlbumQuery)) {
            ps.setInt(1, productID);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                throw new SQLException("Album not found.");
            }
            albumTitle = rs.getString("title");
            price = rs.getDouble("price");
        }

        // Check if user already owns this album
        String checkOwned = """
                SELECT 1 FROM Purchase pu
                WHERE pu.username = ? AND pu.productID = ?
                """;
        try (PreparedStatement ps = con.prepareStatement(checkOwned)) {
            ps.setString(1, UI.user.getUsername());
            ps.setInt(2, productID);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                System.out.println("You already own this album.");
                con.rollback();
                return;
            }
        }

        // Generate next transactionID
        int transactionID;
        String getMaxID = "SELECT COALESCE(MAX(transactionID), 0) FROM Transactions";
        try (Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(getMaxID)) {
            rs.next();
            transactionID = rs.getInt(1) + 1;
        }

        // Insert into Transactions
        String insertTransaction = """
                INSERT INTO Transactions (transactionID, price, date)
                VALUES (?, ?, CURRENT_DATE)
                """;
        try (PreparedStatement ps = con.prepareStatement(insertTransaction)) {
            ps.setInt(1, transactionID);
            ps.setDouble(2, price);
            ps.executeUpdate();
        }

        // Insert into Purchase
        String insertPurchase = """
                INSERT INTO Purchase (transactionID, username, productID)
                VALUES (?, ?, ?)
                """;
        try (PreparedStatement ps = con.prepareStatement(insertPurchase)) {
            ps.setInt(1, transactionID);
            ps.setString(2, UI.user.getUsername());
            ps.setInt(3, productID);
            ps.executeUpdate();
        }

        con.commit();
        System.out.println("Successfully purchased \"" + albumTitle + "\" for $" + String.format("%.2f", price));

    } catch (SQLException e) {
        try {
            if (con != null) con.rollback();
        } catch (SQLException ex) {
            System.err.println("Rollback failed: " + ex.getMessage());
        }
        System.err.println("Purchase failed:");
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
}