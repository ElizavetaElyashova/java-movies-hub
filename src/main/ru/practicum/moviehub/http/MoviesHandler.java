package ru.practicum.moviehub.http;

import com.google.gson.*;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

public class MoviesHandler extends BaseHttpHandler {
    private final MoviesStore moviesStore;

    public MoviesHandler(MoviesStore moviesStore) {
        this.moviesStore = moviesStore;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        Endpoint endpoint = getEndpoint(ex.getRequestURI().getPath(), ex.getRequestMethod(), Optional.ofNullable(ex.getRequestURI().getQuery()));

        switch (endpoint) {
            case GET_MOVIES -> handleGetMovies(ex);
            case POST_MOVIES -> handlePostMovies(ex);
            case DELETE_MOVIES -> handleDeleteMovies(ex);
            case GET_MOVIES_ID -> handleGetMovieById(ex);
            case GET_MOVIES_YEAR -> handleGetMoviesByYear(ex);
            case UNKNOWN -> sendWithoutBody(ex, 405);
        }
    }

    public void handleGetMovies(HttpExchange ex) throws IOException {
        sendJson(ex, 200, moviesStore.toString());
    }

    public void handlePostMovies(HttpExchange ex) throws IOException {
        try (InputStream is = ex.getRequestBody()) {
            if (!(ex.getRequestHeaders().get("Content-Type").contains(CT_JSON))) {
                sendWithoutBody(ex, 415);
                return;
            }
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            Gson gson = new Gson();
            JsonElement jsonElement = JsonParser.parseString(body);
            if (!jsonElement.isJsonObject()) {
                ErrorResponse errorResponse = new ErrorResponse("Некорректный JSON");
                sendJson(ex, 400, gson.toJson(errorResponse));
                return;
            }
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            if (!(jsonObject.has("title") && jsonObject.has("year"))) {
                ErrorResponse errorResponse = new ErrorResponse("Некорректный JSON");
                sendJson(ex, 400, gson.toJson(errorResponse));
                return;
            }
            String title = jsonObject.get("title").getAsString();
            int year = jsonObject.get("year").getAsInt();
            Movie movie = new Movie(title, year);
            ErrorResponse errorResponse = movie.validate();
            if (errorResponse.getError().isBlank()) {
                moviesStore.addMovie(movie);
                sendJson(ex, 201, movie.toString());
            } else {
                sendJson(ex, 422, gson.toJson(errorResponse));
            }
        } catch (JsonParseException e) {
            ErrorResponse errorResponse = new ErrorResponse("Некорректный JSON");
            sendJson(ex, 400, new Gson().toJson(errorResponse));
        }
    }

    public void handleDeleteMovies(HttpExchange ex) throws IOException {
        String[] pathParts = ex.getRequestURI().getPath().split("/");
        String id = pathParts[2];
        if (isInteger(id)) {
            if (moviesStore.deleteMovie(Integer.parseInt(id))) {
                sendWithoutBody(ex, 204);
            } else {
                ErrorResponse errorResponse = new ErrorResponse("Фильм не найден");
                sendJson(ex, 404, errorResponse.toString());
            }
        } else {
            ErrorResponse errorResponse = new ErrorResponse("Некорректный ID");
            sendJson(ex, 400, errorResponse.toString());
        }
    }

    public void handleGetMovieById(HttpExchange ex) throws IOException {
        String[] pathParts = ex.getRequestURI().getPath().split("/");
        String id = pathParts[2];
        if (isInteger(id)) {
            Optional<Movie> target = moviesStore.searchMovie(Integer.parseInt(id));
            if (target.isEmpty()) {
                ErrorResponse errorResponse = new ErrorResponse("Фильм не найден");
                sendJson(ex, 404, errorResponse.toString());
            } else {
                sendJson(ex, 200, target.get().toString());
            }
        } else {
            ErrorResponse errorResponse = new ErrorResponse("Некорректный ID");
            sendJson(ex, 400, errorResponse.toString());
        }
    }

    public void handleGetMoviesByYear(HttpExchange ex) throws IOException {
        String year = ex.getRequestURI().getQuery().substring(5);
        if (isInteger(year)) {
            List<Movie> moviesByYear = moviesStore.filterByYear(Integer.parseInt(year));
            sendJson(ex, 200, new Gson().toJson(moviesByYear));
        } else {
            ErrorResponse errorResponse = new ErrorResponse("Некорректный параметр запроса year");
            sendJson(ex, 400, errorResponse.toString());
        }
    }

    public Endpoint getEndpoint(String requestPath, String requestMethod, Optional<String> query) {
        String[] pathParts = requestPath.split("/");
        if (!pathParts[1].equals("movies")) {
            return Endpoint.UNKNOWN;
        }
        if (requestMethod.equalsIgnoreCase("POST")) {
            return Endpoint.POST_MOVIES;
        }
        if (requestMethod.equalsIgnoreCase("DELETE") && pathParts.length == 3) {
            return Endpoint.DELETE_MOVIES;
        }
        if (requestMethod.equalsIgnoreCase("GET")) {
            if (query.isPresent()) {
                if (query.get().substring(0, 4).equals("year")) {
                    return Endpoint.GET_MOVIES_YEAR;
                }
            }
            if (pathParts.length == 3) {
                return Endpoint.GET_MOVIES_ID;
            }
            if (pathParts[1].length() == 6) {
                return Endpoint.GET_MOVIES;
            }
        }
        return Endpoint.UNKNOWN;
    }

    public boolean isInteger(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

}
