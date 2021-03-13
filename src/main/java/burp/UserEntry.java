package burp;

/*
** UserTable中每一行就是一个UserEntry对象
 */
public class UserEntry {
    private String name;
    private String cookies;
    private String header = "";

    public UserEntry() {
    }

    public UserEntry(String name, String cookies, String header) {
        this.name = name;
        this.cookies = cookies;
        this.header = header;
    }

    public UserEntry(String name) {
        this.name = name;
        this.cookies = "";
        this.header = "";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCookies() {
        return cookies;
    }

    public void setCookies(String cookies) {
        this.cookies = cookies;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }
}
