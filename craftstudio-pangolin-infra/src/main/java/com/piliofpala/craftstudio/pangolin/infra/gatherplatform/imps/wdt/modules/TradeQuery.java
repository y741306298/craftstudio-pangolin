package com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.wdt.modules;

import com.alibaba.fastjson2.JSONObject;
import com.piliofpala.craftstudio.pangolin.domain.ecommerceorder.exception.GatherOrderException;
import com.piliofpala.craftstudio.pangolin.domain.ecommerceorder.vo.EcommerceOrder;
import com.piliofpala.craftstudio.pangolin.domain.ecommerceorder.vo.EcommerceOrderSpec;
import com.piliofpala.craftstudio.pangolin.domain.ecommerceorder.vo.EcommercePlatformType;
import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.platform.vo.GatherPlatformType;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.wdt.client.WdtClient;
import com.piliofpala.craftstudio.shared.domain.geo.consignee.vo.Address;
import com.piliofpala.craftstudio.shared.domain.geo.consignee.vo.Consignee;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class TradeQuery {
    private static final System.Logger log = System.getLogger("craftstudio-pangolin");

    public String queryTradeIdByTradeNo(WdtClient client, String tradeNo) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("trade_no", tradeNo);
        try {
            String response = client.execute("trade_query.php", params);
            log.log(System.Logger.Level.INFO, response);
            var respObj = JSONObject.parseObject(response);
            if(!respObj.containsKey("trades")){
                return null;
            }
            var trades = respObj.getJSONArray("trades");
            if(trades == null || trades.size()!=1) {
                return null;
            }
            return trades.getJSONObject(0).getString("trade_id");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public List<EcommerceOrder> queryByTimePeriod(WdtClient client, Date startTime, Date endTime) throws GatherOrderException {
        return queryByTimePeriodPageByPage(0, client, startTime, endTime);
    }

    private List<EcommerceOrder> queryByTimePeriodPageByPage(int current, WdtClient client, Date startTime, Date endTime) throws GatherOrderException {
        int pageSize = 100;
        Map<String, String> params = new HashMap<String, String>();
        params.put("page_no", String.valueOf(current));
        params.put("page_size", String.valueOf(pageSize));
        params.put("start_time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(startTime));
        params.put("end_time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(endTime));
        params.put("time_type", "2");//使用trade_time
        List<EcommerceOrder> orders = new ArrayList<>();
        try {
            String response = client.execute("trade_query.php", params);
            var respObj = JSONObject.parseObject(response);
            log.log(System.Logger.Level.INFO, response);
            if(respObj == null){
                throw new GatherOrderException("收单失败: 旺店通数据异常:" + response);
            }
            int code = respObj.getInteger("code");
            if(code!=0){
                throw new GatherOrderException("收单失败:" + respObj.getString("message"));
            }
            if(!respObj.containsKey("trades")) {
                return orders;
            }
            var trades = respObj.getJSONArray("trades");
            if(trades == null || trades.isEmpty()){
                return orders;
            }
            for (int i=0; i<trades.size(); i++) {
                var trade = trades.getJSONObject(i);
                EcommerceOrder order = new EcommerceOrder();
                order.setGatherPlatformType(GatherPlatformType.WDT);
                order.setGatherPlatformOrderId(trade.getString("trade_no"));
                order.setShopId(trade.getString("shop_id"));
                order.setShopCode(trade.getString("shop_no"));
                order.setEcommercePlatformType(findEcommercePlatformType(trade.getString("fenxiao_platform_id")));
                order.setEcommerceOrderId(trade.getString("fenxiao_tid"));
                order.setEcommerceShopName(trade.getString("fenxiao_shop_name"));
                order.setBuyerNick(trade.getString("buyer_nick"));
                order.setCustomerServiceRemark(trade.getString("cs_remark"));
                order.setBuyerMessage(trade.getString("buyer_message"));
                try {
                    order.setCreateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(trade.getString("trade_time")));
                    order.setPayTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(trade.getString("pay_time")));
                    order.setPlanDeliverTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(trade.getString("plan_deliver_time")));
                }catch (ParseException e){
                    e.printStackTrace();
                    log.log(System.Logger.Level.ERROR, e);
                }
                Consignee consignee = new Consignee();
                consignee.setName(trade.getString("receiver_name"));
                consignee.setPhone(trade.getString("receiver_mobile"));
                Address address = new Address();
                consignee.setAddress(address);
                address.setDetailAddress(trade.getString("receiver_address"));
                address.setTerminalRegionCode(
                        "CN-" + resolveRegionCode(trade.getString("receiver_city"), trade.getString("receiver_district"))
                );
                order.setConsignee(consignee);

                var goodsList = trade.getJSONArray("goods_list");
                List<EcommerceOrderSpec> specs = new ArrayList<>();
                order.setSpecs(specs);
                for(int goodsIndex = 0; goodsIndex<goodsList.size(); goodsIndex++) {
                    var goods = goodsList.getJSONObject(goodsIndex);
                    var specCode = goods.getString("spec_no");
                    var specName = goods.getString("goods_name");
                    if(specCode==null || specCode.startsWith("virtual_product")) {
                        log.log(System.Logger.Level.INFO, "虚拟货品规格，忽略:" + specCode);
                        continue;
                    }
                    EcommerceOrderSpec spec = new EcommerceOrderSpec();
                    spec.setCode(specCode);
                    spec.setName(specName);
                    specs.add(spec);
                }
                orders.add(order);
            }

            int total = respObj.getInteger("total_count");
            if((current+1)*pageSize < total) {
                var nextOrders = queryByTimePeriodPageByPage(current+1,client,startTime, endTime);
                orders.addAll(nextOrders);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return orders;
    }

    /**
     * 参考：https://open.wangdian.cn/qjb/open/guide?path=qjbguide_ptdmbb
     * @param fenXiaoPlatformId
     * @return
     */
    private EcommercePlatformType findEcommercePlatformType(String fenXiaoPlatformId) {
        if(fenXiaoPlatformId == null || fenXiaoPlatformId.isBlank()) {
            return EcommercePlatformType.UNKNOWN;
        }
        if("1".equals(fenXiaoPlatformId)){
            return EcommercePlatformType.TB;
        }
        if("3".equals(fenXiaoPlatformId)){
            return EcommercePlatformType.JD;
        }
        if("56".equals(fenXiaoPlatformId)){
            return EcommercePlatformType.XHS;
        }
        if("39".equals(fenXiaoPlatformId)){
            return EcommercePlatformType.PDD;
        }
        //抖音系
        if("139".equals(fenXiaoPlatformId)){
            return EcommercePlatformType.DY;
        }
        if("154".equals(fenXiaoPlatformId)){
            return EcommercePlatformType.DY;
        }
        if("193".equals(fenXiaoPlatformId)){
            return EcommercePlatformType.DY;
        }
        if("201".equals(fenXiaoPlatformId)){
            return EcommercePlatformType.DY;
        }
        if("69".equals(fenXiaoPlatformId)){//放心购(抖店）
            return EcommercePlatformType.DY;
        }
        return EcommercePlatformType.UNKNOWN;
    }

    private String resolveRegionCode(String receiverCity, String receiverDistrict) {
        String regionCode = receiverDistrict;
        // 旺店通的地区码规则比较SB，对应省直辖的县级市（比如湖北省 潜江市 潜江市），他们把这个地理层级上本应该是district级别，应该挂在
        // district字段上的情况给“政治化”了，认为【省直辖的县级市】和地级市“平级”，因此把这个地区码从本来应该所在的 district 字段上拿掉了，
        // 给放到了receiver_city上。
        // 而为了解决被硬生生拿走的本应该属于 district 的问题，他们这时候又给 district 赋予了 【0】这个奇怪的、不存在的类似补丁的“地区码”
        if(regionCode == null || "0".equals(regionCode)) {
            //所以这时候只能退而求其次，使用 receiverCity 而不是 receiverDistrict
            regionCode = receiverCity;
            //但要注意的是：旺店通的城市code后面有00结尾，但我们系统中的城市代码没有，因此要去掉
            regionCode = regionCode.replaceAll("0+$", "");
        }

        return RegionCodeMapper.map(regionCode);//然后再完成一次转译

    }

}
