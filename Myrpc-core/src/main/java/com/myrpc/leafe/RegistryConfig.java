package com.myrpc.leafe;

public class RegistryConfig {
    private String registryAddress;
    private String registrytype;
    public RegistryConfig(String registrytype,String registryAddress)
    {
        this.registryAddress = registryAddress;
        this.registrytype = registrytype;
    }

    public String getRegistryAddress() {
        return registryAddress;
    }

    public void setRegistryAddress(String registryAddress) {
        this.registryAddress = registryAddress;
    }

    public String getRegistrytype() {
        return registrytype;
    }

    public void setRegistrytype(String registrytype) {
        this.registrytype = registrytype;
    }
}
