package org.shark.melian.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
public class TMDBApiClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TMDBApiClient(
            @Value("${tmdb.api-url:https://api.themoviedb.org/3}") String apiUrl,
            @Value("${tmdb.access-token}") String accessToken
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public TMDBResponse searchMovies(Map<String, String> params) {
        try {
            StringBuilder uriBuilder = new StringBuilder("/search/movie");
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
            String rawJson = restClient.get()
                    .uri(uriBuilder.toString())
                    .retrieve()
                    .body(String.class);

            return objectMapper.readValue(rawJson, TMDBResponse.class);
        } catch (Exception ex) {
            System.err.println("[ERROR] TMDB API failed: " + ex.getMessage());
            ex.printStackTrace();
            return null;
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
