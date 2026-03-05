package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;

@RestController
@RequestMapping("/admin/report")
@Api(tags = "数据统计相关接口")
@Slf4j
public class ReportController {
    @Autowired
    private ReportService reportService;

    /**
     * 营业额统计
     * @param begin
     * @param end
     * @return
     */
    @GetMapping("/turnoverStatistics")
    @ApiOperation("营业额统计")
    public Result<TurnoverReportVO> turnoverStatistics(@DateTimeFormat(pattern="yyyy-MM-dd") LocalDate begin,
                                                       @DateTimeFormat(pattern="yyyy-MM-dd") LocalDate end){
        log.info("查询日期区间{}到{}的营业数据", begin, end);
        return Result.success(reportService.getTurnoverStatistics(begin, end));
    }

    /**
     * 用户统计
     * @param begin
     * @param end
     * @return
     */
    @GetMapping("/userStatistics")
    @ApiOperation("用户统计")
    public Result<UserReportVO> userStatistics(@DateTimeFormat(pattern="yyyy-MM-dd") LocalDate begin,
                                               @DateTimeFormat(pattern="yyyy-MM-dd") LocalDate end){
        log.info("查询日期区间{}到{}的用户数据", begin, end);
        return Result.success(reportService.getUserStatistics(begin, end));
    }

    /**
     * 订单统计
     * @param begin
     * @param end
     * @return
     */
    @GetMapping("/ordersStatistics")
    @ApiOperation("订单统计")
    public Result<OrderReportVO> ordersStatistics(@DateTimeFormat(pattern="yyyy-MM-dd") LocalDate begin,
                                                  @DateTimeFormat(pattern="yyyy-MM-dd") LocalDate end){
        log.info("查询日期区间{}到{}的订单数据", begin, end);
        return Result.success(reportService.getOrdersStatistics(begin, end));
    }

    /**
     * 查询菜品销量排名top10
     * @param begin
     * @param end
     * @return
     */
    @GetMapping("/top10")
    @ApiOperation("查询菜品销量Top10")
    public Result<SalesTop10ReportVO> top10(@DateTimeFormat(pattern="yyyy-MM-dd") LocalDate begin,
                                            @DateTimeFormat(pattern="yyyy-MM-dd") LocalDate end){
        log.info("查询日期区间{}到{}的订单数据", begin, end);
        return Result.success(reportService.getTop10Dishes(begin, end));
    }

    @GetMapping("/export")
    @ApiOperation("导出营业报表数据")
    public void exportBusinessData(HttpServletResponse response){ // 为了将xlsx文件下载给用户，需要一个响应对象response
        log.info("导出营业报表数据");
        reportService.exportBusinessData(response);
    }
}
