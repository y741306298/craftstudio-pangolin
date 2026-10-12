package com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.jst.modules.simulator;

import com.alibaba.fastjson2.JSONObject;

public class AspDataPackageParser {
    public JSONObject parseToJson(String data){
        // 去除开头的0|
        data = data.replaceFirst("^\\d+\\|", "");
        var dataObj = JSONObject.parse(data);
        var returnValueStr = dataObj.getString("ReturnValue");
        if(returnValueStr!=null) {
            // 去除ReturnValue开头的L:
            returnValueStr = returnValueStr.replaceFirst("^L:", "");
            var returnValueObj = JSONObject.parse(returnValueStr);
            dataObj.put("ReturnValue",returnValueObj);//将ReturnValue替换为对象
        }
        return dataObj;
    }
}
