package org.shark.melian.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.shark.melian.service.TMDBServicePure;
import org.shark.melian.model.MovieResult;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Simple servlet to search movies via TMDB API and return JSON results.
 */
public class SearchMoviesServlet extends HttpServlet {

    private final TMDBServicePure tmdbService;
    private final ObjectMapper mapper = new ObjectMapper();

    public SearchMoviesServlet(TMDBServicePure tmdbService) {
        this.tmdbService = tmdbService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String query = req.getParameter("query");
        if (query == null || query.isBlank()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Missing query parameter");
            return;
        }
        int limit = 10;
        try {
            if (req.getParameter("limit") != null) {
                limit = Integer.parseInt(req.getParameter("limit"));
            }
        } catch (NumberFormatException ignored) { }

        List<MovieResult> results = tmdbService.search(query, limit);
        resp.setContentType("application/json");
        mapper.writeValue(resp.getWriter(), results);
    }
}
