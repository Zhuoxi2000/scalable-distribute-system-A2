import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zaxxer.hikari.HikariDataSource;
import io.swagger.client.model.AlbumsProfile;
import io.swagger.client.model.ImageMetaData;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@WebServlet(name = "AlbumsServlet", value = "/albums/*")
@MultipartConfig(fileSizeThreshold = 1024 * 1024 * 10,
        maxFileSize = 1024 * 1024 * 50,
        maxRequestSize = 1024 * 1024 * 100)
public class AlbumsServlet extends HttpServlet {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private DataSer dataSer;
    private HikariDataSource connectionPool;
    private albumCon ac;

    @Override
    public void init() {
        this.ac = new albumCon();
        this.dataSer = new DataSer();
        this.connectionPool = this.dataSer.getConnectionPool();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        String urlPath = req.getPathInfo();

        if (urlPath == null || urlPath.isEmpty()) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            res.getWriter().write("missing parameters");
            return;
        }

        if (!isUrlValid(urlPath)) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.getWriter().write("invalid request");
            return;
        }

        String albumId = urlPath.split("/")[1];

        try (Connection connection = this.connectionPool.getConnection()) {
            ResultSet resultSet = this.ac.getAlbumProfile(connection, albumId);

            if (resultSet.next()) {
                res.setStatus(HttpServletResponse.SC_OK);
                res.getWriter().write(resultSet.getString("AlbumProfile"));
            } else {
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                res.getWriter().write("Not found");
            }
        } catch (SQLException e) {
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            res.getWriter().write("Error with the database");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("application/json");

        String urlPath = req.getPathInfo();
        String servletPath = req.getServletPath();

        if (urlPath != null || !isUrlValid(servletPath)) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.getWriter().write("invalid request");
            return;
        }

        Part image = req.getPart("image");
        Part albumProfilePart = req.getPart("profile");

        if (image == null || !isImageContentType(image.getContentType()) || albumProfilePart == null) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.getWriter().write("Missing Image or Wrong Profile");
            return;
        }

        String[] albumProfileParsed = parseAlbumProfile(albumProfilePart);
        String artist = albumProfileParsed[0];
        String title = albumProfileParsed[1];
        String year = albumProfileParsed[2];

        if (artist == null || title == null || year == null) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.getWriter().write("Wrong with the profile ");
            return;
        }

        String uuid = String.valueOf(UUID.randomUUID());
        String imageSize = String.valueOf(image.getSize());

        String imageData = gson.toJson(new ImageMetaData().albumID(uuid).imageSize(imageSize));
        String albumProfile = gson.toJson(new AlbumsProfile().artist(artist).title(title).year(year));

        try (Connection connection = this.connectionPool.getConnection()) {
            int rowsAffected = this.ac.postToDatabase(connection, uuid, imageData, albumProfile);

            if (rowsAffected > 0) {
                res.setStatus(HttpServletResponse.SC_OK);
                res.getWriter().write(imageData);
            } else {
                res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                res.getWriter().write("Error when posting to database");
            }
        } catch (SQLException e) {
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            res.getWriter().write("There was an error with the database");
        }
    }

    private String[] parseAlbumProfile(Part albumProfilePart) throws IOException {
        //throwexception
        String jsonContent = new String(albumProfilePart.getInputStream().readAllBytes());

        String artistRegex = "artist: (.*?)\\n";
        String titleRegex = "title: (.*?)\\n";
        String yearRegex = "year: (.*?)\\n";

        String artist = extractValue(jsonContent, artistRegex);
        String title = extractValue(jsonContent, titleRegex);
        String year = extractValue(jsonContent, yearRegex);

        return new String[]{artist, title, year};
    }

    private String extractValue(String input, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    private boolean isImageContentType(String contentType) {
        return contentType != null && contentType.startsWith("image/");
    }

    private boolean isUrlValid(String urlPath) {
        for (Endpoint endpoint : Endpoint.values()) {
            Pattern pattern = endpoint.pattern;

            if (pattern.matcher(urlPath).matches()) {
                return true;
            }
        }

        return false;
    }

    private enum Endpoint {
        POST_NEW_ALBUM(Pattern.compile("/albums")), GET_ALBUM_BY_KEY(Pattern.compile("^/[^/]+$"));

        public final Pattern pattern;

        Endpoint(Pattern pattern) {
            this.pattern = pattern;
        }
    }

    @Override
    public void destroy() {
        dataSer.close();
    }
}
