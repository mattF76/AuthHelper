package burp;

/*
** OriginalRequestTable中每一行对应的对象
 */
public class ORequestEntry {
    private IHttpRequestResponse iHttpRequestResponse;
    private String method;
    private String URL;

    public ORequestEntry(IHttpRequestResponse iHttpRequestResponse, String method, String URL) {
        this.iHttpRequestResponse = iHttpRequestResponse;
        this.method = method;
        this.URL = URL;
    }

    public IHttpRequestResponse getiHttpRequestResponse() {
        return iHttpRequestResponse;
    }

    public void setiHttpRequestResponse(IHttpRequestResponse iHttpRequestResponse) {
        this.iHttpRequestResponse = iHttpRequestResponse;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getURL() {
        return URL;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }
}
