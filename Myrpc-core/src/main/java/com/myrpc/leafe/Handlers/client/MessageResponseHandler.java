package com.myrpc.leafe.Handlers.client;

import com.myrpc.leafe.bootatrap.MyRpcBootstrap;
import com.myrpc.leafe.packet.server.rpcResponsePacket;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
@ChannelHandler.Sharable    // 标记该Handler可以多个Channel共享
public class MessageResponseHandler extends SimpleChannelInboundHandler<rpcResponsePacket> {
    public static final MessageResponseHandler INSTANCE = new MessageResponseHandler();
    private MessageResponseHandler(){

    }
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, rpcResponsePacket rpcResponsePacket) throws Exception {
        log.info("收到服务端返回数据:{}",rpcResponsePacket);
        CompletableFuture<Object> res = MyRpcBootstrap.getInstance().getConfigration().getPNDING_REQUESTS().remove(rpcResponsePacket.getRequestId());
        if (res != null) {
            log.info("服务端回应");
            res.complete(rpcResponsePacket.getObject());
        } else {
            res.completeExceptionally(new Exception("未找到对应的请求"));
        }

    }
}
