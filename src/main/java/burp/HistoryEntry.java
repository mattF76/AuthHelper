package burp;

import java.net.URL;

public class HistoryEntry {
    private IHttpRequestResponsePersisted requestResponse;  // 因为请求非常多，所以需要保存到文件中去
    private URL url;
    private String method;
    private String user;        // 请求中Cookie对应的用户
    private int httpCode;
    private int httpSize;

    public HistoryEntry(IHttpRequestResponsePersisted requestResponse,
                         URL url, String method, String user,int httpCode, int httpSize) {
        this.requestResponse = requestResponse;
        this.user = user;
        this.url = url;
        this.method = method;
        this.httpCode = httpCode;
        this.httpSize = httpSize;
    }

    public IHttpRequestResponsePersisted getRequestResponse() {
        return requestResponse;
    }

    public URL getUrl() {
        return url;
    }

    public String getMethod() {
        return method;
    }

    public String getUser() {
        return user;
    }

    public int getHttpCode() {
        return httpCode;
    }

    public int getHttpSize() {
        return httpSize;
    }
}
