package org.example.inventoryapi.dto;

import java.util.List;

public record PageResponse<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int currentPage,
        int size
) {
    public static <T> PageResponse<T> of(List<T> all, int page, int size) {
        int total = all.size();
        int pages = total == 0 ? 0 : (int) Math.ceil((double) total / size);
        int from  = Math.min(page * size, total);
        int to    = Math.min(from + size, total);
        return new PageResponse<>(all.subList(from, to), total, pages, page, size);
    }
}
