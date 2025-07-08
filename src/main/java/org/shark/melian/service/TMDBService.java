package org.shark.melian.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.shark.melian.model.MovieResult;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class TMDBService {
	private static final String TMDB_API = "https://api.themoviedb.org/3";
	private static final String TMDB_ACCESS_TOKEN =
			"Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJkYzEzOThhZDdiMTY4ZjM2ZGJkMGIzYTZmYTYzOThhYSIsIm5iZiI6MTc0OTkzNDEyNi4xNDgsInN1YiI6IjY4NGRlMDJlYzgzZWJlNzgxOWJiNGU1YyIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.oI0cWBV3BQmfGNqjh27YLAqNuZK2gIQW-wkYeamNv5Y";

	private final RestClient restClient;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public TMDBService() {
		this.restClient = RestClient.builder()
				.baseUrl(TMDB_API)
				.defaultHeader(HttpHeaders.AUTHORIZATION, TMDB_ACCESS_TOKEN)
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
		System.err.println("[DEBUG] Searching for title: " + title);
		try {
			String query = URLEncoder.encode(title, StandardCharsets.UTF_8);
			String uri = "/search/movie?query=" + query;

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
