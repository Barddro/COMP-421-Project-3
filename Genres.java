public class Genres {
    private static String[] genres = new String[0];
    private static boolean dirty = true;

    private static void updateGenresInternal() {
        if(dirty) {
            // execute query below to obtain list of all genres
            //something like:
            //  genres = execute_query(SELECT * FROM Genres)
            dirty = false;
        }

    }

    public static String[] getGenres() {
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
        try {
            // USE A PREPARED STATEMENT HERE TO INSERT GENRE
            dirty = true;
        }
        catch(Exception e) {
            System.err.println("Error occurred inserting genre " + genre + " into the db");
        }
    }
}
