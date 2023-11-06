package Client;

import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.DefaultApi;
import io.swagger.client.model.AlbumInfo;
import io.swagger.client.model.AlbumsProfile;
import io.swagger.client.model.ImageMetaData;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class albumRun implements Runnable {
    private final int numReqs;
    private final DefaultApi albumsApi;
    private final boolean initializationPhase;
    private int successfulReq;
    private int failedReq;
    private final List<Long> latenciesPost;
    private final List<Long> latenciesGet;
    private int counter;

    public albumRun(int numReqs, String serverUrl, boolean initializationPhase) {
        this.numReqs = numReqs;
        this.albumsApi = new DefaultApi();
        this.albumsApi.getApiClient().setBasePath(serverUrl);
        this.initializationPhase = initializationPhase;
        this.successfulReq = 0;
        this.failedReq = 0;
        this.latenciesPost = new ArrayList<>();
        this.latenciesGet = new ArrayList<>();
        this.counter = 1999;
    }

    @Override
    public void run() {
        long start, end, currentLatency;
        ApiResponse<?> responseCode;

        for (int k = 0; k < this.numReqs; k++) {
            // Make POST request
            start = System.currentTimeMillis();
            responseCode = makeApiRequest("POST", null);
            end = System.currentTimeMillis();
            currentLatency = end - start;
            this.latenciesPost.add(currentLatency);

            // Make GET request
            start = System.currentTimeMillis();
            makeApiRequest("GET", getPostUUID(responseCode));
            end = System.currentTimeMillis();
            currentLatency = end - start;
            this.latenciesGet.add(currentLatency);
        }

        albumClt.totalThreadsLatch.countDown();

        if (initializationPhase) {
            return;
        }

        albumClt.SUCCESSFUL_REQ.addAndGet(this.successfulReq);
        albumClt.FAILED_REQ.addAndGet(this.failedReq);
        albumClt.latenciesPost.addAll(this.latenciesPost);
        albumClt.latenciesGet.addAll(this.latenciesGet);
    }

    private ApiResponse<?> makeApiRequest(String requestMethod, String requestParameters) {
        ApiResponse<?> response = null;
        int attempts = 0;
        boolean isGetReq = requestMethod.equals("GET");

        int maxRetries = 5;
        while (attempts < maxRetries) {
            try {
                response = isGetReq ? getAlbum(requestParameters) : postAlbum();
                if (response.getStatusCode() == 200) {
                    this.successfulReq += 1;
                    return response;
                }
                attempts++;
            } catch (ApiException e) {
                attempts++;
            }
        }

        this.failedReq += 1;

        assert response != null;
        return response;
    }

    private ApiResponse<AlbumInfo> getAlbum(String albumID) throws ApiException {
        return this.albumsApi.getAlbumByKeyWithHttpInfo(albumID);
    }

    private ApiResponse<ImageMetaData> postAlbum() throws ApiException {
        File image = new File("src/main/java/timage.png");
        AlbumsProfile profile = new AlbumsProfile().artist("Monkey D. Luffy").title("One Piece").year(String.valueOf(this.counter++));
        return this.albumsApi.newAlbumWithHttpInfo(image, profile);
    }

    private String getPostUUID(ApiResponse<?> response) {
        ImageMetaData imageMetaData = (ImageMetaData) response.getData();
        return imageMetaData.getAlbumID();
    }
}
