package com.tumblr.jumblr.request;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth10aService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.tumblr.jumblr.JumblrClient;
import com.tumblr.jumblr.TumblrApi;
import com.tumblr.jumblr.exceptions.JumblrException;
import com.tumblr.jumblr.responses.JsonElementDeserializer;
import com.tumblr.jumblr.responses.ResponseWrapper;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Where requests are made from
 *
 * @author jc
 */
public class RequestBuilder {

  private OAuth1AccessToken token;
  private OAuth10aService service;
  private String hostname = "api.tumblr.com";
  private String xauthEndpoint = "https://www.tumblr.com/oauth/access_token";
  private String version = "0.0.11";
  private final JumblrClient client;

  public RequestBuilder(JumblrClient client) {
    this.client = client;
  }

  public String getRedirectUrl(String path) {
    OAuthRequest request = this.constructGet(path, null);
    sign(request);
    boolean presetVal = HttpURLConnection.getFollowRedirects();
    HttpURLConnection.setFollowRedirects(false);
    Response response = execute(request);
    HttpURLConnection.setFollowRedirects(presetVal);
    if (response.getCode() == 301 || response.getCode() == 302) {
      return response.getHeader("Location");
    } else {
      throw new JumblrException(response);
    }
  }

  public ResponseWrapper postMultipart(String path, Map<String, ?> bodyMap) throws IOException {
    OAuthRequest request = this.constructPost(path, bodyMap);
    sign(request);
    OAuthRequest newRequest = RequestBuilder.convertToMultipart(request, bodyMap);
    return clear(execute(newRequest));
  }

  public ResponseWrapper post(String path, Map<String, ?> bodyMap) {
    OAuthRequest request = this.constructPost(path, bodyMap);
    sign(request);
    return clear(execute(request));
  }

  /**
   * Posts an XAuth request. A new method is needed because the response from
   * the server is not a standard Tumblr JSON response.
   *
   * @param email the user's login email.
   * @param password the user's password.
   * @return the login token.
   */
  public OAuth1AccessToken postXAuth(final String email, final String password) {
    OAuthRequest request = constructXAuthPost(email, password);
    setToken("", ""); // Empty token is required for Scribe to execute XAuth.
    sign(request);
    return clearXAuth(execute(request));
  }

  // Construct an XAuth request
  private OAuthRequest constructXAuthPost(String email, String password) {
    OAuthRequest request = new OAuthRequest(Verb.POST, xauthEndpoint);
    request.addBodyParameter("x_auth_username", email);
    request.addBodyParameter("x_auth_password", password);
    request.addBodyParameter("x_auth_mode", "client_auth");
    return request;
  }

  public ResponseWrapper get(String path, Map<String, ?> map) {
    OAuthRequest request = this.constructGet(path, map);
    sign(request);
    return clear(execute(request));
  }

  private Response execute(OAuthRequest request) {
    try {
      return service.execute(request);
    } catch (InterruptedException | ExecutionException | IOException e) {
      throw new JumblrException(e);
    }
  }

  public OAuthRequest constructGet(String path, Map<String, ?> queryParams) {
    String url = "https://" + hostname + "/v2" + path;
    OAuthRequest request = new OAuthRequest(Verb.GET, url);
    if (queryParams != null) {
      for (Map.Entry<String, ?> entry : queryParams.entrySet()) {
        request.addQuerystringParameter(entry.getKey(), entry.getValue().toString());
      }
    }
    request.addHeader("User-Agent", "jumblr/" + this.version);

    return request;
  }

  private OAuthRequest constructPost(String path, Map<String, ?> bodyMap) {
    String url = "https://" + hostname + "/v2" + path;
    OAuthRequest request = new OAuthRequest(Verb.POST, url);

    for (Map.Entry<String, ?> entry : bodyMap.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      if (value == null || value instanceof File) {
        continue;
      }
      request.addBodyParameter(key, value.toString());
    }
    request.addHeader("User-Agent", "jumblr/" + this.version);

    return request;
  }

  public void setConsumer(String consumerKey, String consumerSecret) {
    service = new ServiceBuilder(consumerKey).apiSecret(consumerSecret)
        //.callback("")
        .build(TumblrApi.instance());
    //service = new ServiceBuilder().
    //    provider(TumblrApi.class).
    //    apiKey(consumerKey).apiSecret(consumerSecret).
    //    build();
  }

  public void setToken(String token, String tokenSecret) {
    this.token = new OAuth1AccessToken(token, tokenSecret);
    //this.token = new Token(token, tokenSecret);
  }

  public void setToken(final OAuth1AccessToken token) {
    this.token = token;
  }

  /* package-visible for testing */ ResponseWrapper clear(Response response) {
    if (response.getCode() == 200 || response.getCode() == 201) {
      try {
        String json = response.getBody();
        Gson gson = new GsonBuilder().
            registerTypeAdapter(JsonElement.class, new JsonElementDeserializer()).
            create();
        ResponseWrapper wrapper = gson.fromJson(json, ResponseWrapper.class);
        if (wrapper == null) {
          throw new JumblrException(response);
        }
        wrapper.setClient(client);
        return wrapper;
      } catch (JsonSyntaxException | IOException ex) {
        throw new JumblrException(ex);
      }
    } else {
      throw new JumblrException(response);
    }
  }

  private OAuth1AccessToken parseXAuthResponse(final Response response) {
    String responseStr = null;
    try {
      responseStr = response.getBody();
    } catch (IOException e) {
      throw new JumblrException(e);
    }
    if (responseStr != null) {
      // Response is received in the format "oauth_token=value&oauth_token_secret=value".
      String extractedToken = null, extractedSecret = null;
      final String[] values = responseStr.split("&");
      for (String value : values) {
        final String[] kvp = value.split("=");
        if (kvp != null && kvp.length == 2) {
          if (kvp[0].equals("oauth_token")) {
            extractedToken = kvp[1];
          } else if (kvp[0].equals("oauth_token_secret")) {
            extractedSecret = kvp[1];
          }
        }
      }
      if (extractedToken != null && extractedSecret != null) {
        return new OAuth1AccessToken(extractedToken, extractedSecret);
      }
    }
    // No good
    throw new JumblrException(response);
  }

  /* package-visible for testing */ OAuth1AccessToken clearXAuth(Response response) {
    if (response.getCode() == 200 || response.getCode() == 201) {
      return parseXAuthResponse(response);
    } else {
      throw new JumblrException(response);
    }
  }

  private void sign(OAuthRequest request) {
    if (token != null) {
      service.signRequest(token, request);
    }
  }

  public static OAuthRequest convertToMultipart(OAuthRequest request, Map<String, ?> bodyMap)
      throws IOException {
    return new MultipartConverter(request, bodyMap).getRequest();
  }

  public String getHostname() {
    return hostname;
  }

  /**
   * Set hostname without protocol
   *
   * @param host such as "api.tumblr.com"
   */
  public void setHostname(String host) {
    this.hostname = host;
  }

  public String getUrlPrefix() {
    return "https://" + hostname + "/v2";
  }
}
