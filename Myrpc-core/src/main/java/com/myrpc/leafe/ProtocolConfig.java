package com.myrpc.leafe;

public class ProtocolConfig {
    private String protocol;
    private String host;
    private int port;
    public ProtocolConfig(String protocol,String host,int port)
    {
        this.protocol = protocol;
        this.host = host;
        this.port = port;
    }
    public ProtocolConfig(String protocol)
    {
        this.protocol = protocol;
    }
    public String getProtocol()
    {
        return protocol;
    }
}
