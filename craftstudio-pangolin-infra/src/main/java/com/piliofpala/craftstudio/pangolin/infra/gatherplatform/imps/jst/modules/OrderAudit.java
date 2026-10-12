package com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.jst.modules;

import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.jst.modules.simulator.HttpDataBuilder;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.jst.modules.simulator.JSTUrl;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.jst.modules.simulator.WebSimulator;

import java.util.Map;

public class OrderAudit {
    private static final System.Logger log = System.getLogger("craftstudio-pangolin");

    public void auditOrder(String oId) {
        log.log(System.Logger.Level.INFO,"------------------------------订单审核------------------------------");
        var apiUrl = JSTUrl.ERP321 + "/app/order/order/list.aspx?_c=jst-epaas&epaas=true&am___=ConfirmCheckOids&ts___="+System.currentTimeMillis();
        var webSimulator = WebSimulator.getLoggedInstance();
        var viewState = webSimulator.fetchViewState(apiUrl);
        var form = HttpDataBuilder.buildForm(Map.of(
                "__VIEWSTATE", viewState.getViewState(),
                "__VIEWSTATEGENERATOR", viewState.getViewStateGenerator(),
                "__CALLBACKID", "JTable1",
                "__CALLBACKPARAM", "{\"Method\":\"ConfirmCheckOids\",\"Args\":[\""+oId+"\"],\"CallControl\":\"{page}\"}"
        ));
        var expressSetterResult = webSimulator.postForm(apiUrl, form, apiUrl);
        log.log(System.Logger.Level.INFO,expressSetterResult.body());
    }
}
