import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class albumCon {

    albumCon() {
    }

    ResultSet getAlbumProfile(Connection connection, String albumId) throws SQLException
    {
        String selectQuery = "SELECT AlbumProfile FROM albumRequests WHERE AlbumID = ?";
        PreparedStatement preparedStatement = connection.prepareStatement(selectQuery);

        preparedStatement.setString(1, albumId);
        return preparedStatement.executeQuery();
    }

    int postToDatabase(Connection connection, String uuid, String imageData, String albumProfile) throws SQLException {
        String insertQuery = "INSERT INTO albumRequests (AlbumID, ImageData, AlbumProfile) VALUES (?, ?, ?)";
        PreparedStatement preparedStatement = connection.prepareStatement(insertQuery);

        preparedStatement.setString(1, uuid);
        preparedStatement.setString(2, imageData);
        preparedStatement.setString(3, albumProfile);

        return preparedStatement.executeUpdate();
    }
}
