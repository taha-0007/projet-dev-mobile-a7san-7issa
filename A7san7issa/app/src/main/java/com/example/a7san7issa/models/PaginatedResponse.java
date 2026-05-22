package com.example.a7san7issa.models;

import java.util.List;

public class PaginatedResponse<T> {
    private int count;
    private String next;
    private String previous;
    private List<T> results;

    public int getCount() { return count; }
    public String getNext() { return next; }
    public String getPrevious() { return previous; }
    public List<T> getResults() { return results; }
}