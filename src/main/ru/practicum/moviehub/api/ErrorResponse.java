package ru.practicum.moviehub.api;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ErrorResponse {
    private String error;
    private final List<String> details;

    public ErrorResponse() {
        error = "";
        details = new ArrayList<>();
    }

    public ErrorResponse(String error) {
        this.error = error;
        details = new ArrayList<>();
    }

    public String getError() {
        return error;
    }


    public void setError(String error) {
        this.error = error;
    }

    public List<String> getDetails() {
        return details;
    }

    public void addDetail(String detail) {
        details.add(detail);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ErrorResponse that = (ErrorResponse) o;
        return Objects.equals(error, that.error) && Objects.equals(details, that.details);
    }

    @Override
    public int hashCode() {
        return Objects.hash(error, details);
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}