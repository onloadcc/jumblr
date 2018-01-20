package com.tumblr.jumblr.request;

import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.tumblr.jumblr.JumblrClient;
import com.tumblr.jumblr.exceptions.JumblrException;
import com.tumblr.jumblr.responses.ResponseWrapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.legacy.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Response.class)
public class RequestBuilderTest {

  private RequestBuilder rb;

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Before public void setup() {
    rb = new RequestBuilder(null);
  }

  @Test public void testClearEmptyJson() throws IOException {
    Response r = mock(Response.class);
    when(r.getCode()).thenReturn(200);
    when(r.getBody()).thenReturn("");

    thrown.expect(JumblrException.class);
    ResponseWrapper got = rb.clear(r);
  }

  @Test public void testGetParams() {
    JumblrClient client = new JumblrClient("abc", "def");
    client.getRequestBuilder().setHostname("test.com");
    Map<String, String> map = new HashMap<String, String>();
    map.put("limit", "1");
    OAuthRequest request = client.getRequestBuilder().constructGet("/path", map);

    assertEquals(request.getUrl(), "https://test.com/v2/path");
    assertEquals(request.getQueryStringParams().asFormUrlEncodedString(), "limit=1");
  }

  @Test public void testXauthForbidden() throws IOException {
    Response r = mock(Response.class);
    when(r.getCode()).thenReturn(403);
    when(r.getBody()).thenReturn("");

    thrown.expect(JumblrException.class);
    rb.clearXAuth(r);
  }

  @Test public void testXauthSuccess() throws IOException {
    Response r = mock(Response.class);
    when(r.getCode()).thenReturn(200);
    when(r.getBody()).thenReturn("oauth_token=valueForToken&oauth_token_secret=valueForSecret");

    OAuth1AccessToken token = rb.clearXAuth(r);
    assertEquals(token.getToken(), "valueForToken");
    assertEquals(token.getTokenSecret(), "valueForSecret");
  }

  @Test public void testXauthSuccessWithExtra() throws IOException {
    Response r = mock(Response.class);
    when(r.getCode()).thenReturn(201);
    when(r.getBody()).thenReturn(
        "oauth_token=valueForToken&oauth_token_secret=valueForSecret&other=paramisokay");

    OAuth1AccessToken token = rb.clearXAuth(r);
    assertEquals(token.getToken(), "valueForToken");
    assertEquals(token.getTokenSecret(), "valueForSecret");
  }

  @Test public void testXauthBadResponseGoodCode() throws IOException {
    Response r = mock(Response.class);
    when(r.getCode()).thenReturn(200);
    when(r.getBody()).thenReturn("");

    thrown.expect(JumblrException.class);
    rb.clearXAuth(r);
  }
}
