package com.piliofpala.craftstudio.pangolin.infra.gatherplatform;

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
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.dto.TsGatherOrderResult;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.jst.JstGatherPlatform;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.wdt.WdtGatherPlatform;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.wdt.client.WdtClient;
import com.piliofpala.craftstudio.shared.domain.logistics.vo.LogisticsCarrierPresetType;

import java.util.Date;
import java.util.List;

public abstract class GatherPlatform {
    //店铺相关
    public abstract Shop queryShopByCode(String shopCode);

    //产品相关
    public abstract void pushProductSpec(ProductSpec productSpec)  throws BrandNotExistException, ProductSpecPushException;
    public abstract ProductSpec queryProductSpecByCode(String specCode);


    //订单相关
    public abstract List<EcommerceOrder> gatherOrders(Date startTime, Date endTime) throws GatherOrderException;
    public abstract TsGatherOrderResult gatherOrdersByTs(long ts) throws GatherOrderException;

    //物流相关
    public abstract void configLogisticsWarehouse(String logisticsId, String warehouseId, String uniCode) throws LogisticsWarehouseConfigException;

    public abstract LogisticsLabel printLogisticsLabel(String uniCode, String printer, boolean dispatch, LogisticsCarrierPresetType carrierPresetType);
    public abstract LogisticsLabel rePrint(String uniCode, String printer, boolean dispatch, LogisticsCarrierPresetType carrierPresetType, String logisticsOrderId);
    public static GatherPlatform getInstance(GatherPlatformType type){
        if(type == GatherPlatformType.WDT){
            return WdtGatherPlatform.getInstance();
        }
        if(type == GatherPlatformType.JST){
            return JstGatherPlatform.getInstance();
        }
        throw new UnsupportedOperationException("暂不支持的聚单平台类型："+type);
    }

    public static GatherPlatform getTestInstance(GatherPlatformType type){
        if(type == GatherPlatformType.WDT){
            return WdtGatherPlatform.getTestInstance();
        }
        if(type == GatherPlatformType.JST){
            return JstGatherPlatform.getTestInstance();
        }
        throw new UnsupportedOperationException("暂不支持的聚单平台类型："+type);
    }

    public abstract String dumpInfo();
}
