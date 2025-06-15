package org.shark.melian.service;

import org.shark.melian.model.ChunkDto;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service("restApiChunkService")
public class RestApiChunkService implements ChunkService {

    // Token de acceso para TMDB
    private static final String TMDB_API = "https://api.themoviedb.org/3";
    private static final String TMDB_ACCESS_TOKEN =
            "eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJkYzEzOThhZDdiMTY4ZjM2ZGJkMGIzYTZmYTYzOThhYSIsIm5iZiI6MTc0OTkzNDEyNi4xNDgsInN1YiI6IjY4NGRlMDJlYzgzZWJlNzgxOWJiNGU1YyIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.oI0cWBV3BQmfGNqjh27YLAqNuZK2gIQW-wkYeamNv5Y";

    private static String encode(String s) {
        return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
    }

    @Override
    public List<ChunkDto> getChunks(
            String table, String source, int limit, String afterId,
            String filter, List<String> tags, String sort
    ) {

        try {
            filter = java.net.URLDecoder.decode(filter, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            // log o ignorar
        }

        if (!"film".equalsIgnoreCase(table))
            throw new IllegalArgumentException("Only film table supported for TMDB");

        // Parsear filtro para obtener título (simple, puedes mejorar)
        String titleQuery = extractTitleFromFilter(filter);

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add(getBearerAuthInterceptor());

        String url = TMDB_API + "/search/movie?query=" + encode(titleQuery);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + TMDB_ACCESS_TOKEN);
        headers.set("accept", "application/json");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<TMDBResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, TMDBResponse.class
        );

        List<ChunkDto> chunks = new ArrayList<>();
        if (response.getBody() != null && response.getBody().results != null) {
            for (TMDBMovie movie : response.getBody().results) {
                ChunkDto chunk = new ChunkDto();
                chunk.setId(String.valueOf(movie.id));
                chunk.setText(movie.title + " | " + movie.overview);
                chunk.setMetadata(Map.of(
                        "release_date", movie.release_date,
                        "rating", movie.vote_average
                ));
                chunks.add(chunk);
                if (chunks.size() >= limit) break;
            }
        }
        return chunks;
    }

    // Extrae el valor del título del filtro SQL (e.g., title='Thor' o title LIKE '%Thor%')
    private String extractTitleFromFilter(String filter) {
        if (filter == null) return "";
        // Busca patrones de igualdad o LIKE
        String title = filter.replaceAll("(?i)title\\s*=\\s*'([^']+)'", "$1")
                .replaceAll("(?i)title\\s+LIKE\\s+'%?([^'%]+)%?'", "$1");
        // Fallback por si quedó igual
        if (title.equals(filter)) {
            // Intenta extraer sin quotes
            int eq = filter.indexOf('=');
            if (eq >= 0) title = filter.substring(eq + 1).replaceAll("'", "").trim();
        }
        return title.trim();
    }

    private ClientHttpRequestInterceptor getBearerAuthInterceptor() {
        return (request, body, execution) -> {
            request.getHeaders().setBearerAuth(TMDB_ACCESS_TOKEN);
            request.getHeaders().setAccept(List.of(MediaType.APPLICATION_JSON));
            return execution.execute(request, body);
        };
    }

    // DTO internos
    static class TMDBResponse {
        public List<TMDBMovie> results;
    }

    static class TMDBMovie {
        public int id;
        public String title;
        public String overview;
        public String release_date;
        public double vote_average;
    }
}
