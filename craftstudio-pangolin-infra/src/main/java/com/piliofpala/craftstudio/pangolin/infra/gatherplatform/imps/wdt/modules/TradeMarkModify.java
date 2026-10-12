package com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.wdt.modules;

import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.wdt.client.WdtClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TradeMarkModify {
    public void modify(WdtClient client, String tradeNo, String remark) {
        Map<String, String> params = new HashMap<String, String>();

        params.put("trade_ids", tradeNo);
        params.put("remark_flag", "1");
        params.put("type", "2");
        params.put("remark", remark);
        try {
            String response = client.execute("sales_trade_batch_remark_flag.php", params);
            System.out.println(response);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }
}
