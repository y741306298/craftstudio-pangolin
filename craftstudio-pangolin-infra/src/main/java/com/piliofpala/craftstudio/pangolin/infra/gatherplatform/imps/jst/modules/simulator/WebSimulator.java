package com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.jst.modules.simulator;

import com.alibaba.fastjson2.JSONObject;
import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.auth.AuthException;
import com.piliofpala.craftstudio.pangolin.domain.logistics.exception.LogisticsLabelPrintException;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.jst.modules.constants.JstAccountConfig;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.utils.WebUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebSimulator {
    private static final System.Logger log = System.getLogger("craftstudio-pangolin");

    private static final String WWW_BASE = "https://www.erp321.com";
    private static final String API_BASE = "https://api.erp321.com";

    private static final String LOGIN_URL = API_BASE + "/erp/webapi/UserApi/WebLogin/Passport";

    private final CookieManager cookieManager;

    private final String account = "13349138430";
    private final String password = "1qaz@WSX3edc$RFV";

    private static WebSimulator instance;

    public static WebSimulator getLoggedInstance() {
        if(instance == null) {
            instance = new WebSimulator();
            var cookies = new CookieHolder().loadCookies();
            if(cookies == null) {
                instance.login();
            }else {
                log.log(System.Logger.Level.INFO,"存在本地cookie，直接使用，无需登录");
                cookies.forEach(cookie->{
                    instance.cookieManager.getCookieStore().add(
                            URI.create("https://" + cookie.getDomain() + (cookie.getPath() == null ? "/" : cookie.getPath())),
                            cookie
                    );
                });
            }
        }
        return instance;
    }

    private WebSimulator() {
        this.cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    }

    /**
     * 登录完成后，CookieManager 此时应该已经拿到 u_sso_token / u_id / u_co_id / ...
     */
    public void login() {
        log.log(System.Logger.Level.INFO,"模拟登录");
        get(WWW_BASE + "/", WWW_BASE);
        String loginBody =
                "{"
                        + "\"data\":{"
                        + "\"account\":\"" + jsonEscape(account) + "\","
                        + "\"password\":\"" + jsonEscape(password) + "\","
                        + "\"isApp\":false"
                        + "}"
                + "}";

        HttpResult loginResult = postJson(LOGIN_URL, loginBody, WWW_BASE);

        if (loginResult.statusCode() < 200 || loginResult.statusCode() >= 300) {
            throw new LogisticsLabelPrintException("登录失败 HTTP " + loginResult.statusCode() + "\n" + loginResult.body());
        }

        log.log(System.Logger.Level.INFO,"登录成功");
        //更新持久化cookie
        new CookieHolder().saveCookieFile(cookieManager);
        dumpCookies();
    }

    /**
     * 登出
     */
    public void logout() {
        log.log(System.Logger.Level.INFO,"登出");
        var apiUrl = API_BASE + "/usercenterapi/Passport/SignOff";
        var result = instance.postJson(
                apiUrl,
                "{\n" +
                        "    \"data\": {},\n" +
                        "    \"uid\": "+JstAccountConfig.UID+",\n" +
                        "    \"coid\": "+JstAccountConfig.COID+"\n" +
                        "}",
                apiUrl
        );
        log.log(System.Logger.Level.INFO,"登出结果："+result.body());
        new CookieHolder().cleanCookieFile();//清理cookie
        instance = null;//直接重置instance为null
    }

    /**
     * POST JSON
     */
    public HttpResult postJson(String url, String json, String cookieUri) {
        try {
            HttpURLConnection conn = openConnection(url);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            setCommonHeaders(conn, "application/json", cookieUri);
            byte[] body = json.getBytes(StandardCharsets.UTF_8);
            conn.setRequestProperty("Content-Length", String.valueOf(body.length));
            try (var output = conn.getOutputStream()) {
                output.write(body);
            }
            return readResponse(conn);
        } catch (IOException e) {
            throw new LogisticsLabelPrintException(e.getMessage());
        }
    }

    /**
     * POST application/x-www-form-urlencoded
     */
    public HttpResult postForm(String url, String form, String referer) {
        try{
            HttpURLConnection conn = openConnection(url);

            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            setCommonHeaders(conn, "application/x-www-form-urlencoded; charset=UTF-8", referer);

            conn.setRequestProperty("X-Requested-With", "XMLHttpRequest");

            byte[] body = form.getBytes(StandardCharsets.UTF_8);

            conn.setRequestProperty("Content-Length", String.valueOf(body.length));

            try (var output = conn.getOutputStream()) {
                output.write(body);
            }
            var result = readResponse(conn);
            new CookieHolder().saveCookieFile(cookieManager);//这里cookie可能变化，保存之
            return result;
        }catch (Exception e){
            e.printStackTrace();
            throw new LogisticsLabelPrintException(e.getMessage());
        }
    }

    /**
     * GET
     */
    public HttpResult get(String url, String referer) {

        try {
            HttpURLConnection conn = openConnection(url);
            conn.setRequestMethod("GET");
            setCommonHeaders(conn, null, referer);
            return readResponse(conn);
        } catch (IOException e) {
            throw new LogisticsLabelPrintException(e.getMessage());
        }
    }

    public ViewState fetchViewState(String url) {
        HttpResult expressSetterPage = get(url, WWW_BASE);
        ViewState viewState = new ViewState(
                extractInputValue(expressSetterPage.body(), "__VIEWSTATE"),
                extractInputValue(expressSetterPage.body(), "__VIEWSTATEGENERATOR")
        );
        log.log(System.Logger.Level.INFO, viewState.dumpInfo());
        return viewState;
    }


    /**
     * 聚水潭执行某些动作（比如订单审核后到生成出库单之间）后期望结果落地可能有延迟，这里提供统一等待
     * @throws InterruptedException
     */
    public static void waitStupid(int seconds) {
        try {
            Thread.sleep(seconds*1000L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 短信验证码获取sid授权
     * @param smsCode
     * @param sid
     * @throws AuthException
     */
    public void obtainAuthSid(String smsCode) throws AuthException {
        log.log(System.Logger.Level.INFO, "-------------------------------短信验证码做Sid授权-----------------------------");
        var sid = findCookieValue("_sid"+ JstAccountConfig.UID);
        if(sid == null) {
            throw new AuthException("请先获取sid到cookie中");
        }

        var apiUrl = WebSimulator.API_BASE + "/erp/webapi/SecurityApi/CheckSmsAuthCode";

        var authResult = WebSimulator.getLoggedInstance().postJson(apiUrl, "{\n" +
                "    \"data\": {\n" +
                "        \"smsCode\": "+smsCode+",\n" +
                "        \"action\": \"打单发货\",\n" +
                "        \"toAdmin\": false,\n" +
                "        \"sid\": \""+sid+"\"\n" +
                "    },\n" +
                "    \"ip\": \"\",\n" +
                "    \"coid\": \""+JstAccountConfig.COID+"\",\n" +
                "    \"uid\": \""+ JstAccountConfig.UID+"\"\n" +
                "}", apiUrl);
        if (authResult.statusCode() < 200 || authResult.statusCode() >= 300) {
            throw new AuthException("授权失败 " + authResult.statusCode() + "\n" + authResult.body());
        }
        var bodyObj = JSONObject.parse(authResult.body());
        if(bodyObj == null || !bodyObj.containsKey("code") || bodyObj.getIntValue("code")!=0) {
            throw new AuthException("授权失败 " + authResult.body());
        }
        log.log(System.Logger.Level.INFO,"授权成功");
    }

    /**
     * 打开连接。
     *
     * HttpURLConnection 会通过 JVM 全局 CookieHandler
     * 自动读取/保存 Cookie。
     */
    private HttpURLConnection openConnection(String url) throws IOException {
        /*
         * CookieManager 必须挂到 JVM CookieHandler。
         *
         * 因为 HttpURLConnection 本身不会主动维护
         * 浏览器式的 Cookie Session。
         */
        CookieHandlerHolder.ensureInstalled(cookieManager);
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);
        conn.setUseCaches(false);
        return conn;
    }

    /**
     * 公共请求头。
     *
     * 抓包中实际存在的关键 Header：
     *
     * Accept
     * Accept-Language
     * Cache-Control
     * Origin
     * Referer
     * User-Agent
     */
    private void setCommonHeaders(HttpURLConnection conn, String contentType, String referer) {

        conn.setRequestProperty("Accept", "application/json, text/javascript, */*; q=0.01");

        conn.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9");

        conn.setRequestProperty("Cache-Control", "no-cache");

        conn.setRequestProperty("Pragma", "no-cache");

        conn.setRequestProperty("Origin", WWW_BASE);

        if (referer != null) {
            conn.setRequestProperty("Referer", referer);
        }

        conn.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                        + "AppleWebKit/537.36 "
                        + "(KHTML, like Gecko) "
                        + "Chrome/152.0.0.0 "
                        + "Safari/537.36"
        );

        if (contentType != null) {
            conn.setRequestProperty("Content-Type", contentType);
        }
    }


    /**
     * 从 HTML 中提取：
     *
     * <input ... name="__VIEWSTATE" value="xxx">
     *
     * ASP.NET WebForms 页面使用。
     */
    private String extractInputValue(String html, String name) {

        Pattern pattern = Pattern.compile(
                "<input[^>]*"
                        + "name=[\"']"
                        + Pattern.quote(name)
                        + "[\"'][^>]*"
                        + "value=[\"']([^\"']*)[\"']",
                Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return htmlUnescape(matcher.group(1));
        }
        /*
         * 某些页面 input 的属性顺序可能反过来。
         */
        Pattern reversePattern = Pattern.compile(
                "<input[^>]*"
                        + "value=[\"']([^\"']*)[\"'][^>]*"
                        + "name=[\"']"
                        + Pattern.quote(name)
                        + "[\"']",
                Pattern.CASE_INSENSITIVE
        );
        matcher = reversePattern.matcher(html);

        if (matcher.find()) {
            return htmlUnescape(matcher.group(1));
        }
        return null;
    }

    private String htmlUnescape(String value) {
        return value
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">");
    }


    private static String jsonEscape(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * 读取 HTTP Response。
     */
    private HttpResult readResponse(HttpURLConnection conn) throws IOException {
        int statusCode = conn.getResponseCode();
        InputStream inputStream;
        if (statusCode >= 400) {
            inputStream = conn.getErrorStream();
        } else {
            inputStream = conn.getInputStream();
        }
        String body = "";
        if (inputStream != null) {
            body = readString(inputStream);
        }
        return new HttpResult(statusCode, body, conn.getHeaderFields());
    }

    private String readString(InputStream inputStream) throws IOException {
        try (var in = inputStream; var reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[8192];
            int len;
            while ((len = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, len);
            }
            return sb.toString();
        }
    }

    /**
     * 打印请求后的 Cookie 状态。
     *
     * 调试时非常有用。
     */
    public void dumpCookies() {
        StringBuilder sb = new StringBuilder();
        sb.append("========== COOKIES ==========\n");
        cookieManager.getCookieStore().getCookies().forEach(cookie ->
                sb.append(cookie.getName())
                        .append("=").append(cookie.getValue())
                        .append("; domain=").append(cookie.getDomain())
                        .append("; path=").append(cookie.getPath())
                        .append("\n")
        );
        sb.append("=============================");

        log.log(System.Logger.Level.INFO,sb.toString());
    }

    /**
     * CookieHandler 安装器。
     *
     * CookieHandler.setDefault() 是 JVM 全局设置，
     * 防止重复安装。
     */
    private static class CookieHandlerHolder {

        private static boolean installed = false;

        private static synchronized void ensureInstalled(CookieManager cookieManager) {
            if (!installed) {
                java.net.CookieHandler.setDefault(cookieManager);
                installed = true;
            }
        }
    }

    private String findCookieValue(String name){
        for (HttpCookie cookie : cookieManager.getCookieStore().getCookies()) {
            if(cookie.getName().equals(name)) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
