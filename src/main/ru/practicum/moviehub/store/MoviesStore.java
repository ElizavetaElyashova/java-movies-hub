package ru.practicum.moviehub.store;

import com.google.gson.Gson;
import ru.practicum.moviehub.model.Movie;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class MoviesStore {
    private final HashMap<Integer, Movie> store;

    public MoviesStore() {
        store = new HashMap<>();
    }

    public void addMovie(Movie movie) {
        store.put(movie.getId(), movie);
    }

    public Optional<Movie> searchMovie(Integer id) {
        return Optional.ofNullable(store.getOrDefault(id, null));
    }

    public boolean deleteMovie(Integer id) {
        return store.remove(id) != null;
    }

    public void deleteAll() {
        store.clear();
    }

    public List<Movie> filterByYear(int year) {
        return store.values().stream()
                .filter(movie -> movie.getYear() == year)
                .toList();
    }

    public HashMap<Integer, Movie> getStore() {
        return store;
    }

    @Override
    public String toString() {
        return new Gson().toJson(store.values());
    }
}