package com.smartcommunity.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.server.common.PageResult;
import com.smartcommunity.server.common.PageQuery;
import com.smartcommunity.server.entity.Community;
import com.smartcommunity.server.entity.InOutRecord;
import com.smartcommunity.server.mapper.CommunityMapper;
import com.smartcommunity.server.mapper.InOutRecordMapper;
import com.smartcommunity.server.service.InOutRecordService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InOutRecordServiceImpl implements InOutRecordService {

    private final InOutRecordMapper recordMapper;
    private final CommunityMapper communityMapper;

    public InOutRecordServiceImpl(InOutRecordMapper recordMapper, CommunityMapper communityMapper) {
        this.recordMapper = recordMapper;
        this.communityMapper = communityMapper;
    }

    @Override
    public PageResult<InOutRecord> pageList(PageQuery pageQuery, String personName, Long communityId,
                                            Integer type, String startTime, String endTime) {
        // 查询进入记录（type=1）
        Page<InOutRecord> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        LambdaQueryWrapper<InOutRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InOutRecord::getType, 1);

        if (personName != null && !personName.isBlank()) {
            wrapper.like(InOutRecord::getPersonName, personName);
        }
        if (communityId != null) {
            wrapper.eq(InOutRecord::getCommunityId, communityId);
        }
        if (startTime != null && !startTime.isBlank()) {
            wrapper.ge(InOutRecord::getTime, startTime);
        }
        if (endTime != null && !endTime.isBlank()) {
            wrapper.le(InOutRecord::getTime, endTime);
        }
        wrapper.orderByDesc(InOutRecord::getTime);

        Page<InOutRecord> result = recordMapper.selectPage(page, wrapper);

        // 为每条进入记录查找对应的外出记录，并填充社区名和门牌号
        for (InOutRecord record : result.getRecords()) {
            // 填充社区名称
            if (record.getCommunityId() != null) {
                Community c = communityMapper.selectById(record.getCommunityId());
                if (c != null) record.setCommunityName(c.getName());
            }
            // 门牌号从 location 获取（checkIn 时已写入）
            record.setHouseNo(record.getLocation());

            // 查找外出记录
            LocalDate entryDate = record.getTime().toLocalDate();
            LambdaQueryWrapper<InOutRecord> outWrapper = new LambdaQueryWrapper<>();
            outWrapper.eq(InOutRecord::getPersonName, record.getPersonName())
                      .eq(InOutRecord::getCommunityId, record.getCommunityId())
                      .eq(InOutRecord::getType, 2)
                      .ge(InOutRecord::getTime, record.getTime())
                      .lt(InOutRecord::getTime, entryDate.plusDays(1).atStartOfDay())
                      .orderByAsc(InOutRecord::getTime)
                      .last("LIMIT 1");
            InOutRecord outRecord = recordMapper.selectOne(outWrapper);
            if (outRecord != null) {
                record.setOutTime(outRecord.getTime());
            }
        }

        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }
}
