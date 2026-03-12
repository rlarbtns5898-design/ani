package com.example.demo.user.dto;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class PaginationDTO {

    private boolean has_next_page;

    public boolean isHasNextPage() {
        return has_next_page;
    }
}
