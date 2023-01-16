package edu.harvard.iq.dataverse;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public class CrossRefRESTfullClient implements Closeable {
    private static final Logger logger = Logger.getLogger(CrossRefRESTfullClient.class.getCanonicalName());

    private final String url;
    private final String username;
    private final String password;
    private final CloseableHttpClient httpClient;
    private final String encoding = "utf-8";

    public CrossRefRESTfullClient(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
        httpClient = HttpClients.createDefault();
    }

    public boolean testDOIExists(String doi) throws IOException {
        HttpGet httpGet = new HttpGet(this.url + "/works/" + doi);
        httpGet.setHeader("Accept", "application/json");
        HttpResponse response = httpClient.execute(httpGet);
        if (response.getStatusLine().getStatusCode() != 200) {
            EntityUtils.consumeQuietly(response.getEntity());
            return false;
        }
        EntityUtils.consumeQuietly(response.getEntity());
        return true;
    }

    public String getMetadata(String doi) {
        HttpGet httpGet = new HttpGet(this.url + "/works/" + doi);
        httpGet.setHeader("Accept", "application/json");
        try {
            HttpResponse response = httpClient.execute(httpGet);
            String data = EntityUtils.toString(response.getEntity(), encoding);
            if (response.getStatusLine().getStatusCode() != 200) {
                String errMsg = "Response from getMetadata: " + response.getStatusLine().getStatusCode() + ", " + data;
                logger.info(errMsg);
                throw new RuntimeException(errMsg);
            }
            return data;
        } catch (IOException ioe) {
            logger.info("IOException when get metadata");
            throw new RuntimeException("IOException when get metadata", ioe);
        }
    }

    public String postMetadata(String xml) throws IOException {
        HttpEntity entity = MultipartEntityBuilder.create()
                .addTextBody("operation", "doMDUpload")
                .addTextBody("login_id", username)
                .addTextBody("login_passwd", password)
                .addBinaryBody("fname", xml.getBytes(StandardCharsets.UTF_8), ContentType.APPLICATION_XML, "metadata.xml")
                .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                .build();
        HttpResponse response = Request.Post(url + "/servlet/deposit")
                .body(entity)
                .setHeader("Accept", "*/*")
                .execute().returnResponse();

        String data = EntityUtils.toString(response.getEntity(), encoding);
        if (response.getStatusLine().getStatusCode() != 200) {
            String errMsg = "Response from postMetadata: " + response.getStatusLine().getStatusCode() + ", " + data;
            logger.info(errMsg);
            throw new IOException(errMsg);
        }
        return data;
    }

    @Override
    public void close() throws IOException {
        if (this.httpClient != null) {
            try {
                httpClient.close();
            } catch (IOException io) {
                logger.warning("IOException closing hhtpClient: " + io.getMessage());
            }
        }
    }
}
