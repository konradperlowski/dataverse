package edu.harvard.iq.dataverse.externaltools;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataTable;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.util.ArrayList;
import java.util.List;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

public class ExternalToolServiceBeanTest {

    public ExternalToolServiceBeanTest() {
    }

//test commented out because now there is no way to get all the handlers for a file...
    @Test
    public void testfindAll() {
        DataFile dataFile = new DataFile();
        dataFile.setId(42l);
        List<DataTable> dataTables = new ArrayList<DataTable>();
        dataTables.add(new DataTable());
        dataFile.setDataTables(dataTables);
        ApiToken apiToken = new ApiToken();
        apiToken.setTokenString("7196b5ce-f200-4286-8809-03ffdbc255d7");
        ExternalTool externalTool = new ExternalTool("displayName", "description", "http://foo.com", "{}");
        ExternalToolHandler externalToolHandler4 = new ExternalToolHandler(externalTool, dataFile, apiToken);
        List<ExternalTool> externalTools = new ArrayList<>();
        externalTools.add(externalTool);
        List<ExternalTool> availableExternalTools = ExternalToolServiceBean.findExternalToolsByFile(externalTools, dataFile);
        assertEquals(availableExternalTools.size(), 1);
    }

    @Test
    public void testParseAddExternalToolInput() {
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("displayName", "AwesomeTool");
        job.add("description", "This tool is awesome.");
        job.add("toolUrl", "http://awesometool.com");
        job.add("toolParameters", Json.createObjectBuilder()
                .add("queryParameters", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("fileid", "{fileId}")
                                .build())
                        .add(Json.createObjectBuilder()
                                .add("key", "{apiToken}")
                                .build())
                        .build())
                .build());
        String tool = job.build().toString();
        System.out.println("tool: " + tool);
        ExternalTool externalTool = ExternalToolServiceBean.parseAddExternalToolManifest(tool);
        assertEquals("AwesomeTool", externalTool.getDisplayName());
        DataFile dataFile = new DataFile();
        dataFile.setId(42l);
        ApiToken apiToken = new ApiToken();
        apiToken.setTokenString("7196b5ce-f200-4286-8809-03ffdbc255d7");
        ExternalToolHandler externalToolHandler = new ExternalToolHandler(externalTool, dataFile, apiToken);
        String toolUrl = externalToolHandler.getToolUrlWithQueryParams();
        System.out.println("result: " + toolUrl);
        assertEquals("http://awesometool.com?fileid=42&key=7196b5ce-f200-4286-8809-03ffdbc255d7", toolUrl);
    }

    @Test
    public void testParseAddExternalToolInputMultipleReservedWords() {
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("displayName", "Amazing Tool");
        job.add("description", "This tool is amazing.");
        job.add("toolUrl", "http://amazingtool.com");
        job.add("toolParameters", Json.createObjectBuilder()
                .add("queryParameters", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("uri", "{siteUrl}/api/access/datafile/{fileId}/metadata/ddi")
                                .build())
                        .build())
                .build());
        String tool = job.build().toString();
        System.out.println("tool: " + tool);
        ExternalTool externalTool = null;
        try {
            externalTool = ExternalToolServiceBean.parseAddExternalToolManifest(tool);
        } catch (Exception ex) {
            assertNotNull(ex);
            assertEquals("Unknown reserved word: {siteUrl}/api/access/datafile/{fileId}/metadata/ddi", ex.getMessage());
            System.out.println("Returning early. Try setting hardCodedKludgeForDataExplorer to true.");
            return;
        }
        assertEquals("Amazing Tool", externalTool.getDisplayName());
        DataFile dataFile = new DataFile();
        dataFile.setId(42l);
        ApiToken apiToken = new ApiToken();
        apiToken.setTokenString("7196b5ce-f200-4286-8809-03ffdbc255d7");
        // Try setting the kludge to true to see how we want the parser to work.
        if (ExternalToolHandler.hardCodedKludgeForDataExplorer) {
            // We set this property in this test so that it gets inserted by a call to SystemConfig.getDataverseSiteUrlStatic() in ExternalToolHandler.java.
            System.setProperty(SystemConfig.SITE_URL, "https://dataverse.example.com");
            ExternalToolHandler externalToolHandler = new ExternalToolHandler(externalTool, dataFile, apiToken);
            String toolUrl = externalToolHandler.getToolUrlWithQueryParams();
            System.out.println("result: " + toolUrl);
            assertEquals("http://amazingtool.com?uri=https://dataverse.example.com/api/access/datafile/42/metadata/ddi", toolUrl);
        }
    }

    @Test
    public void testParseAddExternalToolInputNoFileId() {
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("displayName", "AwesomeTool");
        job.add("description", "This tool is awesome.");
        job.add("toolUrl", "http://awesometool.com");
        job.add("toolParameters", Json.createObjectBuilder()
                .add("queryParameters", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("key", "{apiToken}")
                                .build())
                        .build())
                .build());
        String tool = job.build().toString();
        System.out.println("tool: " + tool);
        Exception expectedException = null;
        try {
            ExternalTool externalTool = ExternalToolServiceBean.parseAddExternalToolManifest(tool);
        } catch (Exception ex) {
            expectedException = ex;
        }
        assertNotNull(expectedException);
        assertEquals("Required reserved word not found: {fileId}", expectedException.getMessage());
    }

    @Test
    public void testParseAddExternalToolInputNull() {
        Exception expectedException = null;
        try {
            ExternalTool externalTool = ExternalToolServiceBean.parseAddExternalToolManifest(null);
        } catch (Exception ex) {
            expectedException = ex;
        }
        assertNotNull(expectedException);
        assertEquals("External tool manifest was null or empty!", expectedException.getMessage());
    }

    @Test
    public void testParseAddExternalToolInputEmptyString() {
        Exception expectedException = null;
        try {
            ExternalTool externalTool = ExternalToolServiceBean.parseAddExternalToolManifest("");
        } catch (Exception ex) {
            expectedException = ex;
        }
        assertNotNull(expectedException);
        assertEquals("External tool manifest was null or empty!", expectedException.getMessage());
    }

    @Test
    public void testParseAddExternalToolInputUnknownReservedWord() {
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("displayName", "AwesomeTool");
        job.add("description", "This tool is awesome.");
        job.add("toolUrl", "http://awesometool.com");
        job.add("toolParameters", Json.createObjectBuilder()
                .add("queryParameters", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("fileid", "{fileId}")
                                .build())
                        .add(Json.createObjectBuilder()
                                .add("key", "{apiToken}")
                                .build())
                        .add(Json.createObjectBuilder()
                                .add("mode", "mode1")
                                .build())
                        .build())
                .build());
        String tool = job.build().toString();
        System.out.println("tool: " + tool);
        Exception expectedException = null;
        try {
            ExternalTool externalTool = ExternalToolServiceBean.parseAddExternalToolManifest(tool);
        } catch (Exception ex) {
            expectedException = ex;
        }
        assertNotNull(expectedException);
        assertEquals("Unknown reserved word: mode1", expectedException.getMessage());
    }

    @Test
    public void testParseAddExternalToolInputNoDisplayName() {
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("description", "This tool is awesome.");
        job.add("toolUrl", "http://awesometool.com");
        job.add("toolParameters", Json.createObjectBuilder().build());
        String tool = job.build().toString();
        System.out.println("tool: " + tool);
        Exception expectedException = null;
        try {
            ExternalTool externalTool = ExternalToolServiceBean.parseAddExternalToolManifest(tool);
        } catch (Exception ex) {
            expectedException = ex;
        }
        assertNotNull(expectedException);
        assertEquals("displayName is required.", expectedException.getMessage());
    }

    @Test
    public void testParseAddExternalToolInputNoDescription() {
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("displayName", "AwesomeTool");
        job.add("toolUrl", "http://awesometool.com");
        job.add("toolParameters", Json.createObjectBuilder().build());
        String tool = job.build().toString();
        System.out.println("tool: " + tool);
        Exception expectedException = null;
        try {
            ExternalTool externalTool = ExternalToolServiceBean.parseAddExternalToolManifest(tool);
        } catch (Exception ex) {
            expectedException = ex;
        }
        assertNotNull(expectedException);
        assertEquals("description is required.", expectedException.getMessage());
    }

    @Test
    public void testParseAddExternalToolInputNoToolUrl() {
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("displayName", "AwesomeTool");
        job.add("description", "This tool is awesome.");
        job.add("toolParameters", Json.createObjectBuilder().build());
        String tool = job.build().toString();
        System.out.println("tool: " + tool);
        Exception expectedException = null;
        try {
            ExternalTool externalTool = ExternalToolServiceBean.parseAddExternalToolManifest(tool);
        } catch (Exception ex) {
            expectedException = ex;
        }
        assertNotNull(expectedException);
        assertEquals("toolUrl is required.", expectedException.getMessage());
    }

}
