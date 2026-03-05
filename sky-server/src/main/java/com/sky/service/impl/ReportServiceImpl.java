package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WorkspaceService workspaceService;

    /**
     * 生成日期列表（公用方法）
     * @param begin
     * @param end
     * @return
     */
    private List<LocalDate> generateDateList(LocalDate begin, LocalDate end) {
        long distance = ChronoUnit.DAYS.between(begin, end);
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        for (int i = 1; i <= distance; i++) {
            dateList.add(begin.plusDays(i));
        }
        return dateList;
    }

    /**
     * 获取指定时间段的营业额统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        // 生成起止日期的字符串
        List<LocalDate> dateList = generateDateList(begin, end);

        // 查询各日期对应的营业额，并拼接对应结果
        List<Double> turnoverList = new ArrayList<>(Collections.nCopies(dateList.size(), 0.0));
        List<Map<String, Object>> turnoverStat = orderMapper.getTurnoverStatistics(begin, end);
        for(Map<String, Object> map : turnoverStat){
            Integer index = (Integer) map.get("indexes");
            BigDecimal totalSales = (BigDecimal) map.get("total_sales");
            turnoverList.set(index, totalSales.doubleValue());
        }

        // 组装返回结果
        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
    }

    /**
     * 获取用户统计数据
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        // 生成起止日期的字符串
        List<LocalDate> dateList = generateDateList(begin, end);

        // 获取用户新增数据
        List<Integer> newUserList = new ArrayList<>(Collections.nCopies(dateList.size(), 0));
        List<Map<String, Object>> newUserStat = userMapper.getNewUserStatistics(begin, end);
        for(Map<String, Object> map : newUserStat){
            Integer index = (Integer) map.get("indexes");
            Long newUsers = (Long) map.get("new_users");
            newUserList.set(index, newUsers.intValue());
        }

        // 计算得到用户总数数据
        List<Integer> totalUserList = new ArrayList<>();
        for(int i = 0; i < newUserList.size(); i++){
            if(i == 0){
                totalUserList.add(newUserList.get(i));
            }else{
                totalUserList.add(totalUserList.get(i - 1) + newUserList.get(i));
            }
        }

        // 组装返回结果
        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .build();
    }

    /**
     * 获取订单统计数据
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO getOrdersStatistics(LocalDate begin, LocalDate end) {
        // 生成起止日期的字符串
        List<LocalDate> dateList = generateDateList(begin, end);

        // 统计各个日期的订单数据
        List<Integer> orderCountList = new ArrayList<>(Collections.nCopies(dateList.size(), 0));
        List<Map<String, Object>> orderStat = orderMapper.getOrderStatistics(begin, end, null);
        for(Map<String, Object> map : orderStat){
            Integer index = (Integer) map.get("indexes");
            Long orderCount = (Long) map.get("total_orders");
            orderCountList.set(index, orderCount.intValue());
        }

        // 统计各个日期的有效订单数
        List<Integer> validOrderCountList = new ArrayList<>(Collections.nCopies(dateList.size(), 0));
        List<Map<String, Object>> validOrderStat = orderMapper.getOrderStatistics(begin, end, 5);
        for(Map<String, Object> map : validOrderStat){
            Integer index = (Integer) map.get("indexes");
            Long validOrderCount = (Long) map.get("total_orders");
            validOrderCountList.set(index, validOrderCount.intValue());
        }

        // 计算总的订单数
        Integer totalOrderCount = orderCountList.stream().reduce(0, Integer::sum);
        // 计算有效的订单数
        Integer validOrderCount = validOrderCountList.stream().reduce(0, Integer::sum);
        // 计算订单完成率
        Double orderCompletionRate = 0.0;
        if(totalOrderCount != 0){
            orderCompletionRate = validOrderCount / (double) totalOrderCount;
        }

        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .orderCompletionRate(orderCompletionRate)
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .build();
    }

    /**
     * 获取销量排名前10的菜品
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO getTop10Dishes(LocalDate begin, LocalDate end) {
        List<GoodsSalesDTO> ret = orderMapper.getTop100Dishes(begin, end);
        List<String> nameList = ret.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        List<Integer> numberList = ret.stream().map(GoodsSalesDTO::getNumber)
                .map(Long::new)
                .map(Long::intValue)
                .collect(Collectors.toList());

        return SalesTop10ReportVO.builder()
                .nameList(StringUtils.join(nameList, ","))
                .numberList(StringUtils.join(numberList, ","))
                .build();
    }

    /**
     * 导出营业数据Excel报表
     * @param response
     */
    @Override
    public void exportBusinessData(HttpServletResponse response) {
        // 通过工作台Service查询数据库，获取近30天的营业数据
        LocalDate begin = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now().minusDays(1);
        BusinessDataVO businessDataVO = workspaceService.getBusinessData(LocalDateTime.of(begin, LocalTime.MIN),
                                                                         LocalDateTime.of(end, LocalTime.MAX));

        // 将该营业数据写入到Excel中
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("templates/SheetTemplate.xlsx");
        try{
            XSSFWorkbook workbook = new XSSFWorkbook(in);

            // 填充表格数据
            XSSFSheet sheet = workbook.getSheet("Sheet1");
            sheet.getRow(1).getCell(1).setCellValue("时间："+begin+"至"+end);

            XSSFRow row = sheet.getRow(3);
            row.getCell(2).setCellValue(businessDataVO.getTurnover());
            row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessDataVO.getNewUsers());

            row = sheet.getRow(4);
            row.getCell(2).setCellValue(businessDataVO.getValidOrderCount());
            row.getCell(4).setCellValue(businessDataVO.getUnitPrice());

            LocalDate date = begin;
            int i = 0;
            while(date.isBefore(end)){
                BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(date,LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));
                row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessData.getTurnover());
                row.getCell(3).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData.getUnitPrice());
                row.getCell(6).setCellValue(businessData.getNewUsers());

                date = date.plusDays(1);
                i++;
            }

            // 创建Excel文件，并通过输出流写出
            ServletOutputStream out = response.getOutputStream();
            workbook.write(out);

            // 关闭流
            out.close();
            workbook.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
