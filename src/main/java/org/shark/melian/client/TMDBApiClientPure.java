package org.shark.melian.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.shark.melian.config.MelianConfig;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Pure Java TMDB API Client without Spring dependencies.
 * Uses Apache HttpClient for HTTP requests.
 */
public class TMDBApiClientPure {

    private final String apiUrl;
    private final String accessToken;
    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public TMDBApiClientPure(MelianConfig config) {
        this.apiUrl = config.getProperty("tmdb.api-url");
        this.accessToken = config.getProperty("tmdb.access-token");
        this.httpClient = HttpClients.createDefault();
        this.objectMapper = new ObjectMapper();
    }

    public TMDBResponse searchMovies(Map<String, String> params) {
        try {
            StringBuilder uriBuilder = new StringBuilder(apiUrl).append("/search/movie");
            if (params != null && !params.isEmpty()) {
                uriBuilder.append('?');
                for (var entry : params.entrySet()) {
                    uriBuilder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                            .append('=')
                            .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                            .append('&');
                }
                uriBuilder.deleteCharAt(uriBuilder.length() - 1);
            }

            HttpGet request = new HttpGet(uriBuilder.toString());
            request.setHeader("Authorization", "Bearer " + accessToken);
            request.setHeader("Accept", "application/json");

            String response = httpClient.execute(request, httpResponse -> {
                return EntityUtils.toString(httpResponse.getEntity());
            });

            return objectMapper.readValue(response, TMDBResponse.class);
        } catch (Exception ex) {
            System.err.println("[ERROR] TMDB API failed: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        }
    }

    public void close() {
        try {
            if (httpClient != null) {
                httpClient.close();
            }
        } catch (Exception e) {
            System.err.println("Error closing HTTP client: " + e.getMessage());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TMDBResponse {
        public List<TMDBMovie> results;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TMDBMovie {
        public String title;
        public String overview;
        public String release_date;
        public double vote_average;
    }
}