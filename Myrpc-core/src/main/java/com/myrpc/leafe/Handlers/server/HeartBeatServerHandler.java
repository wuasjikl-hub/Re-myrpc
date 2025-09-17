package com.myrpc.leafe.Handlers.server;

import com.myrpc.leafe.packet.heartBeat.heartBeatPacket;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ChannelHandler.Sharable
public class HeartBeatServerHandler extends SimpleChannelInboundHandler<heartBeatPacket> {
    public static final HeartBeatServerHandler INSTANCE = new HeartBeatServerHandler();
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, heartBeatPacket requestPacket) throws Exception {
        log.info("服务端收到心跳包");
        //将心跳包返回给客户端
        channelHandlerContext.channel().writeAndFlush(requestPacket);
    }
}
