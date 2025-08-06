package org.shark.melian.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Component
@RequiredArgsConstructor
@Slf4j
public class TMDBApiClientPure {

    private final MelianProperties melianProperties;
    private final CloseableHttpClient httpClient = HttpClients.createDefault();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<MovieResult> searchMovies(String query, int limit) {
        try {
            String apiUrl = melianProperties.getTmdb().getApiUrl();
            String accessToken = melianProperties.getTmdb().getAccessToken();

            StringBuilder uriBuilder = new StringBuilder(apiUrl).append("/search/movie");
            uriBuilder.append("?query=").append(URLEncoder.encode(query, StandardCharsets.UTF_8));

            // Puedes agregar el parámetro language si lo necesitas:
            // uriBuilder.append("&language=en");

            String url = uriBuilder.toString();
            log.debug("[TMDBApiClientPure] Request URL: {}", url);

            HttpGet request = new HttpGet(url);
            request.setHeader("Authorization", "Bearer " + accessToken);
            request.setHeader("Accept", "application/json");

            log.debug("[TMDBApiClientPure] Headers: Authorization=Bearer ****, Accept=application/json");

            String response = httpClient.execute(request, httpResponse -> {
                return EntityUtils.toString(httpResponse.getEntity());
            });

            log.debug("[TMDBApiClientPure] Response: {}", response);

            TMDBResponse tmdbResponse = objectMapper.readValue(response, TMDBResponse.class);
            return tmdbResponse.getResults().stream()
                    .limit(limit)
                    .map(this::convertToMovieResult)
                    .toList();
        } catch (Exception ex) {
            log.error("[TMDBApiClientPure] TMDB API failed: {}", ex.getMessage(), ex);
            return List.of();
        }
    }

    public TMDBResponse searchMovies(Map<String, String> params) {
        try {
            String apiUrl = melianProperties.getTmdb().getApiUrl();
            String accessToken = melianProperties.getTmdb().getAccessToken();

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

            String url = uriBuilder.toString();
            log.debug("[TMDBApiClientPure] Request URL: {}", url);

            HttpGet request = new HttpGet(url);
            request.setHeader("Authorization", "Bearer " + accessToken);
            request.setHeader("Accept", "application/json");

            log.debug("[TMDBApiClientPure] Headers: Authorization=Bearer ****, Accept=application/json");

            String response = httpClient.execute(request, httpResponse -> {
                return EntityUtils.toString(httpResponse.getEntity());
            });

            log.debug("[TMDBApiClientPure] Response: {}", response);

            return objectMapper.readValue(response, TMDBResponse.class);
        } catch (Exception ex) {
            log.error("[TMDBApiClientPure] TMDB API failed: {}", ex.getMessage(), ex);
            return null;
        }
    }

    private MovieResult convertToMovieResult(TMDBMovie movie) {
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
            log.error("[TMDBApiClientPure] Error closing HTTP client: {}", e.getMessage(), e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TMDBResponse {
        public List<TMDBMovie> results;

        public List<TMDBMovie> getResults() {
            return results != null ? results : List.of();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TMDBMovie {
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