package com.smartcommunity.server.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsVO {
    private Long communityCount;
    private Long personCount;
    private Long todayRecordCount;
    private Long cameraCount;
    private Map<String, Long> communityPersonMap;
    private Map<String, Long> personTypeMap;
    private Map<String, Long> weeklyRecordMap;
}
