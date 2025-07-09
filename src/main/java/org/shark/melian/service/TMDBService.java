package org.shark.melian.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.shark.melian.model.MovieResult;
import org.springframework.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class TMDBService {
        private final RestClient restClient;
        private final ObjectMapper objectMapper = new ObjectMapper();

        public TMDBService(
                        @Value("${tmdb.api-url:https://api.themoviedb.org/3}") String apiUrl,
                        @Value("${tmdb.access-token}") String accessToken
        ) {
                this.restClient = RestClient.builder()
                                .baseUrl(apiUrl)
                                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                .requestInterceptor((request, body, execution) -> {
                                        System.err.println("[DEBUG] Request URI: " + request.getURI());
                                        System.err.println("[DEBUG] Request Headers: " + request.getHeaders());
                                        return execution.execute(request, body);
                                })
                                .build();

                System.err.println("[DEBUG] TMDBService initialized");
        }

        public List<MovieResult> search(String title, int limit) {
                return searchByParams(Map.of("query", title), limit);
        }

        public List<MovieResult> searchByParams(Map<String, String> params, int limit) {
                System.err.println("[DEBUG] Searching with params: " + params);
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
                        String uri = uriBuilder.toString();

			String rawJson = restClient.get()
					.uri(uri)
					.retrieve()
					.body(String.class);

			System.err.println("[DEBUG] Raw JSON response: " + rawJson);

			TMDBResponse response = objectMapper.readValue(rawJson, TMDBResponse.class);

			if (response == null || response.results == null || response.results.isEmpty()) {
				System.err.println("[DEBUG] No movie results found.");
				return List.of();
			}

			System.err.println("[DEBUG] Found " + response.results.size() + " movie(s).");

			List<MovieResult> results = new ArrayList<>();
			for (TMDBMovie movie : response.results) {
				System.err.printf("[DEBUG] Movie: %s (%s) | Rating: %.2f%n",
						movie.title, movie.release_date, movie.vote_average);

				results.add(new MovieResult(
						movie.title,
						movie.overview,
						movie.release_date,
						movie.vote_average
				));

				if (results.size() >= limit) break;
			}

			return results;

		} catch (Exception ex) {
			System.err.println("[ERROR] TMDB API failed: " + ex.getMessage());
			ex.printStackTrace();
			return List.of();
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	static class TMDBResponse {
		public List<TMDBMovie> results;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	static class TMDBMovie {
		public String title;
		public String overview;
		public String release_date;
		public double vote_average;
	}
}
