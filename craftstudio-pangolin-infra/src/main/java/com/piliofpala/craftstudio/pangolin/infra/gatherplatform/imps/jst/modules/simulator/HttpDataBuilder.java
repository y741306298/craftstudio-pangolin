package com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.jst.modules.simulator;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class HttpDataBuilder {
    public static String buildForm(Map<String, String> form) {
        if(form == null || form.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        form.forEach((key,value)->{
            if (!sb.isEmpty()) {
                sb.append('&');
            }
            sb.append(URLEncoder.encode(key, StandardCharsets.UTF_8));
            sb.append('=');
            sb.append(URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8));
        });
        return sb.toString();
    }
}
