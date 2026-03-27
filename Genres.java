import utils.DAO;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;

/**
 * Utility class used to cache the list of genres st. we do not need to reobtain every time from the DB, only once made dirty (any insert has occurred)
 */
public class Genres {
    private static HashSet<String> genres = new HashSet<>();
    private static boolean dirty = true;

    private static void updateGenresInternal() {
        if(dirty) {
            try (Statement stmt = DAO.getConnection().createStatement();) {
                String query = "SELECT * FROM Genre";
                ResultSet results = stmt.executeQuery(query);

                while(results.next()) {
                    Genres.genres.add(results.getString("name"));
                }

                dirty = false;
            }
            catch (SQLException e) {
                throw new RuntimeException(e);
            }

        }

    }

    public static HashSet<String> getGenres() {
        updateGenresInternal();
        return genres;
    }
    public static boolean validateGenre(String genre) {
        genre = genre.toLowerCase();

        updateGenresInternal();

        for (String actualGenre: genres) {
            if (genre.equals(actualGenre)) {
                return true;
            }
        }
        return false;
    }

    public static void addGenre(String genre) {
        genre = genre.toLowerCase();

        String query = "INSERT INTO Genre" +
                "VALUES ('?');";

        try (PreparedStatement stmt = DAO.getConnection().prepareStatement(query)){
            stmt.setString(1, genre);
            stmt.executeUpdate();
            dirty = true;
        }
        catch(SQLException e) {
            if("23505".equals(e.getSQLState()) || e.getErrorCode() == -803) {
                System.out.println("Genre " + genre + "already exists in the db");
            }
            else {
                System.err.println("Error occurred inserting genre " + genre + " into the db");
            }
        }
    }
}
