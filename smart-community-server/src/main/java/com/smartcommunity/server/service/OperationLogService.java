package com.smartcommunity.server.service;

import com.smartcommunity.server.common.PageResult;
import com.smartcommunity.server.common.PageQuery;
import com.smartcommunity.server.entity.OperationLog;
import com.smartcommunity.server.vo.StatisticsVO;

public interface OperationLogService {

    PageResult<OperationLog> pageList(PageQuery pageQuery, String keyword);

    StatisticsVO getStatistics();
}
