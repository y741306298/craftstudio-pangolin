package com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.wdt;

import com.piliofpala.craftstudio.pangolin.domain.ecommerceorder.exception.GatherOrderException;
import com.piliofpala.craftstudio.pangolin.domain.ecommerceorder.vo.EcommerceOrder;
import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.platform.vo.GatherPlatformType;
import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.product.entity.ProductSpec;
import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.product.exception.BrandNotExistException;
import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.product.exception.ProductSpecPushException;
import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.shop.entity.Shop;
import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.supplier.entity.Supplier;
import com.piliofpala.craftstudio.pangolin.domain.logistics.exception.LogisticsWarehouseConfigException;
import com.piliofpala.craftstudio.pangolin.domain.logistics.vo.LogisticsLabel;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.GatherPlatform;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.dto.TsGatherOrderResult;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.wdt.client.WdtClient;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.wdt.modules.*;
import com.piliofpala.craftstudio.shared.domain.logistics.vo.LogisticsCarrierPresetType;

import java.util.Date;
import java.util.List;

public class WdtGatherPlatform extends GatherPlatform {
    private String env;
    private WdtClient client;

    private static final WdtGatherPlatform instance = new WdtGatherPlatform("正式环境",new WdtClient(
            "ywyl", "ywyl-ot", "2b0d48a9a7ee7a23a3ba97948aa8ce6b", "https://openapi.huice.com/openapi/"
    ));
    private static final WdtGatherPlatform testInstance = new WdtGatherPlatform("测试环境", new WdtClient(
            "haijun", "ywyl-test", "jW1D)WmUq!ve", "https://openapi.ali.huice.cc/openapi/"
    ));
    public static WdtGatherPlatform getInstance(){
        return instance;
    }
    public static WdtGatherPlatform getTestInstance(){
        return testInstance;
    }

    private WdtGatherPlatform() {
    }

    private WdtGatherPlatform(String env, WdtClient wdtClient) {
        this.env = env;
        client = wdtClient;
    }

    @Override
    public Shop queryShopByCode(String shopCode) {
        return new ShopQuery().queryShopByCode(client, shopCode);
    }

    @Override
    public void pushProductSpec(ProductSpec productSpec) throws BrandNotExistException, ProductSpecPushException {
        new GoodsPush().push(client, productSpec);
    }

    @Override
    public ProductSpec queryProductSpecByCode(String specCode) {
        return new GoodsQuery().queryProductSpecByCode(client, specCode);
    }

    @Override
    public List<EcommerceOrder> gatherOrders(Date startTime, Date endTime) throws GatherOrderException {
        return new TradeQuery().queryByTimePeriod(client, startTime, endTime);
    }

    @Override
    public TsGatherOrderResult gatherOrdersByTs(long ts) throws GatherOrderException {
        throw new RuntimeException("旺店通不支持ts收单模式，请使用【起止时间】模式");
    }

    @Override
    public void configLogisticsWarehouse(String logisticsId, String warehouseId, String uniCode) throws LogisticsWarehouseConfigException {
        new LogisticsWarehouseModify().modify(client, logisticsId, warehouseId, uniCode);
    }

    @Override
    public LogisticsLabel printLogisticsLabel(String uniCode, String printer, boolean dispatch, LogisticsCarrierPresetType carrierPresetType) {
        return new LogisticsPrint().print(client, uniCode, printer, dispatch, carrierPresetType);
    }

    @Override
    public LogisticsLabel rePrint(String uniCode, String printer, boolean dispatch, LogisticsCarrierPresetType carrierPresetType, String logisticsOrderId) {
        return new LogisticsPrint().rePrint(client, uniCode, printer, dispatch, carrierPresetType, logisticsOrderId);

    }

    @Override
    public String dumpInfo() {
        String content =  "===============旺店通（"+env+"）===============\n";
        content += "sid:"+client.getSid()+"\n";
        content += "appKey:"+client.getAppkey()+"\n";
        content += "appSecret:"+client.getAppsecret()+"\n";
        content += "baseUrl:"+client.getBaseUrl()+"\n";
        content +=  "=======================================\n";
        return content;
    }
}
