package com.beezhub.student_accommodation.model.dto;

import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
public class PageDto<T> {

    private List<T> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean last;
    private boolean first;

    public static <S, T> PageDto<T> fromPage(Page<S> page, Function<? super S, ? extends T> mapper) {
        PageDto<T> dto = new PageDto<>();
        dto.setContent(page.getContent().stream().map(mapper).collect(Collectors.toList()));
        dto.setPageNumber(page.getNumber());
        dto.setPageSize(page.getSize());
        dto.setTotalElements(page.getTotalElements());
        dto.setTotalPages(page.getTotalPages());
        dto.setLast(page.isLast());
        dto.setFirst(page.isFirst());
        return dto;
    }
}
