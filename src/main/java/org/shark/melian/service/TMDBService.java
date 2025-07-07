package org.shark.melian.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.shark.melian.model.MovieResult;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class TMDBService {
	private static final String TMDB_API = "https://api.themoviedb.org/3";
	private static final String TMDB_ACCESS_TOKEN =
			"Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJkYzEzOThhZDdiMTY4ZjM2ZGJkMGIzYTZmYTYzOThhYSIsIm5iZiI6MTc0OTkzNDEyNi4xNDgsInN1YiI6IjY4NGRlMDJlYzgzZWJlNzgxOWJiNGU1YyIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.oI0cWBV3BQmfGNqjh27YLAqNuZK2gIQW-wkYeamNv5Y";

	private final RestTemplate restTemplate;

	public TMDBService() {
		this.restTemplate = new RestTemplate();
		this.restTemplate.getInterceptors().add(authHeaderInterceptor());
	}

	public List<MovieResult> search(String title, int limit) {
		String query = URLEncoder.encode(title, StandardCharsets.UTF_8);
		String url = TMDB_API + "/search/movie?query=" + query;

		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", TMDB_ACCESS_TOKEN);
		headers.setAccept(List.of(MediaType.APPLICATION_JSON));
		HttpEntity<String> entity = new HttpEntity<>(headers);

		ResponseEntity<TMDBResponse> response = restTemplate.exchange(
				url, HttpMethod.GET, entity, TMDBResponse.class
		);

		List<MovieResult> results = new ArrayList<>();
		if (response.getBody() != null && response.getBody().results != null) {
			for (TMDBMovie movie : response.getBody().results) {
				results.add(new MovieResult(
						movie.title,
						movie.overview,
						movie.release_date,
						movie.vote_average
				));
				if (results.size() >= limit) break;
			}
		}

		return results;
	}

	private ClientHttpRequestInterceptor authHeaderInterceptor() {
		return (request, body, execution) -> {
			request.getHeaders().setBearerAuth(TMDB_ACCESS_TOKEN.replace("Bearer ", ""));
			return execution.execute(request, body);
		};
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
