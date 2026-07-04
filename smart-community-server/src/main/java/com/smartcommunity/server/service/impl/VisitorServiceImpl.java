package com.smartcommunity.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.server.common.PageResult;
import com.smartcommunity.server.common.PageQuery;
import com.smartcommunity.server.entity.Community;
import com.smartcommunity.server.entity.InOutRecord;
import com.smartcommunity.server.entity.Visitor;
import com.smartcommunity.server.mapper.CommunityMapper;
import com.smartcommunity.server.mapper.InOutRecordMapper;
import com.smartcommunity.server.mapper.VisitorMapper;
import com.smartcommunity.server.service.VisitorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class VisitorServiceImpl implements VisitorService {

    private final VisitorMapper visitorMapper;
    private final InOutRecordMapper recordMapper;
    private final CommunityMapper communityMapper;

    public VisitorServiceImpl(VisitorMapper visitorMapper, InOutRecordMapper recordMapper,
                              CommunityMapper communityMapper) {
        this.visitorMapper = visitorMapper;
        this.recordMapper = recordMapper;
        this.communityMapper = communityMapper;
    }

    @Override
    public PageResult<Visitor> pageList(PageQuery pageQuery, String name, Long communityId, Integer status) {
        Page<Visitor> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        LambdaQueryWrapper<Visitor> wrapper = new LambdaQueryWrapper<>();
        if (name != null && !name.isBlank()) {
            wrapper.like(Visitor::getName, name);
        }
        if (communityId != null) {
            wrapper.eq(Visitor::getCommunityId, communityId);
        }
        if (status != null) {
            wrapper.eq(Visitor::getStatus, status);
        }
        wrapper.orderByAsc(Visitor::getCreateTime);
        Page<Visitor> result = visitorMapper.selectPage(page, wrapper);

        // 填充社区名称和签到时间
        for (Visitor v : result.getRecords()) {
            if (v.getCommunityId() != null) {
                Community c = communityMapper.selectById(v.getCommunityId());
                if (c != null) v.setCommunityName(c.getName());
            }
            // 状态>=2（已签到/已签退）时，visitTime已被覆盖为签到时间
            if (v.getStatus() >= 2) {
                v.setCheckInTime(v.getVisitTime());
            }
        }

        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public Visitor getById(Long id) {
        return visitorMapper.selectById(id);
    }

    @Override
    @Transactional
    public void add(Visitor visitor) {
        visitorMapper.insert(visitor);
    }

    @Override
    @Transactional
    public void update(Visitor visitor) {
        visitorMapper.updateById(visitor);
    }

    @Override
    @Transactional
    public void cancel(Long id) {
        Visitor visitor = visitorMapper.selectById(id);
        if (visitor != null) {
            visitor.setStatus(4);
            visitorMapper.updateById(visitor);
        }
    }

    @Override
    @Transactional
    public void checkIn(Long id) {
        Visitor visitor = visitorMapper.selectById(id);
        if (visitor != null && visitor.getStatus() == 1) {
            LocalDateTime now = LocalDateTime.now();
            visitor.setStatus(2);
            visitor.setVisitTime(now);
            visitorMapper.updateById(visitor);

            // 同步创建进入记录
            InOutRecord record = new InOutRecord();
            record.setPersonName(visitor.getName());
            record.setCommunityId(visitor.getCommunityId());
            record.setType(1);
            record.setTime(now);
            record.setLocation(visitor.getHouseNo());
            record.setVerifyType(3); // 3=访客登记
            record.setVerified(1);
            recordMapper.insert(record);
        }
    }

    @Override
    @Transactional
    public void checkOut(Long id) {
        Visitor visitor = visitorMapper.selectById(id);
        if (visitor != null && visitor.getStatus() == 2) {
            LocalDateTime now = LocalDateTime.now();
            visitor.setStatus(3);
            visitor.setLeaveTime(now);
            visitorMapper.updateById(visitor);

            // 同步创建离开记录
            InOutRecord record = new InOutRecord();
            record.setPersonName(visitor.getName());
            record.setCommunityId(visitor.getCommunityId());
            record.setType(2);
            record.setTime(now);
            record.setLocation(visitor.getHouseNo());
            record.setVerifyType(3);
            record.setVerified(1);
            recordMapper.insert(record);
        }
    }
}
