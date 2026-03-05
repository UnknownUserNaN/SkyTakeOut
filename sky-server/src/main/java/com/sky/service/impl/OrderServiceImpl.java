package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.properties.ShopProperties;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.service.UserService;
import com.sky.utils.AmapUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private WebSocketServer webSocketServer;

    // 与定位相关的配置
    @Autowired
    private AmapUtil amapUtil;
    @Autowired
    private ShopProperties shopProperties;


    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    @Override
    @Transactional
    public OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO) {
        // 处理地址簿为空的异常情况
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if(addressBook == null){
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        // 处理订单为空的异常情况
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> carts = shoppingCartMapper.list(shoppingCart); // 通过模糊查询查询对应用户ID的购物车数据
        if(carts == null || carts.size() == 0){
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        // 处理配送距离过远的异常情况（Day9练习的进阶要求）
        String destinationAddress = addressBook.getProvinceName() + " " + addressBook.getCityName() + " "
                + addressBook.getDistrictName() + " " + addressBook.getDetail();
        String originAddress = shopProperties.getAddress();
        Integer distanceThreshold = shopProperties.getDistanceThreshold();
        Integer distance = amapUtil.getDistance(originAddress, destinationAddress);
        if(distance > distanceThreshold){
            throw new OrderBusinessException(MessageConstant.DELIVERY_OUT_OF_RANGE);
        }

        // 向订单表插入一条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID); // 设置订单支付状态
        orders.setStatus(Orders.PENDING_PAYMENT); // 设置订单状态
        orders.setNumber(String.valueOf(System.currentTimeMillis())); // 设置订单号（用系统时间戳代替）
        orders.setPhone(addressBook.getPhone()); // 设置收货人手机号
        orders.setUserId(userId); // 设置用户ID
        orderMapper.insert(orders);

        // 向订单明细表插入多条数据
        List<OrderDetail> orderDetails = new ArrayList<>();
        for(ShoppingCart cart : carts){
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetails.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetails);

        // 清空购物车
        shoppingCartMapper.cleanShoppingCart(userId);

        // 封装响应结果并返回
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderTime(orders.getOrderTime())
                .orderAmount(orders.getAmount())
                .build();
        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getByUserId(userId);

        // 自行修改：跳过申请微信支付的过程
//        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );
        // 自行修改：生成空JSON，模拟微信支付成功
        JSONObject jsonObject = new JSONObject();

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);

        // 通过WebSocket向客户端推送短信
        Map map = new HashMap();
        map.put("type", 1);
        map.put("orderId", ordersDB.getId());
        map.put("content", "订单号：" + outTradeNo);
        webSocketServer.sendToAllClient(JSON.toJSONString(map));
    }

    /**
     * 历史订单查询
     *
     * @param page
     * @param pageSize
     * @param status
     * @return
     */
    @Override
    public PageResult getHistoryOrders(Integer page, Integer pageSize, Integer status) {
        // 查出当前页面的所有订单
        Long userId = BaseContext.getCurrentId();
        PageHelper.startPage(page, pageSize);
        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(userId);
        ordersPageQueryDTO.setStatus(status);

        // 分页条件查询
        Page<Orders> pages = orderMapper.pageQuery(ordersPageQueryDTO);
        List<OrderVO> list = new ArrayList();

        // 查询出订单明细，并封装入OrderVO进行响应
        if (pages != null && pages.getTotal() > 0) {
            for (Orders orders : pages) {
                Long orderId = orders.getId();// 订单id

                // 查询订单明细
                List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orderId);

                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDetailList(orderDetails);

                list.add(orderVO);
            }
        }
        return new PageResult(pages.getTotal(), list);
    }

    /**
     * 订单详情
     *
     * @param id
     * @return
     */
    @Override
    public OrderVO getOrderDetail(Long id) {
        // 根据id查询订单，注意联查其他表
        Orders order = orderMapper.getByOrderId(id);
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

        // 设置地址信息
        AddressBook addressBook = addressBookMapper.getById(order.getAddressBookId());
        String address = addressBook.getProvinceName() + addressBook.getCityName() + addressBook.getDistrictName() + addressBook.getDetail();

        // 组装订单详情结果
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(order, orderVO);
        orderVO.setConsignee(addressBook.getConsignee());
        orderVO.setAddress(address);
        orderVO.setOrderDishes(orderDetailList.stream().map(
                orderDetail -> orderDetail.getName() + "×" + orderDetail.getNumber()).collect(Collectors.joining(",")));
        orderVO.setOrderDetailList(orderDetailList);
        return orderVO;
    }

    /**
     * 取消订单
     *
     * @param ordersCancelDTO
     */
    @Override
    public void cancelOrder(OrdersCancelDTO ordersCancelDTO) {
        Orders orders = orderMapper.getByOrderId(ordersCancelDTO.getId());

        // 如果用户已经支付，则进行退款
        if (orders.getPayStatus().equals(Orders.PAID)) {
            //用户已支付，需要退款
//            String refund = weChatPayUtil.refund(
//                    orders.getNumber(),
//                    orders.getNumber(),
//                    new BigDecimal(0.01),
//                    new BigDecimal(0.01));
//            log.info("申请退款：{}", refund);
            log.info("已经完成模拟退款！");
        }

        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orders.setCancelTime(LocalDateTime.now());

        orderMapper.update(orders);
    }

    /**
     * 再来一单
     *
     * @param id
     */
    @Override
    public void getOneMoreOrder(Long id) {
        // 复制相关的支付信息
        OrdersSubmitDTO ordersSubmitDTO = new OrdersSubmitDTO();
        Orders orders = orderMapper.getByOrderId(id);
        BeanUtils.copyProperties(orders, ordersSubmitDTO);
        // 添加相同的商品到购物车
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);
        for (OrderDetail orderDetail : orderDetailList) {
            ShoppingCart cart = new ShoppingCart();
            BeanUtils.copyProperties(orderDetail, cart);
            cart.setUserId(BaseContext.getCurrentId());
            cart.setCreateTime(LocalDateTime.now());
            shoppingCartMapper.insert(cart);
        }
        // 复用提交订单的方法
        submit(ordersSubmitDTO);
    }

    /**
     * 根据条件查询订单（也是分页查询的来源）
     *
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult getByQuery(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        // 分页条件查询
        Page<Orders> pages = orderMapper.pageQuery(ordersPageQueryDTO);
        List<OrderVO> list = new ArrayList();

        // 查询出订单明细，并封装入OrderVO进行响应
        if (pages != null && pages.getTotal() > 0) {
            for (Orders orders : pages) {
                Long orderId = orders.getId();// 订单id

                // 查询订单明细
                List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orderId);

                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDetailList(orderDetails);

                // 填充用户名
                Long userId = orders.getUserId();
                User user = userMapper.getByUserId(userId);
                String username = user.getName();
                orderVO.setUserName(username);

                // 填充地址信息
                Long addressBookId = orders.getAddressBookId();
                AddressBook addressBook = addressBookMapper.getById(addressBookId);
                String address = addressBook.getProvinceName() + addressBook.getCityName() +
                        addressBook.getDistrictName() + addressBook.getDetail();
                String consignee = addressBook.getConsignee();
                orderVO.setAddress(address);
                orderVO.setConsignee(consignee);

                list.add(orderVO);
            }
        }
        return new PageResult(pages.getTotal(), list);
    }

    /**
     * 统计订单数据
     *
     * @return
     */
    @Override
    public Map<String, Integer> getStatistics() {
        return orderMapper.getStatistics();
    }

    /**
     * 商家接单
     *
     * @param id
     */
    @Override
    public void confirmOrder(Long id) {
        Orders orders = orderMapper.getByOrderId(id);
        // 只有订单状态为待确认时才可以确认订单
        if(orders.getStatus() != null && !orders.getStatus().equals(Orders.TO_BE_CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        orders.setStatus(Orders.CONFIRMED);
        orderMapper.update(orders);
    }

    /**
     * 商家拒单
     *
     * @param ordersRejectionDTO
     */
    @Override
    public void rejectOrder(OrdersRejectionDTO ordersRejectionDTO) {
        // 获取当前订单
        Orders orders = orderMapper.getByOrderId(ordersRejectionDTO.getId());

        // 只有订单状态为待确认时才可以拒绝订单
        if(orders.getStatus() != null && !orders.getStatus().equals(Orders.TO_BE_CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 如果用户已经支付，则进行退款
        if (orders.getPayStatus().equals(Orders.PAID)) {
            //用户已支付，需要退款
//            String refund = weChatPayUtil.refund(
//                    orders.getNumber(),
//                    orders.getNumber(),
//                    new BigDecimal(0.01),
//                    new BigDecimal(0.01));
//            log.info("申请退款：{}", refund);
            log.info("已经完成模拟退款！");
        }
        orders.setStatus(Orders.CANCELLED);
        orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        orders.setCancelTime(LocalDateTime.now());

        orderMapper.update(orders);
    }

    /**
     * 派送订单
     *
     * @param id
     */
    @Override
    public void deliverOrder(Long id) {
        // 获取当前订单
        Orders orders = orderMapper.getByOrderId(id);

        // 只有订单状态为确认时才可以派送订单
        if(orders.getStatus() != null && !orders.getStatus().equals(Orders.CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);
        orderMapper.update(orders);
    }

    /**
     * 完成订单
     *
     * @param id
     */
    @Override
    public void completeOrder(Long id) {
        // 获取当前订单
        Orders orders = orderMapper.getByOrderId(id);

        // 只有订单状态为派送时才可以派送订单
        if(orders.getStatus() != null && !orders.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    /**
     * 催单
     *
     * @param id
     */
    @Override
    public void reminder(Long id) {
        Orders orders = orderMapper.getByOrderId(id);

        if(orders == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 调用WebSocket，向商家端发送消息
        Map map = new HashMap();
        map.put("type", 2);
        map.put("orderId", id);
        map.put("content", "订单号：" + orders.getNumber());
        webSocketServer.sendToAllClient(JSON.toJSONString(map)); // 向所有客户端浏览器推送消息
    }

}
