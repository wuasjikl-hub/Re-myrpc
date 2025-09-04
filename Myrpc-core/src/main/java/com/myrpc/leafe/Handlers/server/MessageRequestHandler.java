package com.myrpc.leafe.Handlers.server;

import com.myrpc.leafe.packet.client.rpcRequestPacket;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
@ChannelHandler.Sharable    // 标记该Handler可以多个Channel共享
public class MessageRequestHandler extends SimpleChannelInboundHandler<rpcRequestPacket> {
    public static final MessageRequestHandler INSTANCE = new MessageRequestHandler();
    private MessageRequestHandler(){

    }
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, rpcRequestPacket rpcRequestPacket) throws Exception {

    }
}
