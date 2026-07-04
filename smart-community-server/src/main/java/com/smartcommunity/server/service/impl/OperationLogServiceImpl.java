package com.smartcommunity.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.server.common.PageResult;
import com.smartcommunity.server.common.PageQuery;
import com.smartcommunity.server.entity.InOutRecord;
import com.smartcommunity.server.entity.OperationLog;
import com.smartcommunity.server.entity.Person;
import com.smartcommunity.server.mapper.*;
import com.smartcommunity.server.service.OperationLogService;
import com.smartcommunity.server.vo.StatisticsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogMapper operationLogMapper;
    private final CommunityMapper communityMapper;
    private final PersonMapper personMapper;
    private final InOutRecordMapper recordMapper;
    private final CameraMapper cameraMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public PageResult<OperationLog> pageList(PageQuery pageQuery, String keyword) {
        Page<OperationLog> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(OperationLog::getUsername, keyword).or().like(OperationLog::getModule, keyword);
        }
        wrapper.orderByAsc(OperationLog::getCreateTime);
        Page<OperationLog> result = operationLogMapper.selectPage(page, wrapper);
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public StatisticsVO getStatistics() {
        Long communityCount = communityMapper.selectCount(null);
        Long personCount = personMapper.selectCount(null);
        Long cameraCount = cameraMapper.selectCount(null);
        Long todayRecordCount = recordMapper.selectCount(new LambdaQueryWrapper<InOutRecord>()
                .apply("DATE(time) = CURDATE()"));

        Map<String, Long> communityPersonMap = new LinkedHashMap<>();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT c.name, COUNT(p.person_id) AS cnt FROM community c " +
                "LEFT JOIN person p ON c.community_id = p.community_id AND p.deleted = 0 " +
                "GROUP BY c.community_id, c.name");
        for (Map<String, Object> row : rows)
            communityPersonMap.put((String) row.get("name"), ((Number) row.get("cnt")).longValue());

        Map<String, Long> personTypeMap = new LinkedHashMap<>();
        personTypeMap.put("业主", personMapper.selectCount(new LambdaQueryWrapper<Person>().eq(Person::getPersonType, 1)));
        personTypeMap.put("租户", personMapper.selectCount(new LambdaQueryWrapper<Person>().eq(Person::getPersonType, 2)));
        personTypeMap.put("家属", personMapper.selectCount(new LambdaQueryWrapper<Person>().eq(Person::getPersonType, 3)));

        Map<String, Long> weeklyRecordMap = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            String date = LocalDate.now().minusDays(i).toString();
            Long count = recordMapper.selectCount(new LambdaQueryWrapper<InOutRecord>()
                    .apply("DATE(time) = {0}", date));
            weeklyRecordMap.put(date, count);
        }

        return StatisticsVO.builder()
                .communityCount(communityCount).personCount(personCount)
                .todayRecordCount(todayRecordCount).cameraCount(cameraCount)
                .communityPersonMap(communityPersonMap).personTypeMap(personTypeMap)
                .weeklyRecordMap(weeklyRecordMap).build();
    }
}
