import utils.DAO;

import java.sql.*;
import java.util.ArrayList;

class Playlist {

//     creates a new playlist for the given user.
//     returns the new playlistID, or -1 on failure.
    public static int create(String name, String username) throws SQLException {
        Connection con = DAO.getConnection();

        String insertPlaylist =
                "INSERT INTO Playlist(playlistID, name, username) " +
                "VALUES ((SELECT COALESCE(MAX(playlistID), 0) + 1 FROM Playlist), ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(insertPlaylist)) {
            ps.setString(1, name);
            ps.setString(2, username);
            ps.executeUpdate();
        }

        String getID = "SELECT MAX(playlistID) FROM Playlist WHERE username = ?";
        try (PreparedStatement ps = con.prepareStatement(getID)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }

        return -1;
    }

//     searches for songs matching the given term (case-insensitive substring match).
//     returns a list of rows: [songTitle, productID, albumTitle, artistName, length]
    public static ArrayList<String[]> searchSongs(String term) throws SQLException {
        Connection con = DAO.getConnection();
        ArrayList<String[]> results = new ArrayList<>();

        String query =
                "SELECT s.songTitle, s.productID, a.title AS albumTitle, ar.name AS artistName, s.length " +
                "FROM Songs s " +
                "JOIN Album a ON s.productID = a.productID " +
                "JOIN Artists ar ON a.artistID = ar.artistid " +
                "WHERE LOWER(s.songTitle) LIKE LOWER(?)";

        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, "%" + term + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                results.add(new String[]{
                    rs.getString("songTitle"),
                    String.valueOf(rs.getInt("productID")),
                    rs.getString("albumTitle"),
                    rs.getString("artistName"),
                    rs.getString("length")
                });
            }
        }

        return results;
    }

//     returns true if the given song is already in the playlist.
    public static boolean containsSong(int playlistID, String songTitle, int productID) throws SQLException {
        Connection con = DAO.getConnection();
        String query = "SELECT 1 FROM Contains WHERE playlistID = ? AND songTitle = ? AND productID = ?";
        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, playlistID);
            ps.setString(2, songTitle);
            ps.setInt(3, productID);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        }
    }

//     adds a song to a playlist.
    public static void addSong(int playlistID, String songTitle, int productID) throws SQLException {
        Connection con = DAO.getConnection();
        String query = "INSERT INTO Contains(playlistID, songTitle, productID) VALUES (?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, playlistID);
            ps.setString(2, songTitle);
            ps.setInt(3, productID);
            ps.executeUpdate();
        }
    }

//     returns [playlistID, name, songCount] for each playlist owned by the user.
    public static ArrayList<String[]> listPlaylists(String username) throws SQLException {
        Connection con = DAO.getConnection();
        ArrayList<String[]> results = new ArrayList<>();

        String query =
                "SELECT p.playlistID, p.name, COUNT(c.songTitle) AS songCount " +
                "FROM Playlist p " +
                "LEFT JOIN Contains c ON p.playlistID = c.playlistID " +
                "WHERE p.username = ? " +
                "GROUP BY p.playlistID, p.name " +
                "ORDER BY p.name";

        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                results.add(new String[]{String.valueOf(rs.getInt("playlistID")),rs.getString("name"),String.valueOf(rs.getInt("songCount"))});
            }
        }
        return results;
    }

    public static ArrayList<String[]> listSongs(int playlistID) throws SQLException {
        Connection con = DAO.getConnection();
        ArrayList<String[]> results = new ArrayList<>();

        String query =
                "SELECT s.songTitle, a.title AS albumTitle, ar.name AS artistName, s.length, s.productID " +
                        "FROM Contains c " +
                        "JOIN Songs s ON c.songTitle = s.songTitle AND c.productID = s.productID " +
                        "JOIN Album a ON s.productID = a.productID " +
                        "JOIN Artists ar ON a.artistID = ar.artistid " +
                        "WHERE c.playlistID = ? " +
                        "ORDER BY s.songTitle";

        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, playlistID);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                results.add(new String[]{
                        rs.getString("songTitle"),
                        rs.getString("albumTitle"),
                        rs.getString("artistName"),
                        rs.getString("length"),
                        String.valueOf(rs.getInt("productID"))   // index [4]
                });
            }
        }
        return results;
    }

    public static void rename(int playlistID, String newName, String username) throws SQLException {
        Connection con = DAO.getConnection();
        String query = "UPDATE Playlist SET name = ? WHERE playlistID = ? AND username = ?";
        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, newName);
            ps.setInt(2, playlistID);
            ps.setString(3, username);
            ps.executeUpdate();
        }
    }

    public static void removeSong(int playlistID, String songTitle, int productID) throws SQLException {
        Connection con = DAO.getConnection();
        String query = "DELETE FROM Contains WHERE playlistID = ? AND songTitle = ? AND productID = ?";
        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, playlistID);
            ps.setString(2, songTitle);
            ps.setInt(3, productID);
            ps.executeUpdate();
        }
    }

    public static void delete(int playlistID, String username) throws SQLException {
        Connection con = DAO.getConnection();

        // remove all songs first to satisfy foreign key constraint
        String deleteSongs = "DELETE FROM Contains WHERE playlistID = ?";
        try (PreparedStatement ps = con.prepareStatement(deleteSongs)) {
            ps.setInt(1, playlistID);
            ps.executeUpdate();
        }

        // then delete the playlist itself, scoped to username for safety
        String deletePlaylist = "DELETE FROM Playlist WHERE playlistID = ? AND username = ?";
        try (PreparedStatement ps = con.prepareStatement(deletePlaylist)) {
            ps.setInt(1, playlistID);
            ps.setString(2, username);
            ps.executeUpdate();
        }
    }

    public static void inputListPlaylists() {
        UI.s.nextLine(); // flush trailing newline from menu's nextInt()

        while(true) {

            ArrayList<String[]> playlists;
            try {
                playlists = Playlist.listPlaylists(UI.user.getUsername());
            } catch (SQLException e) {
                System.err.println("Failed to retrieve playlists.");
                System.err.println("Code: " + e.getErrorCode() + " SQLState: " + e.getSQLState());
                return;
            }

            if (playlists.isEmpty()) {
                System.out.println("You have no playlists yet.");
                UI.waitForEnter();
                return;
            }

            System.out.println("Your playlists:");
            for (int i = 0; i < playlists.size(); i++) {
                String[] row = playlists.get(i);
                System.out.println((i + 1) + ". " + row[1] + " (" + row[2] + " songs)");
            }

            System.out.println("Enter a playlist number to view its songs (or 0 to go back):");
            int choice = Integer.parseInt(UI.getValidInput("integer"));

            if (choice == 0) return;
            if (!UI.validateInputRange(1, playlists.size(), choice)) return;

            String[] selected = playlists.get(choice - 1);
            int playlistID = Integer.parseInt(selected[0]);
            String playlistName = selected[1];

            ArrayList<String[]> songs;
            try {
                songs = Playlist.listSongs(playlistID);
            } catch (SQLException e) {
                System.err.println("Failed to retrieve songs.");
                System.err.println("Code: " + e.getErrorCode() + " SQLState: " + e.getSQLState());
                return;
            }

            if (songs.isEmpty()) {
                System.out.println("\"" + playlistName + "\" has no songs yet.");
                UI.waitForEnter();
                return;
            }

            System.out.println("Songs in \"" + playlistName + "\":");
            for (int i = 0; i < songs.size(); i++) {
                String[] row = songs.get(i);
                System.out.println((i + 1) + ". \"" + row[0] + "\" | Album: " + row[1] +
                        " | Artist: " + row[2] + " | Length: " + row[3] + "s");
            }

            UI.waitForEnter();
        }
    }

    public static void inputModifyPlaylist() {
        UI.s.nextLine(); // flush trailing newline from menu's nextInt()

        ArrayList<String[]> playlists;
        try {
            playlists = Playlist.listPlaylists(UI.user.getUsername());
        } catch (SQLException e) {
            System.err.println("Failed to retrieve playlists.");
            System.err.println("Code: " + e.getErrorCode() + " SQLState: " + e.getSQLState());
            return;
        }

        if (playlists.isEmpty()) {
            System.out.println("You have no playlists to modify.");
            return;
        }

        System.out.println("Your playlists:");
        for (int i = 0; i < playlists.size(); i++) {
            String[] row = playlists.get(i);
            System.out.println((i + 1) + ". " + row[1] + " (" + row[2] + " songs)");
        }

        System.out.println("Select a playlist to modify (or 0 to go back):");
        int choice = Integer.parseInt(UI.getValidInput("integer"));

        if (choice == 0) return;
        if (!UI.validateInputRange(1, playlists.size(), choice)) return;

        String[] selected = playlists.get(choice - 1);
        int playlistID = Integer.parseInt(selected[0]);
        String playlistName = selected[1];

        modify_checkpoint:
        while (true) {
            System.out.println("Modifying \"" + playlistName + "\". Choose an option:");
            System.out.println("0. Back");
            System.out.println("1. Rename playlist");
            System.out.println("2. Add a song");
            System.out.println("3. Remove a song");
            
            int modifyOption = Integer.parseInt(UI.getValidInput("integer"));

            switch (modifyOption) {
                case 1:
                    System.out.println("Enter a new name for \"" + playlistName + "\":");
                    String newName = UI.getValidInput("any");
                    try {
                        Playlist.rename(playlistID, newName, UI.user.getUsername());
                        System.out.println("Playlist renamed to \"" + newName + "\".");
                        playlistName = newName;
                    } catch (SQLException e) {
                        System.err.println("Failed to rename playlist.");
                        System.err.println("Code: " + e.getErrorCode() + " SQLState: " + e.getSQLState());
                    }
                    break;

                case 2:
                    System.out.println("Enter a song title to search:");
                    String searchTerm = UI.s.nextLine();
                    ArrayList<String[]> results;
                    try {
                        results = Playlist.searchSongs(searchTerm);
                    } catch (SQLException e) {
                        System.err.println("Error searching for songs.");
                        System.err.println("Code: " + e.getErrorCode() + " SQLState: " + e.getSQLState());
                        break;
                    }
                    if (results.isEmpty()) {
                        System.out.println("No songs found.");
                        break;
                    }
                    for (int i = 0; i < results.size(); i++) {
                        String[] row = results.get(i);
                        System.out.println((i + 1) + ". \"" + row[0] + "\" | Album: " + row[2] +
                                " | Artist: " + row[3] + " | Length: " + row[4] + "s");
                    }
                    System.out.println("Enter song number to add (or 0 to cancel):");
                    int addChoice = Integer.parseInt(UI.getValidInput("integer"));
                    if (addChoice == 0) break;
                    if (!UI.validateInputRange(1, results.size(), addChoice)) break;
                    String[] toAdd = results.get(addChoice - 1);
                    try {
                        if (Playlist.containsSong(playlistID, toAdd[0], Integer.parseInt(toAdd[1]))) {
                            System.out.println("Song already in playlist.");
                        } else {
                            Playlist.addSong(playlistID, toAdd[0], Integer.parseInt(toAdd[1]));
                            System.out.println("Added \"" + toAdd[0] + "\" to playlist.");
                        }
                    } catch (SQLException e) {
                        System.err.println("Failed to add song.");
                        System.err.println("Code: " + e.getErrorCode() + " SQLState: " + e.getSQLState());
                    }
                    break;

                case 3:
                    ArrayList<String[]> songs;
                    try {
                        songs = Playlist.listSongs(playlistID);
                    } catch (SQLException e) {
                        System.err.println("Failed to retrieve songs.");
                        System.err.println("Code: " + e.getErrorCode() + " SQLState: " + e.getSQLState());
                        break;
                    }
                    if (songs.isEmpty()) {
                        System.out.println("Playlist is empty — nothing to remove.");
                        break;
                    }
                    System.out.println("Songs in \"" + playlistName + "\":");
                    for (int i = 0; i < songs.size(); i++) {
                        String[] row = songs.get(i);
                        System.out.println((i + 1) + ". \"" + row[0] + "\" | Album: " + row[1] +
                                " | Artist: " + row[2]);
                    }
                    System.out.println("Enter song number to remove (or 0 to cancel):");
                    int removeChoice = Integer.parseInt(UI.getValidInput("integer"));
                    if (removeChoice == 0) break;
                    if (!UI.validateInputRange(1, songs.size(), removeChoice)) break;
                    String[] toRemove = songs.get(removeChoice - 1);
                    try {
                        Playlist.removeSong(playlistID, toRemove[0], Integer.parseInt(toRemove[4]));
                        System.out.println("Removed \"" + toRemove[0] + "\" from playlist.");
                    } catch (SQLException e) {
                        System.err.println("Failed to remove song.");
                        System.err.println("Code: " + e.getErrorCode() + " SQLState: " + e.getSQLState());
                    }
                    break;

                case 0:
                    break modify_checkpoint;

                default:
                    System.out.println("Please choose a valid option.");
            }
        }
    }

    public static void inputCreatePlaylist() {
        UI.s.nextLine(); // consume trailing newline left by menu's nextInt()

        System.out.println("Enter a name for your new playlist:");
        String playlistName = UI.getValidInput("any");

        int playlistID;
        try {
            playlistID = Playlist.create(playlistName, UI.user.getUsername());
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
            String searchTerm = UI.s.nextLine();

            ArrayList<String[]> results;
            try {
                results = Playlist.searchSongs(searchTerm);
            } catch (SQLException e) {
                System.err.println("Error searching for songs.");
                System.err.println("Code: " + e.getErrorCode() + " SQLState: " + e.getSQLState());
                System.out.println("Add another song? (y/n):");
                if (!UI.s.nextLine().trim().equalsIgnoreCase("y")) break;
                continue;
            }

            if (results.isEmpty()) {
                System.out.println("No songs found matching \"" + searchTerm + "\". Try a different search.");
                System.out.println("Add another song? (y/n):");
                if (!UI.s.nextLine().trim().equalsIgnoreCase("y")) break;
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
                choice = Integer.parseInt(UI.s.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
                System.out.println("Add another song? (y/n):");
                if (!UI.s.nextLine().trim().equalsIgnoreCase("y")) break;
                continue;
            }

            if (choice == 0) {
                System.out.println("Add another song? (y/n):");
                if (!UI.s.nextLine().trim().equalsIgnoreCase("y")) break;
                continue;
            }

            if (!UI.validateInputRange(1, results.size(), choice)) {
                System.out.println("Add another song? (y/n):");
                if (!UI.s.nextLine().trim().equalsIgnoreCase("y")) break;
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
            if (!UI.s.nextLine().trim().equalsIgnoreCase("y")) break;
        }

        System.out.println("\nDone! Playlist \"" + playlistName + "\" has " + songsAdded + " song(s).");
    }

    public static void inputDeletePlaylist() {
        UI.s.nextLine(); // flush trailing newline from menu's nextInt()

        ArrayList<String[]> playlists;
        try {
            playlists = Playlist.listPlaylists(UI.user.getUsername());
        } catch (SQLException e) {
            System.err.println("Failed to retrieve playlists.");
            System.err.println("Code: " + e.getErrorCode() + " SQLState: " + e.getSQLState());
            return;
        }

        if (playlists.isEmpty()) {
            System.out.println("You have no playlists to delete.");
            return;
        }

        System.out.println("Your playlists:");
        for (int i = 0; i < playlists.size(); i++) {
            String[] row = playlists.get(i);
            System.out.println((i + 1) + ". " + row[1] + " (" + row[2] + " songs)");
        }

        System.out.println("Select a playlist to delete (or 0 to go back):");
        int choice = Integer.parseInt(UI.getValidInput("integer"));

        if (choice == 0) return;
        if (!UI.validateInputRange(1, playlists.size(), choice)) return;

        String[] selected = playlists.get(choice - 1);
        int playlistID = Integer.parseInt(selected[0]);
        String playlistName = selected[1];

        System.out.println("Are you sure you want to delete \"" + playlistName + "\"? (y/n):");
        if (!UI.s.nextLine().trim().equalsIgnoreCase("y")) {
            System.out.println("Deletion cancelled.");
            return;
        }

        try {
            Playlist.delete(playlistID, UI.user.getUsername());
            System.out.println("Playlist \"" + playlistName + "\" deleted.");
        } catch (SQLException e) {
            System.err.println("Failed to delete playlist.");
            System.err.println("Code: " + e.getErrorCode() + " SQLState: " + e.getSQLState());
        }
    }
}
