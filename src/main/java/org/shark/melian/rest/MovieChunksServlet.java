package org.shark.melian.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.shark.melian.service.MovieChunkService;
import org.shark.melian.model.ChunkDto;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Servlet that exposes movie chunks from SQL or Mongo storage as JSON.
 */
public class MovieChunksServlet extends HttpServlet {

    private final MovieChunkService sqlService;
    private final MovieChunkService mongoService;
    private final ObjectMapper mapper = new ObjectMapper();

    public MovieChunksServlet(MovieChunkService sqlService, MovieChunkService mongoService) {
        this.sqlService = sqlService;
        this.mongoService = mongoService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String source = req.getParameter("source");
        if (source == null || source.isBlank()) {
            source = "sql";
        }
        int limit = 10;
        try {
            if (req.getParameter("limit") != null) {
                limit = Integer.parseInt(req.getParameter("limit"));
            }
        } catch (NumberFormatException ignored) { }
        String filter = req.getParameter("filter");
        MovieChunkService service = "mongo".equalsIgnoreCase(source) ? mongoService : sqlService;
        List<ChunkDto> chunks = service.getMovieChunks(source, limit, null, filter, null, null);

        resp.setContentType("application/json");
        mapper.writeValue(resp.getWriter(), chunks);
    }
}
