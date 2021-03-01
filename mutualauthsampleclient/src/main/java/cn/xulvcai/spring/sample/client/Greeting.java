package cn.xulvcai.spring.sample.client;

public class Greeting {

    private long id;
    private String content;

    public long getId() {
        return id;
    }

    public Greeting setId(long id) {
        this.id = id;
        return this;
    }

    public String getContent() {
        return content;
    }

    public Greeting setContent(String content) {
        this.content = content;
        return this;
    }
}
