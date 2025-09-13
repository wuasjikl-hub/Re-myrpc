package com.myrpc.leafe.Handlers.client;

import com.myrpc.leafe.bootatrap.MyRpcBootstrap;
import com.myrpc.leafe.packet.heartBeat.heartBeatPacket;
import com.myrpc.leafe.res.HeartBeatResult;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
@ChannelHandler.Sharable
public class HeartBeatClientHandler extends SimpleChannelInboundHandler<heartBeatPacket> {
    public static final HeartBeatClientHandler INSTANCE = new HeartBeatClientHandler();
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, heartBeatPacket requestPacket) throws Exception {
        log.info("客户端收到心跳包");
        long responseTime = System.currentTimeMillis()-requestPacket.getTimeStamp();
        //将心跳包返回给客户端
        CompletableFuture<HeartBeatResult> future = MyRpcBootstrap.getInstance().getConfigration().getHEARTBEAT_PENDING_REQUESTS().remove(requestPacket.getRequestId());
        HeartBeatResult heartBeatResult = new HeartBeatResult();
        heartBeatResult.setResponseTime(responseTime);
        heartBeatResult.setSuccess(responseTime>0);
        if (future != null) {
            log.info("服务端回应");
            future.complete(heartBeatResult);
        }
    }
}
