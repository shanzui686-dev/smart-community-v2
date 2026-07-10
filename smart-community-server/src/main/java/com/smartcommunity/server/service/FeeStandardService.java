package com.smartcommunity.server.service;

import com.smartcommunity.server.common.PageQuery;
import com.smartcommunity.server.common.PageResult;
import com.smartcommunity.server.entity.FeeStandard;
import java.util.List;

public interface FeeStandardService {
    PageResult<FeeStandard> pageList(PageQuery pageQuery, String keyword, Integer status);
    FeeStandard getById(Long id);
    void add(FeeStandard feeStandard);
    void update(FeeStandard feeStandard);
    void delete(Long id);
    List<FeeStandard> listEnabled();
}
