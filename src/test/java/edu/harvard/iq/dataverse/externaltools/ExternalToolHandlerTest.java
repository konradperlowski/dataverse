package edu.harvard.iq.dataverse.externaltools;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.util.SystemConfig;
import javax.json.Json;
import javax.json.JsonObject;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

public class ExternalToolHandlerTest {

    @Test
    public void testToJson() {
        System.out.println("toJson");
        ExternalTool externalTool = new ExternalTool("displayName", "description", "toolUrl", "{}");
        externalTool.setId(42l);
        DataFile dataFile = new DataFile();
        ApiToken apiToken = new ApiToken();
        ExternalToolHandler externalToolHandler = new ExternalToolHandler(externalTool, dataFile, apiToken);
        JsonObject json = externalToolHandler.toJson().build();
        System.out.println("JSON: " + json);
        assertEquals("displayName", json.getString("displayName"));

    }

    // TODO: It would probably be better to split these into individual tests.
    @Test
    public void testGetToolUrlWithOptionalQueryParameters() {
        String toolUrl = "http://example.com";
        ExternalTool externalTool = new ExternalTool("displayName", "description", toolUrl, "{}");

        // One query parameter, not a reserved word, no {fileId} (required) used.
        externalTool.setToolParameters(Json.createObjectBuilder()
                .add("queryParameters", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("mode", "mode1")
                        )
                )
                .build().toString());
        DataFile nullDataFile = null;
        ApiToken nullApiToken = null;
        Exception expectedException1 = null;
        try {
            ExternalToolHandler externalToolHandler1 = new ExternalToolHandler(externalTool, nullDataFile, nullApiToken);
        } catch (Exception ex) {
            expectedException1 = ex;
        }
        assertNotNull(expectedException1);
        assertEquals("A DataFile is required.", expectedException1.getMessage());

        // Two query parameters.
        externalTool.setToolParameters(Json.createObjectBuilder()
                .add("queryParameters", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("mode", "mode1")
                        )
                        .add(Json.createObjectBuilder()
                                .add("key2", "value2")
                        )
                )
                .build().toString());
        Exception expectedException2 = null;
        try {
            ExternalToolHandler externalToolHandler2 = new ExternalToolHandler(externalTool, nullDataFile, nullApiToken);
        } catch (Exception ex) {
            expectedException2 = ex;
        }
        assertNotNull(expectedException2);
        assertEquals("A DataFile is required.", expectedException2.getMessage());

        // Two query parameters, both reserved words, one is {fileId} which is required.
        externalTool.setToolParameters(Json.createObjectBuilder()
                .add("queryParameters", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("key1", "{fileId}")
                        )
                        .add(Json.createObjectBuilder()
                                .add("key2", "{apiToken}")
                        )
                )
                .build().toString());
        DataFile dataFile = new DataFile();
        dataFile.setId(42l);
        ApiToken apiToken = new ApiToken();
        apiToken.setTokenString("7196b5ce-f200-4286-8809-03ffdbc255d7");
        ExternalToolHandler externalToolHandler3 = new ExternalToolHandler(externalTool, dataFile, apiToken);
        String result3 = externalToolHandler3.getQueryParametersForUrl();
        System.out.println("result3: " + result3);
        assertEquals("?key1=42&key2=7196b5ce-f200-4286-8809-03ffdbc255d7", result3);

        // Two query parameters, both reserved words, no apiToken
        externalTool.setToolParameters(Json.createObjectBuilder()
                .add("queryParameters", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("key1", "{fileId}")
                        )
                        .add(Json.createObjectBuilder()
                                .add("key2", "{apiToken}")
                        )
                )
                .build().toString());
        ExternalToolHandler externalToolHandler4 = new ExternalToolHandler(externalTool, dataFile, nullApiToken);
        String result4 = externalToolHandler4.getQueryParametersForUrl();
        System.out.println("result4: " + result4);
        assertEquals("?key1=42&key2=null", result4);

        // Two query parameters, attempt to use a reserved word that doesn't exist.
        externalTool.setToolParameters(Json.createObjectBuilder()
                .add("queryParameters", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("key1", "{noSuchReservedWord}")
                        )
                        .add(Json.createObjectBuilder()
                                .add("key2", "{apiToken}")
                        )
                )
                .build().toString());
        Exception expectedException = null;
        try {
            ExternalToolHandler externalToolHandler5 = new ExternalToolHandler(externalTool, dataFile, nullApiToken);
            String result5 = externalToolHandler5.getQueryParametersForUrl();
            System.out.println("result5: " + result5);
        } catch (Exception ex) {
            System.out.println("Exception caught: " + ex);
            expectedException = ex;
        }
        assertNotNull(expectedException);
        assertEquals("Unknown reserved word: {noSuchReservedWord}", expectedException.getMessage());

    }

    @Test
    public void testParseAwesomeTool() {
        String toolUrl = "https://amazingtool.com";
        ExternalTool externalTool = new ExternalTool("Amazing Tool", "The most amazing tool.", toolUrl, "{}");

        // somewhat sophisticated external tool specification with multiple reserved words in one query parameter
        externalTool.setToolParameters(Json.createObjectBuilder()
                .add("queryParameters", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("uri", "{siteUrl}/api/access/datafile/{fileId}/metadata/ddi")
                        )
                )
                .build().toString());
        DataFile dataFile = new DataFile();
        dataFile.setId(42l);
        ApiToken nullApiToken = null;
        // Try setting the kludge to true to see how we want the parser to work.
        if (ExternalToolHandler.hardCodedKludgeForDataExplorer) {
            System.out.println("kludge is in place");
            // We set this property in this test so that it gets inserted by a call to SystemConfig.getDataverseSiteUrlStatic() in ExternalToolHandler.java.
            System.setProperty(SystemConfig.SITE_URL, "https://dataverse.example.com");
            ExternalToolHandler externalToolHandler = new ExternalToolHandler(externalTool, dataFile, nullApiToken);
            // The example from https://github.com/scholarsportal/Dataverse-Data-Explorer is this:
            // https://scholarsportal.github.io/Dataverse-Data-Explorer/?uri=https://dataverse.scholarsportal.info/api/access/datafile/8988/metadata/ddi
            String queryParams = externalToolHandler.getQueryParametersForUrl();
            System.out.println("query params: " + queryParams);
            assertEquals("?uri=https://dataverse.example.com/api/access/datafile/42/metadata/ddi", queryParams);
        } else {
            System.out.println("no kludge");
            Exception expectedException = null;
            try {
                ExternalToolHandler externalToolHandler = new ExternalToolHandler(externalTool, dataFile, nullApiToken);
                System.out.println("queryParams: " + externalToolHandler.getQueryParametersForUrl());
            } catch (Exception ex) {
                System.out.println("ex: " + ex);
                expectedException = ex;
                assertNotNull(expectedException);
                assertEquals("Unknown reserved word: {siteUrl}/api/access/datafile/{fileId}/metadata/ddi", expectedException.getMessage());
            }
        }
    }

}
