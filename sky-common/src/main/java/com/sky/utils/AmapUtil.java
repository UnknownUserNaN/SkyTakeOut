package com.sky.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sky.exception.AddressNotFoundException;
import com.sky.properties.AmapProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class AmapUtil {

    @Autowired
    private AmapProperties amapProperties;

    /**
     * 根据地址解析经纬度
     * @param address
     * @return
     */
    private String getLonAndLatByAddress(String address){
        //构造参数，调用doGet
        Map<String,String> paramMap = new HashMap<>();
        paramMap.put("address",address);
        paramMap.put("key",amapProperties.getKey());
        paramMap.put("output",amapProperties.getOutput());

        //获取响应json字符串
        String result = HttpClientUtil.doGet(amapProperties.getGeoUrl(), paramMap);

        JSONObject jsonObject = JSONObject.parseObject(result);
        JSONArray geocodesArray = jsonObject.getJSONArray("geocodes");
        if(geocodesArray == null || geocodesArray.isEmpty()){
            throw new AddressNotFoundException("地址解析失败," + address);
        }

        //返回经纬度
        return geocodesArray.getJSONObject(0).getString("location");
    }

    /**
     * 计算两个经纬度之间的距离
     * @param originAddress
     * @param destinationAddress
     * @return
     */
    public Integer getDistance(String originAddress, String destinationAddress) {
        String originLonAndLat = getLonAndLatByAddress(originAddress);
        String destinationLonAndLat = getLonAndLatByAddress(destinationAddress);

        //构造请求参数
        Map<String,String> paramMap = new HashMap<>();
        paramMap.put("origins",originLonAndLat);
        paramMap.put("destination",destinationLonAndLat);
        paramMap.put("key",amapProperties.getKey());
        paramMap.put("output",amapProperties.getOutput());
        String result = HttpClientUtil.doGet(amapProperties.getDistanceUrl(), paramMap);

        //解析响应结果
        JSONObject jsonObject = JSONObject.parseObject(result);
        return Integer.valueOf(jsonObject.getJSONArray("results").getJSONObject(0).getString("distance"));
    }

}
