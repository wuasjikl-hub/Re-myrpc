package com.myrpc.leafe.res;

import io.netty.channel.Channel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.InetSocketAddress;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HeartBeatResult {
        InetSocketAddress address;
        Channel channel;
        boolean success;
        long responseTime;

        public HeartBeatResult(InetSocketAddress address, boolean success, long responseTime) {
                this.address = address;
                this.success = success;
                this.responseTime = responseTime;
        }
}
