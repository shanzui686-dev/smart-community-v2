package com.smartcommunity.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.server.common.PageQuery;
import com.smartcommunity.server.common.PageResult;
import com.smartcommunity.server.entity.FeeStandard;
import com.smartcommunity.server.mapper.FeeStandardMapper;
import com.smartcommunity.server.service.FeeStandardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class FeeStandardServiceImpl implements FeeStandardService {

    private final FeeStandardMapper feeStandardMapper;

    public FeeStandardServiceImpl(FeeStandardMapper feeStandardMapper) {
        this.feeStandardMapper = feeStandardMapper;
    }

    @Override
    public PageResult<FeeStandard> pageList(PageQuery pageQuery, String keyword, Integer status) {
        Page<FeeStandard> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        LambdaQueryWrapper<FeeStandard> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(FeeStandard::getFeeName, keyword);
        }
        if (status != null) {
            wrapper.eq(FeeStandard::getStatus, status);
        }
        wrapper.orderByDesc(FeeStandard::getCreateTime);
        Page<FeeStandard> result = feeStandardMapper.selectPage(page, wrapper);
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public FeeStandard getById(Long id) {
        return feeStandardMapper.selectById(id);
    }

    @Override
    @Transactional
    public void add(FeeStandard feeStandard) {
        feeStandardMapper.insert(feeStandard);
    }

    @Override
    @Transactional
    public void update(FeeStandard feeStandard) {
        feeStandardMapper.updateById(feeStandard);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        feeStandardMapper.deleteById(id);
    }

    @Override
    public List<FeeStandard> listEnabled() {
        LambdaQueryWrapper<FeeStandard> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FeeStandard::getStatus, 1);
        return feeStandardMapper.selectList(wrapper);
    }
}
