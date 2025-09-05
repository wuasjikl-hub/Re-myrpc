package com.myrpc.leafe.Handlers.server;

import com.myrpc.leafe.common.Constant;
import com.myrpc.leafe.packet.client.rpcRequestPacket;
import com.myrpc.leafe.packet.server.rpcResponsePacket;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ChannelHandler.Sharable    // 标记该Handler可以多个Channel共享
public class MessageRequestHandler extends SimpleChannelInboundHandler<rpcRequestPacket> {
    public static final MessageRequestHandler INSTANCE = new MessageRequestHandler();
    private MessageRequestHandler(){

    }
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, rpcRequestPacket rpcRequestPacket) throws Exception {
        log.info("服务端收到数据:{}",rpcRequestPacket);
        rpcResponsePacket rpcResponsePacket = new rpcResponsePacket();
        rpcResponsePacket.setRequestId(rpcRequestPacket.getRequestId());
        rpcResponsePacket.setRequestType(Constant.RESPONSE);
        rpcResponsePacket.setCompressType(rpcRequestPacket.getCompressType());
        rpcResponsePacket.setSerializeType(rpcRequestPacket.getSerializeType());
        rpcResponsePacket.setCode((byte) 0);
        String name = "husfois";
        rpcResponsePacket.setObject(name);
        channelHandlerContext.channel().writeAndFlush(rpcResponsePacket);
    }
}
