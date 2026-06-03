package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MoviesApiTest {
    private static final String BASE = "http://localhost:8080";
    private static final String CT_JSON = "application/json; charset=UTF-8";
    private static MoviesServer server;
    private static HttpClient client;
    private static MoviesStore moviesStore;

    @BeforeAll
    static void beforeAll() {
        moviesStore = new MoviesStore();
        server = new MoviesServer(moviesStore, 8080);
        server.start();
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @BeforeEach
    void beforeEach() {
        moviesStore.deleteAll();
    }

    @AfterAll
    static void afterAll() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void getMovies_whenEmpty_returnEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .setHeader("Content-Type", CT_JSON)
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");
    }

    @Test
    void getMovies_whenNotEmpty_returnArrayOfMovies() throws Exception {
        Movie m1 = new Movie("Вот это драма", 2026);
        Movie m2 = new Movie("Substance", 2024);
        moviesStore.addMovie(m1);
        moviesStore.addMovie(m2);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .setHeader("Content-Type", CT_JSON)
                .GET()
                .build();
        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body();
        assertEquals(new Gson().toJson(List.of(m1, m2)), body,
                "Ожидается JSON-массив");
    }

    @Test
    void postMovies_whenDataIsCorrect() throws Exception {
        Movie m1 = new Movie("Вот это драма", 2026);
        Gson gson = new Gson();
        String movie = gson.toJson(m1);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .setHeader("Content-Type", CT_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(movie, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(201, resp.statusCode(), "POST /movies должен вернуть 201");
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body();
        assertEquals(movie, body);
    }

    @Test
    void postMovies_whenTitleIsEmpty_returnError() throws Exception {
        Movie m1 = new Movie("", 2026);
        Gson gson = new Gson();
        String movie = gson.toJson(m1);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .setHeader("Content-Type", CT_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(movie, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        ErrorResponse expected = new ErrorResponse("Ошибка валидации");
        expected.addDetail("Название не должно быть пустым");
        String body = resp.body();
        assertEquals(expected.toString(), body);
    }

    @Test
    void postMovies_whenTitleIsTooLong_returnError() throws Exception {
        Movie m1 = new Movie("1".repeat(101), 2026);
        Gson gson = new Gson();
        String movie = gson.toJson(m1);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .setHeader("Content-Type", CT_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(movie, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        ErrorResponse expected = new ErrorResponse("Ошибка валидации");
        expected.addDetail("Название слишком длинное, максимальная длина 100 символов");
        String body = resp.body();
        assertEquals(expected.toString(), body);
    }

    @Test
    void postMovies_whenYearIsIncorrect_returnError() throws Exception {
        Movie m1 = new Movie("Unknown", 1800);
        Gson gson = new Gson();
        String movie = gson.toJson(m1);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .setHeader("Content-Type", CT_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(movie, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        ErrorResponse expected = new ErrorResponse("Ошибка валидации");
        expected.addDetail("Год должен быть между " + 1888 + " и " + (LocalDate.now().getYear() + 1));
        String body = resp.body();
        assertEquals(expected.toString(), body);
    }

    @Test
    void postMovies_whenContentTypeIsIncorrect_returnError() throws Exception {
        Movie m1 = new Movie("Вот это драма", 2026);
        Gson gson = new Gson();
        String movie = gson.toJson(m1);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .setHeader("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(movie, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(415, resp.statusCode(), "POST /movies должен вернуть 415");
    }

    @Test
    void postMovies_whenJsonIsIncorrect_returnError() throws Exception {
        String str = "{\"movie\":\"Вот это драма\",\"year\":2026}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .setHeader("Content-Type", CT_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(str, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(400, resp.statusCode(), "POST /movies должен вернуть 400");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        ErrorResponse expected = new ErrorResponse("Некорректный JSON");
        String body = resp.body();
        assertEquals(expected.toString(), body);
    }

    @Test
    void getMoviesId_whenIdExists_returnMovie() throws Exception {
        Movie m1 = new Movie("Вот это драма", 2026);
        moviesStore.addMovie(m1);
        Gson gson = new Gson();
        int id = m1.getId();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + id))
                .setHeader("Content-Type", CT_JSON)
                .GET()
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, resp.statusCode(), "GET /movies/{id} должен вернуть 200");
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body();
        assertEquals(gson.toJson(m1), body);
    }

    @Test
    void getMoviesId_whenIdNotExist_returnError() throws Exception {
        Movie m1 = new Movie("Вот это драма", 2026);
        moviesStore.addMovie(m1);
        int id = 0;
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + id))
                .setHeader("Content-Type", CT_JSON)
                .GET()
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(404, resp.statusCode(), "GET /movies/{id} должен вернуть 404");
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body();
        ErrorResponse expected = new ErrorResponse("Фильм не найден");
        assertEquals(expected.toString(), body);
    }

    @Test
    void getMoviesId_whenIdNotNumber_returnError() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/id"))
                .setHeader("Content-Type", CT_JSON)
                .GET()
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(400, resp.statusCode(), "GET /movies/{id} должен вернуть 400");
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body();
        ErrorResponse expected = new ErrorResponse("Некорректный ID");
        assertEquals(expected.toString(), body);
    }

    @Test
    void deleteMoviesId_whenMovieExists() throws Exception {
        Movie m1 = new Movie("Вот это драма", 2026);
        moviesStore.addMovie(m1);
        int id = m1.getId();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + id))
                .setHeader("Content-Type", CT_JSON)
                .DELETE()
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(204, resp.statusCode(), "DELETE /movies/{id} должен вернуть 204");
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        assertTrue(moviesStore.getStore().isEmpty());
    }

    @Test
    void deleteMoviesId_whenMovieNotExist_returnError() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/0"))
                .setHeader("Content-Type", CT_JSON)
                .DELETE()
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(404, resp.statusCode(), "DELETE /movies/{id} должен вернуть 404");
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body();
        ErrorResponse expected = new ErrorResponse("Фильм не найден");
        assertEquals(expected.toString(), body);
    }

    @Test
    void deleteMoviesId_whenIdNotNumber_returnError() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/id"))
                .setHeader("Content-Type", CT_JSON)
                .DELETE()
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(400, resp.statusCode(), "DELETE /movies/{id} должен вернуть 400");
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body();
        ErrorResponse expected = new ErrorResponse("Некорректный ID");
        assertEquals(expected.toString(), body);
    }

    @Test
    void getMoviesYear_whenMoviesExist_returnArrayOfMovies() throws Exception {
        Movie m1 = new Movie("Вот это драма", 2026);
        Movie m2 = new Movie("Дьявол носит Прада 2", 2026);
        Movie m3 = new Movie("Субстанция", 2024);
        moviesStore.addMovie(m1);
        moviesStore.addMovie(m2);
        moviesStore.addMovie(m3);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2026"))
                .setHeader("Content-Type", CT_JSON)
                .GET()
                .build();
        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, resp.statusCode(), "GET /movies?year=YYYY должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        String body = resp.body();
        assertEquals(new Gson().toJson(List.of(m1, m2)), body);
    }

    @Test
    void getMoviesYear_whenMoviesNotExist_returnEmptyArray() throws Exception {
        Movie m1 = new Movie("Вот это драма", 2026);
        Movie m2 = new Movie("Дьявол носит Прада 2", 2026);
        Movie m3 = new Movie("Субстанция", 2024);
        moviesStore.addMovie(m1);
        moviesStore.addMovie(m2);
        moviesStore.addMovie(m3);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2025"))
                .setHeader("Content-Type", CT_JSON)
                .GET()
                .build();
        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, resp.statusCode(), "GET /movies?year=YYYY должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");
    }

    @Test
    void getMoviesYear_whenYearIsIncorrect_returnError() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=year"))
                .setHeader("Content-Type", CT_JSON)
                .GET()
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(400, resp.statusCode(), "GET /movies?year=YYYY должен вернуть 400");
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body();
        ErrorResponse expected = new ErrorResponse("Некорректный параметр запроса year");
        assertEquals(expected.toString(), body);
    }

    @Test
    void unsupportedMethod_returnError() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .setHeader("Content-Type", CT_JSON)
                .PUT(HttpRequest.BodyPublishers.ofString(""))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(405, resp.statusCode());
    }
}