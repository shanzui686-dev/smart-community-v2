package com.smartcommunity.server.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageQuery {
    private Long pageNum = 1L;
    private Long pageSize = 10L;
    private String orderBy;
    private String orderDirection;
}
