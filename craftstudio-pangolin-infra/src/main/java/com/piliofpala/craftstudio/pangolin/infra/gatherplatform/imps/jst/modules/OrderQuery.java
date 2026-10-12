package com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.jst.modules;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.piliofpala.craftstudio.pangolin.domain.ecommerceorder.exception.GatherOrderException;
import com.piliofpala.craftstudio.pangolin.domain.ecommerceorder.exception.QueryOrderException;
import com.piliofpala.craftstudio.pangolin.domain.ecommerceorder.vo.EcommerceOrder;
import com.piliofpala.craftstudio.pangolin.domain.ecommerceorder.vo.EcommerceOrderSpec;
import com.piliofpala.craftstudio.pangolin.domain.ecommerceorder.vo.EcommercePlatformType;
import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.platform.vo.GatherPlatformType;
import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.shop.entity.Shop;
import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.shop.exception.ShopNotUniqueException;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.dto.TsGatherOrderResult;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.jst.client.JstClient;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.jst.modules.constants.Warehouse;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.wdt.client.WdtClient;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.wdt.modules.RegionCodeMapper;
import com.piliofpala.craftstudio.shared.domain.geo.consignee.vo.Address;
import com.piliofpala.craftstudio.shared.domain.geo.consignee.vo.Consignee;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class OrderQuery {
    private static final System.Logger log = System.getLogger("craftstudio-pangolin");

    public JSONObject queryOrder(JstClient client, String orderId) {
        try {
            String response = client.execute("orders/out/simple/query", JSONObject.toJSONString(
                    Map.of("o_ids", new Integer[]{Integer.parseInt(orderId)})
            ));
            System.out.println(response);
            var respObj = JSONObject.parseObject(response);
            if(respObj.getInteger("code")!=0) {
                throw new QueryOrderException("聚水潭服务暂不可用");
            }
            var orders = respObj.getJSONObject("data").getJSONArray("orders");
            if(orders == null || orders.isEmpty()){
                return null;
            }
            if(orders.size()!=1) {
                throw new QueryOrderException("此id下订单不唯一，请联系相关业务人员处理");
            }
            return orders.getJSONObject(0);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            log.log(System.Logger.Level.ERROR, e.getMessage());
            throw new QueryOrderException("聚水潭服务暂不可用");
        }

    }

    public TsGatherOrderResult queryByTimePeriod(JstClient client, long ts) throws GatherOrderException {
        return queryByTimePeriodPageByPage(client, ts);
    }
    private TsGatherOrderResult queryByTimePeriodPageByPage(JstClient client, long ts) throws GatherOrderException {

        TsGatherOrderResult result = new TsGatherOrderResult(new ArrayList<>(), ts);

        int pageSize = 50;

        var params = new HashMap<String, Object>();
        params.put("start_ts", ts);
        params.put("page_size", pageSize);
        params.put("page_index", 1);
        params.put("is_get_total", false);
        params.put("wms_co_id", Warehouse.MAIN_WAREHOUSE_ID);//只查主仓
        try {
            String response = client.execute("orders/out/simple/query", JSONObject.toJSONString(params));
            var respObj = JSONObject.parseObject(response);
            log.log(System.Logger.Level.INFO, response);
            if(respObj == null){
                throw new GatherOrderException("收单失败: 聚水潭数据异常:" + response);
            }
            int code = respObj.getInteger("code");
            if(code!=0){
                throw new GatherOrderException("收单失败:" + respObj.getString("message"));
            }
            if(!respObj.containsKey("data")) {
                return result;
            }
            var data = respObj.getJSONObject("data");
            var maxTs = data.getLong("max_ts");
            var datas = data.getJSONArray("datas");
            if(datas == null || datas.isEmpty()) {//已经不再有新数据了，直接返回
                return result;
            }
            result.setEcommerceOrders(assembleEcommerceOrders(datas));
            result.setCurrentMaxTs(maxTs);
            if(maxTs <= ts) {//ts已经不再增加，应该返回结果了
                return result;
            }else {//否则继续递归下一页
                var nextResult = queryByTimePeriodPageByPage(client, maxTs);
                var nextOrders = nextResult.getEcommerceOrders().stream().filter(nextOrder->{
                    //去掉订单号重复的， ts的衔接处必然会出现一个重复的
                    return result.getEcommerceOrders().stream().noneMatch(order ->
                            order.getGatherPlatformOrderId().equals(nextOrder.getGatherPlatformOrderId())
                    );
                }).toList();

                if(!nextResult.getEcommerceOrders().isEmpty()) {
                    result.getEcommerceOrders().addAll(nextOrders);//添加后续递归的数据
                    result.setCurrentMaxTs(nextResult.getCurrentMaxTs());//将 currentMaxTs 更新为后续递归的最新结果
                }
            }
        } catch (IOException e) {
            log.log(System.Logger.Level.ERROR, e);
        }
        return result;
    }

    private List<EcommerceOrder> assembleEcommerceOrders(JSONArray datas) {
        var orders = new ArrayList<EcommerceOrder>();
        for (int i=0; i<datas.size(); i++) {
            var trade = datas.getJSONObject(i);
            if("Delete".equals(trade.getString("status"))) {
                continue;//作废的不要
            }
            EcommerceOrder order = new EcommerceOrder();
            order.setGatherPlatformType(GatherPlatformType.JST);
            order.setGatherPlatformOrderId(trade.getString("o_id"));
            order.setShopId(trade.getString("drp_co_id_from"));//聚水潭的【供应商】作为【店铺概念】
            order.setShopCode(trade.getString("drp_co_id_from"));
            order.setEcommerceShopName(trade.getString("shop_name"));
            order.setBuyerNick(trade.getString("shop_buyer_id"));
            order.setCustomerServiceRemark(trade.getString("remark"));
            order.setBuyerMessage(trade.getString("buyer_message"));
            try {
                order.setCreateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(trade.getString("created")));
                if(trade.getString("pay_date")!=null){
                    order.setPayTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(trade.getString("pay_date")));
                }
                order.setPlanDeliverTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(trade.getString("io_date")));
            }catch (ParseException e){
                log.log(System.Logger.Level.ERROR, e);
            }
            Consignee consignee = new Consignee();
            consignee.setName("***");
            consignee.setPhone("1**********");
            Address address = new Address();
            consignee.setAddress(address);
            address.setDetailAddress("**********");

            var labelStr = trade.getString("labels");
            if(labelStr!=null) {
                var labels = labelStr.split(",");
                if(labels.length>0) {
                    address.setTerminalRegionCode(resolveRegionCode(labels));
                    order.setEcommercePlatformType(findEcommercePlatformType(labels));
                }
            }
            if(order.getEcommercePlatformType() == null) {
                order.setEcommercePlatformType(EcommercePlatformType.UNKNOWN);
            }

            if(order.getEcommercePlatformType() == EcommercePlatformType.TB) {
                order.setEcommerceOrderId("聚水潭平台不提供淘宝系电商订单号");
            }else if(order.getEcommercePlatformType() == EcommercePlatformType.PDD){
                order.setEcommerceOrderId("聚水潭平台不提供拼多多电商订单号");
            }else if(trade.getString("raw_so_id")==null){
                order.setEcommerceOrderId("聚水潭平台不提供此电商平台的电商订单号");
            }else {
                order.setEcommerceOrderId(trade.getString("raw_so_id"));
            }

            order.setConsignee(consignee);

            var items = trade.getJSONArray("items");
            List<EcommerceOrderSpec> specs = new ArrayList<>();
            order.setSpecs(specs);
            for(int index = 0; index<items.size(); index++) {
                var item = items.getJSONObject(index);
                var specCode = item.getString("i_id");
                var specName = item.getString("name");
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
        return orders;
    }

    /**
     * @param labels
     * @return
     */
    private EcommercePlatformType findEcommercePlatformType(String[] labels) {
        if(labels.length == 0) {
            return null;
        }
        String ecommercePlatformLabel = null;
        for (String label : labels) {
            if(label.startsWith("ep-")) {
                ecommercePlatformLabel = label;
                break;
            }
        }
        if("ep-TB".equals(ecommercePlatformLabel)){
            return EcommercePlatformType.TB;
        }
        if("ep-JD".equals(ecommercePlatformLabel)){
            return EcommercePlatformType.JD;
        }
        if("ep-XHS".equals(ecommercePlatformLabel)){
            return EcommercePlatformType.XHS;
        }
        if("ep-PDD".equals(ecommercePlatformLabel)){
            return EcommercePlatformType.PDD;
        }
        //抖音系
        if("ep-DY".equals(ecommercePlatformLabel)){
            return EcommercePlatformType.DY;
        }

        return EcommercePlatformType.UNKNOWN;
    }

    private String resolveRegionCode(String[] labels) {
        if(labels.length == 0) {
            return null;
        }
        String bestRegionCode = null;
        for (String label : labels) {
            if(label == null || !label.startsWith("CN-")) {
                continue;
            }
            if(bestRegionCode == null || label.length()>bestRegionCode.length()) {//取最精准地区码
                bestRegionCode = label;
            }
        }
        return bestRegionCode;
    }
}
