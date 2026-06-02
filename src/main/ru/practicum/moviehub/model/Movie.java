package ru.practicum.moviehub.model;

import com.google.gson.Gson;
import ru.practicum.moviehub.api.ErrorResponse;

import java.time.LocalDate;
import java.util.Objects;

public class Movie {
    private int id;
    private final String title;
    private final int year;

    public Movie(String title, int year) {
        this.title = title;
        this.year = year;
        id = hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Movie movie = (Movie) o;
        return year == movie.year && id == movie.id && Objects.equals(title, movie.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, year);
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

    public String getTitle() {
        return title;
    }

    public int getYear() {
        return year;
    }

    public int getId() {
        return id;
    }

    public ErrorResponse validate() {
        ErrorResponse errorResponse = new ErrorResponse();
        if (title.isBlank()) {
            errorResponse.addDetail("Название не должно быть пустым");
        }
        if (title.length() > 100) {
            errorResponse.addDetail("Название слишком длинное, максимальная длина 100 символов");
        }
        if (year < 1888 || year > LocalDate.now().getYear() + 1) {
            errorResponse.addDetail("Год должен быть между " + 1888 + " и " + (LocalDate.now().getYear() + 1));
        }
        if (!errorResponse.getDetails().isEmpty()) {
            errorResponse.setError("Ошибка валидации");
        }
        return errorResponse;
    }


}