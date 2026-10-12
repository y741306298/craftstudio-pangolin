package com.piliofpala.craftstudio.pangolin.infra.gatherplatform.dto;

import com.piliofpala.craftstudio.pangolin.domain.ecommerceorder.vo.EcommerceOrder;

import java.util.List;

public class TsGatherOrderResult {
    List<EcommerceOrder> ecommerceOrders;
    long currentMaxTs;

    public TsGatherOrderResult(List<EcommerceOrder> ecommerceOrders, long currentMaxTs) {
        this.ecommerceOrders = ecommerceOrders;
        this.currentMaxTs = currentMaxTs;
    }

    public List<EcommerceOrder> getEcommerceOrders() {
        return ecommerceOrders;
    }

    public void setEcommerceOrders(List<EcommerceOrder> ecommerceOrders) {
        this.ecommerceOrders = ecommerceOrders;
    }

    public long getCurrentMaxTs() {
        return currentMaxTs;
    }

    public void setCurrentMaxTs(long currentMaxTs) {
        this.currentMaxTs = currentMaxTs;
    }
}
