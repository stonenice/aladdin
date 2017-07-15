
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by stonenice on 2017/7/15.
 *
 * @author stonenice
 */

@Slf4j
@Component("httpclient")
public class HttpClitentHTTPFacade extends HTTPFacade {
    private static final int MAX_CONNECTIONS = 50;
    private static final String INFOS_CONNECT_TIMEOUT_KEY = "connectTimeout";
    private static final int DEFAULT_CONNECT_TIMEOUT = 3000;

    private static final int SOCKET_CONNECT_TIMEOUT_RATE = 5;

    private final static String DEFAULT_MINE_TYPE = "text/html";
    private final static String DEFAULT_CHARSET = "UTF-8";

    private HttpClient client;

    private Map<String, Object> headers;


    public HttpClitentHTTPFacade() {
        try {
            SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
                @Override
                public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    return true;
                }
            }).build();
            SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContext);
            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.INSTANCE).register("https", sslConnectionSocketFactory)
                    .build();
            PoolingHttpClientConnectionManager httpConnectionManager = new PoolingHttpClientConnectionManager(
                    socketFactoryRegistry);
            httpConnectionManager.setDefaultMaxPerRoute(MAX_CONNECTIONS);
            httpConnectionManager.setMaxTotal(MAX_CONNECTIONS * 2);
            client = HttpClients.custom().setSSLSocketFactory(sslConnectionSocketFactory).setConnectionManager(
                    httpConnectionManager)
                    .build();
        } catch (Exception e) {
            log.error("Init http client error!", e);
        }

    }

    @Override
    public void sessionStart() {

    }

    @Override
    public void sessionDestory() {

    }

    @Override
    public Object getCookie(String key) {
        return null;
    }

    @Override
    public Object getSession(String key) {
        return null;
    }

    @Override
    public void setHeaders(Map<String, Object> headers) {
        this.headers = headers;
    }

    @Override
    public HTTPResult get(String url) {
        HttpGet method = new HttpGet(url);
        HTTPResult result;
        long startTime = System.currentTimeMillis();
        long endTime = 0;
        try {
            Map<String, String> infos = splitExtenstionInfosFromHeaders(headers);
            int connectTimeout = DEFAULT_CONNECT_TIMEOUT;
            if (infos != null) {
                String ctstr = infos.containsKey(INFOS_CONNECT_TIMEOUT_KEY) ? infos.get(INFOS_CONNECT_TIMEOUT_KEY) : "-1";
                try {
                    int tmp = Integer.parseInt(ctstr);
                    connectTimeout = tmp > 0 ? tmp : DEFAULT_CONNECT_TIMEOUT;
                } catch (Exception e) {
                }
            }
            int socketTimeout = connectTimeout * SOCKET_CONNECT_TIMEOUT_RATE;
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectionRequestTimeout(connectTimeout)
                    .setSocketTimeout(socketTimeout)
                    .build();
            method.setConfig(requestConfig);
            String charset = DEFAULT_CHARSET;
            if (headers != null && headers.size() > 0) {
                for (Map.Entry<String, Object> header : headers.entrySet()) {
                    String key = header.getKey();
                    if (StringUtils.isBlank(key)) continue;
                    Object value = header.getValue();
                    String val = value != null ? value.toString().trim() : "";
                    method.setHeader(key, val);

                    if ("content-type".equals(key.toLowerCase().trim())) {
                        int pos = val.indexOf("=");
                        charset = (pos > 0 && pos + 1 < val.length()) ? val.substring(pos + 1).trim() : DEFAULT_CHARSET;
                    }
                }
            }

            HttpResponse response = client.execute(method);
            result = HTTPResult.newSuccessResult();
            result.setStatusCode(response.getStatusLine().getStatusCode());
            HttpEntity entity = response.getEntity();
            String body = entity != null ? EntityUtils.toString(entity, charset) : "";
            result.setBody(body);
            endTime = System.currentTimeMillis();
            result.setResponseTime(endTime - startTime);
            return result;
        } catch (Exception e) {
            result = HTTPResult.newErrorResult();
            result.setBody(e.getMessage());
            endTime = System.currentTimeMillis();
            result.setResponseTime(endTime - startTime);
            return result;
        } finally {
            method.releaseConnection();
        }

    }

    @Override
    public HTTPResult get(String url, Map<String, Object> params) {

        long startTime = System.currentTimeMillis();
        long endTime = 0;

        String paramLine = null;
        JString jstr = JString.valueOf(url);
        List<String> holders = jstr.placeholders();

        if (params != null && params.size() > 0) {
            Map<String, Object> newParams = Maps.newHashMap();
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String key = entry.getKey();
                key = key.trim();
                Object value = entry.getValue();
                String val = value != null ? value.toString() : null;
                if (holders != null && holders.size() > 0) {
                    if (!CollectionUtils.contains(holders.iterator(), key)) {
                        sb.append(key);
                        sb.append("=");
                        sb.append(val);
                        sb.append("&");
                    }
                } else {
                    sb.append(key);
                    sb.append("=");
                    sb.append(val);
                    sb.append("&");
                }
            }
            paramLine = StringUtils.stripEnd(sb.toString(), "&");
        }
        String urlline = jstr.toString(params);
        urlline = StringUtils.stripEnd(urlline, "?");
        urlline = StringUtils.isNotBlank(paramLine) ? urlline + "?" + paramLine : urlline;

        HttpGet method = new HttpGet(urlline);

        HTTPResult result;
        try {
            Map<String, String> infos = splitExtenstionInfosFromHeaders(headers);
            int connectTimeout = DEFAULT_CONNECT_TIMEOUT;
            if (infos != null) {
                String ctstr = infos.containsKey(INFOS_CONNECT_TIMEOUT_KEY) ? infos.get(INFOS_CONNECT_TIMEOUT_KEY) : "-1";
                try {
                    int tmp = Integer.parseInt(ctstr);
                    connectTimeout = tmp > 0 ? tmp : DEFAULT_CONNECT_TIMEOUT;
                } catch (Exception e) {
                }
            }
            int socketTimeout = connectTimeout * SOCKET_CONNECT_TIMEOUT_RATE;
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectionRequestTimeout(connectTimeout)
                    .setSocketTimeout(socketTimeout)
                    .build();
            method.setConfig(requestConfig);
            String charset = DEFAULT_CHARSET;
            if (headers != null && headers.size() > 0) {
                for (Map.Entry<String, Object> header : headers.entrySet()) {
                    String key = header.getKey();
                    if (StringUtils.isBlank(key)) continue;
                    Object value = header.getValue();
                    String val = value != null ? value.toString().trim() : "";
                    method.setHeader(key, val);

                    if ("content-type".equals(key.toLowerCase().trim())) {
                        int pos = val.indexOf("=");
                        charset = (pos > 0 && pos + 1 < val.length()) ? val.substring(pos + 1).trim() : DEFAULT_CHARSET;
                    }
                }
            }

            HttpResponse response = client.execute(method);
            result = HTTPResult.newSuccessResult();
            result.setStatusCode(response.getStatusLine().getStatusCode());
            HttpEntity entity = response.getEntity();
            String body = entity != null ? EntityUtils.toString(entity, charset) : "";
            result.setBody(body);
            endTime = System.currentTimeMillis();
            result.setResponseTime(endTime - startTime);
            return result;
        } catch (Exception e) {
            result = HTTPResult.newErrorResult();
            result.setBody(e.getMessage());
            endTime = System.currentTimeMillis();
            result.setResponseTime(endTime - startTime);
            return result;
        } finally {
            method.releaseConnection();
        }

    }

    @Override
    public HTTPResult post(String url, String rawRes) {
        HttpPost method = new HttpPost(url);
        HTTPResult result;

        long startTime = System.currentTimeMillis();
        long endTime = 0;

        try {
            Map<String, String> infos = splitExtenstionInfosFromHeaders(headers);
            int connectTimeout = DEFAULT_CONNECT_TIMEOUT;
            if (infos != null) {
                String ctstr = infos.containsKey(INFOS_CONNECT_TIMEOUT_KEY) ? infos.get(INFOS_CONNECT_TIMEOUT_KEY) : "-1";
                try {
                    int tmp = Integer.parseInt(ctstr);
                    connectTimeout = tmp > 0 ? tmp : DEFAULT_CONNECT_TIMEOUT;
                } catch (Exception e) {
                }
            }

            int socketTimeout = connectTimeout * SOCKET_CONNECT_TIMEOUT_RATE;
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectionRequestTimeout(connectTimeout)
                    .setSocketTimeout(socketTimeout)
                    .build();
            method.setConfig(requestConfig);

            String charset = DEFAULT_CHARSET;
            String mineType = DEFAULT_MINE_TYPE;
            if (headers != null && headers.size() > 0) {
                for (Map.Entry<String, Object> header : headers.entrySet()) {
                    String key = header.getKey();
                    if (StringUtils.isBlank(key)) continue;
                    Object value = header.getValue();
                    String val = value != null ? value.toString().trim() : "";
                    method.setHeader(key, val);

                    if ("content-type".equals(key.toLowerCase().trim())) {
                        int pos = val.indexOf("=");
                        charset = (pos > 0 && pos + 1 < val.length()) ? val.substring(pos + 1).trim() : DEFAULT_CHARSET;
                        int mpos = val.indexOf(";");
                        mineType = mpos >= 0 ? val.substring(0, mpos).trim() : DEFAULT_MINE_TYPE;
                    }
                }
            }

            StringEntity stringEntity = new StringEntity(rawRes, ContentType.create(mineType, charset));
            method.setEntity(stringEntity);
            HttpResponse response = client.execute(method);
            result = HTTPResult.newSuccessResult();
            result.setStatusCode(response.getStatusLine().getStatusCode());
            HttpEntity entity = response.getEntity();
            String body = entity != null ? EntityUtils.toString(entity, charset) : "";
            result.setBody(body);
            endTime = System.currentTimeMillis();
            result.setResponseTime(endTime - startTime);
            return result;
        } catch (Exception e) {
            result = HTTPResult.newErrorResult();
            result.setBody(e.getMessage());
            endTime = System.currentTimeMillis();
            result.setResponseTime(endTime - startTime);
            return result;
        } finally {
            method.releaseConnection();
        }
    }

    @Override
    public HTTPResult post(String url, Map<String, Object> params) {

        long startTime = System.currentTimeMillis();
        long endTime = 0;

        JString jstr = JString.valueOf(url);
        String newurl = jstr.toString(params);
        List<String> holders = jstr.placeholders();

        List<NameValuePair> nvps = Lists.newArrayList();
        if (params != null && params.size() > 0) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String key = entry.getKey();
                if (StringUtils.isBlank(key)) continue;
                key = key.trim();

                if (CollectionUtils.contains(holders.iterator(), key)) continue;

                Object value = entry.getValue();
                nvps.add(new BasicNameValuePair(key, value != null ? value.toString() : ""));

            }
        }

        HttpPost method = new HttpPost(newurl);
        HTTPResult result;
        try {
            Map<String, String> infos = splitExtenstionInfosFromHeaders(headers);
            int connectTimeout = DEFAULT_CONNECT_TIMEOUT;
            if (infos != null) {
                String ctstr = infos.containsKey(INFOS_CONNECT_TIMEOUT_KEY) ? infos.get(INFOS_CONNECT_TIMEOUT_KEY) : "-1";
                try {
                    int tmp = Integer.parseInt(ctstr);
                    connectTimeout = tmp > 0 ? tmp : DEFAULT_CONNECT_TIMEOUT;
                } catch (Exception e) {
                }
            }

            int socketTimeout = connectTimeout * SOCKET_CONNECT_TIMEOUT_RATE;
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectionRequestTimeout(connectTimeout)
                    .setSocketTimeout(socketTimeout)
                    .build();
            method.setConfig(requestConfig);

            String charset = DEFAULT_CHARSET;
            String mineType = DEFAULT_MINE_TYPE;
            if (headers != null && headers.size() > 0) {
                for (Map.Entry<String, Object> header : headers.entrySet()) {
                    String key = header.getKey();
                    if (StringUtils.isBlank(key)) continue;
                    Object value = header.getValue();
                    String val = value != null ? value.toString().trim() : "";
                    method.setHeader(key, val);

                    if ("content-type".equals(key.toLowerCase().trim())) {
                        int pos = val.indexOf("=");
                        charset = (pos > 0 && pos + 1 < val.length()) ? val.substring(pos + 1).trim() : DEFAULT_CHARSET;
                        int mpos = val.indexOf(";");
                        mineType = mpos >= 0 ? val.substring(0, mpos).trim() : DEFAULT_MINE_TYPE;
                    }
                }
            }

            if (nvps != null && nvps.size() > 0) {
                method.setEntity(new UrlEncodedFormEntity(nvps, charset));
            }

            HttpResponse response = client.execute(method);
            result = HTTPResult.newSuccessResult();
            result.setStatusCode(response.getStatusLine().getStatusCode());
            HttpEntity entity = response.getEntity();
            String body = entity != null ? EntityUtils.toString(entity, charset) : "";
            result.setBody(body);
            endTime = System.currentTimeMillis();
            result.setResponseTime(endTime - startTime);
            return result;
        } catch (Exception e) {
            result = HTTPResult.newErrorResult();
            result.setBody(e.getMessage());
            endTime = System.currentTimeMillis();
            result.setResponseTime(endTime - startTime);
            return result;
        } finally {
            method.releaseConnection();
        }
    }

    @Override
    public HTTPResult put(String url, String rawRes) {

        long startTime = System.currentTimeMillis();
        long endTime = 0;

        HttpPut method = new HttpPut(url);
        HTTPResult result;
        try {
            Map<String, String> infos = splitExtenstionInfosFromHeaders(headers);
            int connectTimeout = DEFAULT_CONNECT_TIMEOUT;
            if (infos != null) {
                String ctstr = infos.containsKey(INFOS_CONNECT_TIMEOUT_KEY) ? infos.get(INFOS_CONNECT_TIMEOUT_KEY) : "-1";
                try {
                    int tmp = Integer.parseInt(ctstr);
                    connectTimeout = tmp > 0 ? tmp : DEFAULT_CONNECT_TIMEOUT;
                } catch (Exception e) {
                }
            }

            int socketTimeout = connectTimeout * SOCKET_CONNECT_TIMEOUT_RATE;
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectionRequestTimeout(connectTimeout)
                    .setSocketTimeout(socketTimeout)
                    .build();
            method.setConfig(requestConfig);

            String charset = DEFAULT_CHARSET;
            String mineType = DEFAULT_MINE_TYPE;
            if (headers != null && headers.size() > 0) {
                for (Map.Entry<String, Object> header : headers.entrySet()) {
                    String key = header.getKey();
                    if (StringUtils.isBlank(key)) continue;
                    Object value = header.getValue();
                    String val = value != null ? value.toString().trim() : "";
                    method.setHeader(key, val);

                    if ("content-type".equals(key.toLowerCase().trim())) {
                        int pos = val.indexOf("=");
                        charset = (pos > 0 && pos + 1 < val.length()) ? val.substring(pos + 1).trim() : DEFAULT_CHARSET;
                        int mpos = val.indexOf(";");
                        mineType = mpos >= 0 ? val.substring(0, mpos).trim() : DEFAULT_MINE_TYPE;
                    }
                }
            }

            StringEntity stringEntity = new StringEntity(rawRes, ContentType.create(mineType, charset));
            method.setEntity(stringEntity);
            HttpResponse response = client.execute(method);
            result = HTTPResult.newSuccessResult();
            result.setStatusCode(response.getStatusLine().getStatusCode());
            HttpEntity entity = response.getEntity();
            String body = entity != null ? EntityUtils.toString(entity, charset) : "";
            result.setBody(body);
            endTime = System.currentTimeMillis();
            result.setResponseTime(endTime - startTime);
            return result;
        } catch (Exception e) {
            result = HTTPResult.newErrorResult();
            result.setBody(e.getMessage());
            endTime = System.currentTimeMillis();
            result.setResponseTime(endTime - startTime);
            return result;
        } finally {
            method.releaseConnection();
        }

    }

    @Override
    public HTTPResult put(String url, Map<String, Object> params) {
        long startTime = System.currentTimeMillis();
        long endTime = 0;

        JString jstr = JString.valueOf(url);
        String newurl = jstr.toString(params);
        List<String> holders = jstr.placeholders();

        List<NameValuePair> nvps = Lists.newArrayList();
        if (params != null && params.size() > 0) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String key = entry.getKey();
                if (StringUtils.isBlank(key)) continue;
                key = key.trim();

                if (CollectionUtils.contains(holders.iterator(), key)) continue;

                Object value = entry.getValue();
                nvps.add(new BasicNameValuePair(key, value != null ? value.toString() : ""));

            }
        }

        HttpPut method = new HttpPut(newurl);
        HTTPResult result;
        try {
            Map<String, String> infos = splitExtenstionInfosFromHeaders(headers);
            int connectTimeout = DEFAULT_CONNECT_TIMEOUT;
            if (infos != null) {
                String ctstr = infos.containsKey(INFOS_CONNECT_TIMEOUT_KEY) ? infos.get(INFOS_CONNECT_TIMEOUT_KEY) : "-1";
                try {
                    int tmp = Integer.parseInt(ctstr);
                    connectTimeout = tmp > 0 ? tmp : DEFAULT_CONNECT_TIMEOUT;
                } catch (Exception e) {
                }
            }

            int socketTimeout = connectTimeout * SOCKET_CONNECT_TIMEOUT_RATE;
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectionRequestTimeout(connectTimeout)
                    .setSocketTimeout(socketTimeout)
                    .build();
            method.setConfig(requestConfig);

            String charset = DEFAULT_CHARSET;
            String mineType = DEFAULT_MINE_TYPE;
            if (headers != null && headers.size() > 0) {
                for (Map.Entry<String, Object> header : headers.entrySet()) {
                    String key = header.getKey();
                    if (StringUtils.isBlank(key)) continue;
                    Object value = header.getValue();
                    String val = value != null ? value.toString().trim() : "";
                    method.setHeader(key, val);

                    if ("content-type".equals(key.toLowerCase().trim())) {
                        int pos = val.indexOf("=");
                        charset = (pos > 0 && pos + 1 < val.length()) ? val.substring(pos + 1).trim() : DEFAULT_CHARSET;
                        int mpos = val.indexOf(";");
                        mineType = mpos >= 0 ? val.substring(0, mpos).trim() : DEFAULT_MINE_TYPE;
                    }
                }
            }

            if (nvps != null && nvps.size() > 0) {
                method.setEntity(new UrlEncodedFormEntity(nvps, charset));
            }

            HttpResponse response = client.execute(method);
            result = HTTPResult.newSuccessResult();
            result.setStatusCode(response.getStatusLine().getStatusCode());
            HttpEntity entity = response.getEntity();
            String body = entity != null ? EntityUtils.toString(entity, charset) : "";
            result.setBody(body);
            endTime = System.currentTimeMillis();
            result.setResponseTime(endTime - startTime);
            return result;
        } catch (Exception e) {
            result = HTTPResult.newErrorResult();
            result.setBody(e.getMessage());
            endTime = System.currentTimeMillis();
            result.setResponseTime(endTime - startTime);
            return result;
        } finally {
            method.releaseConnection();
        }
    }

    @Override
    public HTTPResult delete(String url) {
        return delete(url, new HashMap<String, Object>());
    }

    @Override
    public HTTPResult delete(String url, String rawRes) {
        long startTime = System.currentTimeMillis();
        long endTime = 0;

        HttpDeleteWithBody method = new HttpDeleteWithBody(url);
        HTTPResult result;
        try {
            Map<String, String> infos = splitExtenstionInfosFromHeaders(headers);
            int connectTimeout = DEFAULT_CONNECT_TIMEOUT;
            if (infos != null) {
                String ctstr = infos.containsKey(INFOS_CONNECT_TIMEOUT_KEY) ? infos.get(INFOS_CONNECT_TIMEOUT_KEY) : "-1";
                try {
                    int tmp = Integer.parseInt(ctstr);
                    connectTimeout = tmp > 0 ? tmp : DEFAULT_CONNECT_TIMEOUT;
                } catch (Exception e) {
                }
            }

            int socketTimeout = connectTimeout * SOCKET_CONNECT_TIMEOUT_RATE;
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectionRequestTimeout(connectTimeout)
                    .setSocketTimeout(socketTimeout)
                    .build();
            method.setConfig(requestConfig);

            String charset = DEFAULT_CHARSET;
            String mineType = DEFAULT_MINE_TYPE;
            if (headers != null && headers.size() > 0) {
                for (Map.Entry<String, Object> header : headers.entrySet()) {
                    String key = header.getKey();
                    if (StringUtils.isBlank(key)) continue;
                    Object value = header.getValue();
                    String val = value != null ? value.toString().trim() : "";
                    method.setHeader(key, val);

                    if ("content-type".equals(key.toLowerCase().trim())) {
                        int pos = val.indexOf("=");
                        charset = (pos > 0 && pos + 1 < val.length()) ? val.substring(pos + 1).trim() : DEFAULT_CHARSET;
                        int mpos = val.indexOf(";");
                        mineType = mpos >= 0 ? val.substring(0, mpos).trim() : DEFAULT_MINE_TYPE;
                    }
                }
            }

            StringEntity stringEntity = new StringEntity(rawRes, ContentType.create(mineType, charset));
            method.setEntity(stringEntity);
            HttpResponse response = client.execute(method);
            result = HTTPResult.newSuccessResult();
            result.setStatusCode(response.getStatusLine().getStatusCode());
            HttpEntity entity = response.getEntity();
            String body = entity != null ? EntityUtils.toString(entity, charset) : "";
            result.setBody(body);
            endTime = System.currentTimeMillis();
            result.setResponseTime(endTime - startTime);
            return result;
        } catch (Exception e) {
            result = HTTPResult.newErrorResult();
            result.setBody(e.getMessage());
            endTime = System.currentTimeMillis();
            result.setResponseTime(endTime - startTime);
            return result;
        } finally {
            method.releaseConnection();
        }
    }

    @Override
    public HTTPResult delete(String url, Map<String, Object> params) {
        long startTime = System.currentTimeMillis();
        long endTime = 0;

        JString jstr = JString.valueOf(url);
        String newurl = jstr.toString(params);
        List<String> holders = jstr.placeholders();

        List<NameValuePair> nvps = Lists.newArrayList();
        if (params != null && params.size() > 0) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String key = entry.getKey();
                if (StringUtils.isBlank(key)) continue;
                key = key.trim();

                if (CollectionUtils.contains(holders.iterator(), key)) continue;

                Object value = entry.getValue();
                nvps.add(new BasicNameValuePair(key, value != null ? value.toString() : ""));

            }
        }

        HttpDeleteWithBody method = new HttpDeleteWithBody(newurl);
        HTTPResult result;
        try {
            Map<String, String> infos = splitExtenstionInfosFromHeaders(headers);
            int connectTimeout = DEFAULT_CONNECT_TIMEOUT;
            if (infos != null) {
                String ctstr = infos.containsKey(INFOS_CONNECT_TIMEOUT_KEY) ? infos.get(INFOS_CONNECT_TIMEOUT_KEY) : "-1";
                try {
                    int tmp = Integer.parseInt(ctstr);
                    connectTimeout = tmp > 0 ? tmp : DEFAULT_CONNECT_TIMEOUT;
                } catch (Exception e) {
                }
            }

            int socketTimeout = connectTimeout * SOCKET_CONNECT_TIMEOUT_RATE;
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectionRequestTimeout(connectTimeout)
                    .setSocketTimeout(socketTimeout)
                    .build();
            method.setConfig(requestConfig);

            String charset = DEFAULT_CHARSET;
            String mineType = DEFAULT_MINE_TYPE;
            if (headers != null && headers.size() > 0) {
                for (Map.Entry<String, Object> header : headers.entrySet()) {
                    String key = header.getKey();
                    if (StringUtils.isBlank(key)) continue;
                    Object value = header.getValue();
                    String val = value != null ? value.toString().trim() : "";
                    method.setHeader(key, val);

                    if ("content-type".equals(key.toLowerCase().trim())) {
                        int pos = val.indexOf("=");
                        charset = (pos > 0 && pos + 1 < val.length()) ? val.substring(pos + 1).trim() : DEFAULT_CHARSET;
                        int mpos = val.indexOf(";");
                        mineType = mpos >= 0 ? val.substring(0, mpos).trim() : DEFAULT_MINE_TYPE;
                    }
                }
            }

            if (nvps != null && nvps.size() > 0) {
                method.setEntity(new UrlEncodedFormEntity(nvps, charset));
            }

            HttpResponse response = client.execute(method);
            result = HTTPResult.newSuccessResult();
            result.setStatusCode(response.getStatusLine().getStatusCode());
            HttpEntity entity = response.getEntity();
            String body = entity != null ? EntityUtils.toString(entity, charset) : "";
            result.setBody(body);
            endTime = System.currentTimeMillis();
            result.setResponseTime(endTime - startTime);
            return result;
        } catch (Exception e) {
            result = HTTPResult.newErrorResult();
            result.setBody(e.getMessage());
            endTime = System.currentTimeMillis();
            result.setResponseTime(endTime - startTime);
            return result;
        } finally {
            method.releaseConnection();
        }
    }

    private static class HttpDeleteWithBody extends HttpEntityEnclosingRequestBase {
        private static final String METHOD_NAME = "DELETE";

        @Override
        public String getMethod() {
            return METHOD_NAME;
        }

        public HttpDeleteWithBody(final String uri) {
            super();
            setURI(URI.create(uri));
        }

        public HttpDeleteWithBody(final URI uri) {
            super();
            setURI(uri);
        }

        public HttpDeleteWithBody() {
            super();
        }
    }
}
