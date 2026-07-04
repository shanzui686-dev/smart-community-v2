package com.smartcommunity.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartcommunity.server.entity.Menu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MenuMapper extends BaseMapper<Menu> {

    @Select("SELECT DISTINCT m.* FROM role_menu rm " +
            "LEFT JOIN menu m ON rm.menu_id = m.menu_id " +
            "WHERE rm.role_id IN (SELECT role_id FROM user_role WHERE user_id = #{userId}) " +
            "AND m.type IN (1,2) AND m.visible = 1 AND m.status = 1 AND m.deleted = 0 " +
            "ORDER BY m.sort ASC")
    List<Menu> selectMenusByUserId(@Param("userId") Long userId);
}
