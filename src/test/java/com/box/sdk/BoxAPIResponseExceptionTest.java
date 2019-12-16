package com.box.sdk;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.eclipsesource.json.JsonObject;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

/**
 *
 */
public class BoxAPIResponseExceptionTest {

    @ClassRule
    public static final WireMockClassRule WIRE_MOCK_CLASS_RULE = new WireMockClassRule(53621);

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(53620);

    private BoxAPIConnection api = TestConfig.getAPIConnection();

    @Test
    @Category(UnitTest.class)
    public void testAPIResponseExceptionReturnsCorrectErrorMessage() throws MalformedURLException {
        BoxAPIConnection api = new BoxAPIConnection("");

        final JsonObject fakeJSONResponse = JsonObject.readFrom("{\n"
                + "            \"type\": \"error\",\n"
                + "            \"status\": \"409\",\n"
                + "            \"code\": \"item_name_in_use\",\n"
                + "            \"context_info\": {\n"
                + "            \"conflicts\": [\n"
                + "                 {\n"
                + "                     \"type\": \"folder\",\n"
                + "                     \"id\": \"12345\",\n"
                + "                     \"sequence_id\": \"1\",\n"
                + "                     \"etag\": \"1\",\n"
                + "                     \"name\": \"Helpful things\"\n"
                + "                 }\n"
                + "              ]\n"
                + "            },\n"
                + "            \"help_url\": \"http://developers.box.com/docs/#errors\",\n"
                + "            \"message\": \"Item with the same name already exists\",\n"
                + "            \"request_id\": \"5678\"\n"
                + "         }");

        stubFor(post(urlEqualTo("/folders"))
                .willReturn(aResponse()
                        .withStatus(409)
                        .withHeader("Content-Type", "application/json")
                        .withBody(fakeJSONResponse.toString())));

        URL url = new URL("http://localhost:53620/folders");
        BoxAPIRequest request = new BoxAPIRequest(api, url, "POST");

        try {
            BoxJSONResponse response = (BoxJSONResponse) request.send();
        } catch (BoxAPIResponseException e) {
            Assert.assertEquals(409, e.getResponseCode());
            Assert.assertEquals("The API returned an error code [409 | 5678] item_name_in_use - "
                    + "Item with the same name already exists", e.getMessage());
            return;
        }

        Assert.fail("Never threw a BoxAPIResponseException");
    }

    @Test
    @Category(UnitTest.class)
    public void testAPIResponseExceptionMissingFieldsReturnsCorrectErrorMessage() throws MalformedURLException {
        BoxAPIConnection api = new BoxAPIConnection("");
        final JsonObject fakeJSONResponse = JsonObject.readFrom("{\n"
                + "            \"type\": \"error\",\n"
                + "            \"status\": \"409\",\n"
                + "            \"context_info\": {\n"
                + "            \"conflicts\": [\n"
                + "                 {\n"
                + "                     \"type\": \"folder\",\n"
                + "                     \"id\": \"12345\",\n"
                + "                     \"sequence_id\": \"1\",\n"
                + "                     \"etag\": \"1\",\n"
                + "                     \"name\": \"Helpful things\"\n"
                + "                 }\n"
                + "              ]\n"
                + "            },\n"
                + "            \"help_url\": \"http://developers.box.com/docs/#errors\"\n"
                + "        }");

        stubFor(post(urlEqualTo("/folders"))
                .willReturn(aResponse()
                        .withStatus(409)
                        .withHeader("Content-Type", "application/json")
                        .withBody(fakeJSONResponse.toString())));

        URL url = new URL("http://localhost:53620/folders");
        BoxAPIRequest request = new BoxAPIRequest(api, url, "POST");

        try {
            BoxJSONResponse response = (BoxJSONResponse) request.send();
        } catch (BoxAPIResponseException e) {
            Assert.assertEquals(409, e.getResponseCode());
            Assert.assertEquals("The API returned an error code [409]", e.getMessage());
            return;
        }

        Assert.fail("Never threw a BoxAPIResponseException");
    }

    @Test
    @Category(UnitTest.class)
    public void testAPIResponseExceptionMissingBodyReturnsCorrectErrorMessage() throws MalformedURLException {
        BoxAPIConnection api = new BoxAPIConnection("");

        stubFor(post(urlEqualTo("/folders"))
                .willReturn(aResponse()
                        .withStatus(403)));

        URL url = new URL("http://localhost:53620/folders");
        BoxAPIRequest request = new BoxAPIRequest(api, url, "POST");

        try {
            BoxJSONResponse response = (BoxJSONResponse) request.send();
        } catch (BoxAPIResponseException e) {
            Assert.assertEquals(403, e.getResponseCode());
            Assert.assertEquals("", e.getResponse());
            Assert.assertEquals("The API returned an error code [403]", e.getMessage());
            return;
        }

        Assert.fail("Never threw a BoxAPIResponseException");
    }

    @Test
    @Category(UnitTest.class)
    public void testAPIResponseExceptionWithHTMLBodyReturnsCorrectErrorMessage() throws MalformedURLException {

        String body = "<html><body><h1>500 Server Error</h1></body></html>";

        BoxAPIConnection api = new BoxAPIConnection("");

        stubFor(post(urlEqualTo("/folders"))
                .willReturn(aResponse()
                        .withBody(body)
                        .withStatus(500)));

        URL url = new URL("http://localhost:53620/folders");
        BoxAPIRequest request = new BoxAPIRequest(api, url, "POST");

        try {
            BoxJSONResponse response = (BoxJSONResponse) request.send();
        } catch (BoxAPIResponseException e) {
            Assert.assertEquals(500, e.getResponseCode());
            Assert.assertEquals(body, e.getResponse());
            Assert.assertEquals("The API returned an error code [500]", e.getMessage());
            return;
        }

        Assert.fail("Never threw a BoxAPIResponseException");
    }

    @Test
    @Category(UnitTest.class)
    public void testResponseExceptionHeadersIsCaseInsensitive() {
        Map<String, String> headers = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
        headers.put("FOO", "bAr");
        BoxAPIResponse responseObject = new BoxAPIResponse(202, headers);
        BoxAPIResponseException responseException = new BoxAPIResponseException("Test Message", responseObject);

        Assert.assertTrue(responseException.getHeaders().containsKey("foo"));
        Assert.assertTrue(responseException.getHeaders().containsKey("fOo"));
        Assert.assertTrue(responseException.getHeaders().containsKey("FOO"));
        Assert.assertEquals("bAr", responseException.getHeaders().get("foo").get(0));
    }

    @Test
    @Category(UnitTest.class)
    public void testGetResponseHeadersWithNoRequestID() throws IOException {
        String result = "";
        final String userURL = "/users/12345";

        result = TestConfig.getFixture("BoxException/BoxResponseException403");

        WIRE_MOCK_CLASS_RULE.stubFor(WireMock.get(WireMock.urlPathEqualTo(userURL))
                .willReturn(WireMock.aResponse()
                        .withHeader("BOX-REQUEST-ID", "11111")
                        .withStatus(403)
                        .withBody(result)));

        try {
            BoxUser user = new BoxUser(this.api, "12345");
            BoxUser.Info userInfo = user.getInfo();
        } catch (Exception e) {
            Assert.assertEquals("The API returned an error code [403 | .11111] Forbidden", e.getMessage());
        }
    }

    @Test
    @Category(UnitTest.class)
    public void testGetResponseExceptionCorrectlyWithAllID() throws IOException {
        String result = "";
        final String userURL = "/users/12345";

        result = TestConfig.getFixture("BoxException/BoxResponseException403WithRequestID");

        WIRE_MOCK_CLASS_RULE.stubFor(WireMock.get(WireMock.urlPathEqualTo(userURL))
                .willReturn(WireMock.aResponse()
                        .withHeader("BOX-REQUEST-ID", "11111")
                        .withStatus(403)
                        .withBody(result)));

        try {
            BoxUser user = new BoxUser(this.api, "12345");
            BoxUser.Info userInfo = user.getInfo();
        } catch (Exception e) {
            Assert.assertEquals("The API returned an error code [403 | 22222.11111] Forbidden", e.getMessage());
        }
    }

    @Test
    @Category(UnitTest.class)
    public void testGetResponseExceptionErrorAndErrorDescription() throws IOException {
        String result = "";
        final String userURL = "/users/12345";

        result = TestConfig.getFixture("BoxException/BoxResponseException400WithErrorAndErrorDescription");

        WIRE_MOCK_CLASS_RULE.stubFor(WireMock.get(WireMock.urlPathEqualTo(userURL))
                .willReturn(WireMock.aResponse()
                        .withHeader("BOX-REQUEST-ID", "11111")
                        .withStatus(403)
                        .withBody(result)));


        try {
            BoxUser user = new BoxUser(this.api, "12345");
            BoxUser.Info userInfo = user.getInfo();
        } catch (Exception e) {
            Assert.assertEquals("The API returned an error code [403 | 22222.11111] Forbidden - "
                    + "Unauthorized Access", e.getMessage());
        }

    }

    @Test
    @Category(UnitTest.class)
    public void testGetResponseHeadersCorrectlyWithEmptyBody() throws IOException {
        String result = "";
        final String userURL = "/users/12345";

        WIRE_MOCK_CLASS_RULE.stubFor(WireMock.get(WireMock.urlPathEqualTo(userURL))
                .willReturn(WireMock.aResponse()
                        .withHeader("BOX-REQUEST-ID", "11111")
                        .withStatus(403)));

        try {
            BoxUser user = new BoxUser(this.api, "12345");
            BoxUser.Info userInfo = user.getInfo();
        } catch (Exception e) {
            Assert.assertEquals("The API returned an error code [403 | .11111]", e.getMessage());
        }
    }
}
