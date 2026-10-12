package com.piliofpala.craftstudio.pangolin.infra.gatherplatform.jst;

import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.platform.vo.GatherPlatformType;
import com.piliofpala.craftstudio.pangolin.domain.logistics.exception.LogisticsWarehouseConfigException;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.GatherPlatform;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.jst.modules.constants.Warehouse;
import com.piliofpala.craftstudio.shared.domain.logistics.vo.LogisticsCarrierPresetType;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class LogisticsTest {
    /**
     * 快递换仓
     * @throws IOException
     */
    @Test
    @Disabled("影响真实数据，打包不予执行")
    void configLogisticsWarehouse() throws IOException {
        try {
            GatherPlatform.getInstance(GatherPlatformType.JST).configLogisticsWarehouse(
                    "ZTO", Warehouse.MAIN_WAREHOUSE_ID,"542919"
            );
        } catch (LogisticsWarehouseConfigException e) {
            e.printStackTrace();
        }
    }

    /**
     * 打印快递面单
     */
    @Test
    @Disabled()
    public void printLogistics(){
        var logisticsLabel = GatherPlatform.getInstance(GatherPlatformType.JST).printLogisticsLabel(
                "542919","Printer name", true, LogisticsCarrierPresetType.ZTO
        );
    }
}
