package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper {
    /**
     * 根据openid查询用户表
     * @param openid
     * @return
     */
    @Select("select * from user where openid = #{openid}")
    User getByOpenid(String openid);

    /**
     * 插入新用户数据
     * @param user
     */
    @Options(useGeneratedKeys = true, keyColumn = "id", keyProperty = "id")
    void insert(User user);

    /**
     * 根据用户id查询用户数据
     * @param userId
     * @return
     */
    @Select("select * from user where id = #{userId}")
    User getByUserId(Long userId);

    /**
     * 根据动态条件统计用户数量
     * @param map
     * @return
     */
    Integer countByMap(Map map);

    /**
     * 统计指定时间区间内的新增用户数据
     * @param begin
     * @param end
     * @return
     */
    List<Map<String, Object>> getNewUserStatistics(LocalDate begin, LocalDate end);
}
