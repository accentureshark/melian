package org.shark.melian.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.shark.melian.config.MelianProperties;
import org.shark.melian.model.MovieResult;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * IMDB API Client using Spring best practices.
 * Note: Using TMDB API as IMDB API until actual IMDB API is configured.
 */
@Component
@RequiredArgsConstructor
public class IMDBApiClientPure {

    private final MelianProperties melianProperties;
    private final CloseableHttpClient httpClient = HttpClients.createDefault();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public List<MovieResult> searchMovies(String query, int limit) {
        try {
            String apiUrl = melianProperties.getImdb().getApiUrl();
            String accessToken = melianProperties.getImdb().getAccessToken();

            StringBuilder uriBuilder = new StringBuilder(apiUrl).append("/search/movie");
            uriBuilder.append("?query=").append(URLEncoder.encode(query, StandardCharsets.UTF_8));
            
            HttpGet request = new HttpGet(uriBuilder.toString());
            request.setHeader("Authorization", "Bearer " + accessToken);
            request.setHeader("Accept", "application/json");

            String response = httpClient.execute(request, httpResponse -> {
                return EntityUtils.toString(httpResponse.getEntity());
            });

            IMDBResponse imdbResponse = objectMapper.readValue(response, IMDBResponse.class);
            return imdbResponse.getResults().stream()
                    .limit(limit)
                    .map(this::convertToMovieResult)
                    .toList();
        } catch (Exception ex) {
            System.err.println("[ERROR] IMDB API failed: " + ex.getMessage());
            ex.printStackTrace();
            return List.of();
        }
    }

    public IMDBResponse searchMovies(Map<String, String> params) {
        try {
            String apiUrl = melianProperties.getImdb().getApiUrl();
            String accessToken = melianProperties.getImdb().getAccessToken();
            
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

            return objectMapper.readValue(response, IMDBResponse.class);
        } catch (Exception ex) {
            System.err.println("[ERROR] IMDB API failed: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        }
    }

    private MovieResult convertToMovieResult(IMDBMovie movie) {
        return new MovieResult(
                movie.getTitle(),
                movie.getOverview(),
                movie.getReleaseDate(),
                movie.getVoteAverage()
        );
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
    public static class IMDBResponse {
        public List<IMDBMovie> results;
        
        public List<IMDBMovie> getResults() {
            return results != null ? results : List.of();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IMDBMovie {
        public String title;
        public String overview;
        public String release_date;
        public double vote_average;
        
        public String getTitle() {
            return title;
        }
        
        public String getOverview() {
            return overview;
        }
        
        public String getReleaseDate() {
            return release_date;
        }
        
        public double getVoteAverage() {
            return vote_average;
        }
    }
}