package com.myrpc.leafe.LoadBalancer;

import java.net.InetSocketAddress;
import java.util.List;

public interface UpdatableSelector extends Selector{
    void updateAddresses(List<InetSocketAddress> addresses);
}
