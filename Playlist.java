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

//    adds a song to a playlist.

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
}
