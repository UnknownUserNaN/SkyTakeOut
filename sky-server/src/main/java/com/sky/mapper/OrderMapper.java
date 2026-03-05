package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.GoodsSalesDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import com.sky.entity.User;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {
    /**
     * 插入订单数据
     * @param orders
     */
    void insert(Orders orders);

    /**
     * 根据订单号查询订单
     * @param outTradeNo
     * @return
     */
    @Select("select * from orders where number = #{outTradeNo}")
    Orders getByNumber(String outTradeNo);

    /**
     * 更新订单状态
     * @param orders
     */
    void update(Orders orders);

    /**
     * 分页查询订单
     * @param ordersPageQueryDTO
     * @return
     */
    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据id查询订单
     * @param id
     */
    @Select("select * from orders where id = #{id}")
    Orders getByOrderId(Long id);

    /**
     * 根据订单状态统计订单数量
     * @return
     */
    Map<String, Integer> getStatistics();

    /**
     * 根据动态条件统计订单数量
     * @param map
     */
    Integer countByMap(Map map);

    /**
     * 根据动态条件统计营业额
     * @param map
     */
    Double sumByMap(Map map);

    /**
     * 统计指定时间区间内的订单数量
     * @param status
     * @param orderTime
     */
    @Select("select count(id) from orders where status = #{status} and order_time < #{orderTime}")
    List<Orders> getByStatusAndOrderTime(Integer status, LocalDateTime orderTime);

    /**
     * 统计指定时间区间内的营业额数据
     * @param begin
     * @param end
     * @return
     */
    List<Map<String, Object>> getTurnoverStatistics(LocalDate begin, LocalDate end);

    /**
     * 统计指定时间区间内的订单数据（指定时间筛选）
     * @param begin
     * @param end
     * @param status
     * @return
     */
    List<Map<String, Object>> getOrderStatistics(LocalDate begin, LocalDate end, Integer status);

    /**
     * 统计指定时间区间内的销量排名
     * @param begin
     * @param end
     * @return
     */
    List<GoodsSalesDTO> getTop100Dishes(LocalDate begin, LocalDate end);
}
