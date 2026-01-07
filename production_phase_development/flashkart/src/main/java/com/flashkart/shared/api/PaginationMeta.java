package com.flashkart.shared.api;

public class PaginationMeta {
    public int page;
    public int size;
    public long total;

    public PaginationMeta(int page, int size, long total) {
        this.page = page;
        this.size = size;
        this.total = total;
    }
}
