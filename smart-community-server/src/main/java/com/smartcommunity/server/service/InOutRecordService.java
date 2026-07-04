package com.smartcommunity.server.service;

import com.smartcommunity.server.common.PageResult;
import com.smartcommunity.server.common.PageQuery;
import com.smartcommunity.server.entity.InOutRecord;

public interface InOutRecordService {

    PageResult<InOutRecord> pageList(PageQuery pageQuery, String personName, Long communityId, Integer type, String startTime, String endTime);
}
