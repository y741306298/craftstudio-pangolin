package com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.jst;

import com.piliofpala.craftstudio.pangolin.domain.ecommerceorder.exception.GatherOrderException;
import com.piliofpala.craftstudio.pangolin.domain.ecommerceorder.vo.EcommerceOrder;
import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.product.entity.ProductSpec;
import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.product.exception.BrandNotExistException;
import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.product.exception.ProductSpecPushException;
import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.shop.entity.Shop;
import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.supplier.entity.Supplier;
import com.piliofpala.craftstudio.pangolin.domain.logistics.exception.LogisticsWarehouseConfigException;
import com.piliofpala.craftstudio.pangolin.domain.logistics.vo.LogisticsLabel;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.GatherPlatform;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.dto.TsGatherOrderResult;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.jst.client.JstClient;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.jst.modules.*;
import com.piliofpala.craftstudio.shared.domain.logistics.vo.LogisticsCarrierPresetType;

import java.util.Date;
import java.util.List;

public class JstGatherPlatform extends GatherPlatform {
    private String env;
    private JstClient client;
    private static final JstGatherPlatform instance = new JstGatherPlatform("正式环境",new JstClient(
            "3a25371919074892be91a0485a1c5cb1",
            "4e490e53704b4b14b3c063bf0eba4a41",
            "https://openapi.jushuitan.com/"
    ));
    private static final JstGatherPlatform testInstance = new JstGatherPlatform("测试环境", new JstClient(
            "04583fe4b7a04a88ad6ebb348d3a54a9",
            "305f0b57c6234da59525ead880dd9d6f",
            "https://dev-api.jushuitan.com/"
    ));
    public static JstGatherPlatform getInstance(){
        return instance;
    }
    public static JstGatherPlatform getTestInstance(){
        return testInstance;
    }

    private JstGatherPlatform() {
    }

    private JstGatherPlatform(String env, JstClient jstClient) {
        this.env = env;
        this.client = jstClient;
    }

    @Override
    public Shop queryShopByCode(String shopCode) {
        // 聚水潭的【店铺】无法穿透订单，只有供应商信息可以，所以为了保持统一，聚水潭的供应商作为Shop
        Supplier supplier =  new SupplierQuery().queryBySupplierId(client, shopCode);
        if(supplier == null){
            return null;
        }
        Shop shop = new Shop();
        shop.setId(supplier.getId());
        shop.setCode(supplier.getId());
        shop.setCode(supplier.getId());
        shop.setName(supplier.getName());
        return shop;
    }


    @Override
    public void pushProductSpec(ProductSpec productSpec) throws BrandNotExistException, ProductSpecPushException {
        new SkuPush().push(client, productSpec);
    }

    @Override
    public ProductSpec queryProductSpecByCode(String specCode) {
        return new SkuQuery().queryProductSpecByCode(client, specCode);
    }

    @Override
    public List<EcommerceOrder> gatherOrders(Date startTime, Date endTime) throws GatherOrderException {
        throw new RuntimeException("聚水潭不支持【起止时间】收单模式，请使用【ts】模式");
    }

    @Override
    public TsGatherOrderResult gatherOrdersByTs(long ts) throws GatherOrderException {
        return new OrderQuery().queryByTimePeriod(client, ts);
    }

    @Override
    public void configLogisticsWarehouse(String logisticsId, String warehouseId, String uniCode) throws LogisticsWarehouseConfigException {
        new WarehouseModify().modify(client, warehouseId, uniCode);
        new LogisticsModify().modify(client, logisticsId, uniCode);
    }

    @Override
    public LogisticsLabel printLogisticsLabel(String uniCode, String printer, boolean dispatch, LogisticsCarrierPresetType carrierPresetType) {
        return new LogisticsPrint().print(client, uniCode, printer ,dispatch, carrierPresetType);
    }

    @Override
    public LogisticsLabel rePrint(String uniCode, String printer, boolean dispatch, LogisticsCarrierPresetType carrierPresetType, String logisticsOrderId) {
        return new LogisticsPrint().print(client, uniCode, printer ,dispatch, carrierPresetType);
    }

    @Override
    public String dumpInfo() {
        return "";
    }
}
