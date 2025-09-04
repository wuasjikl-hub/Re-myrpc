package com.myrpc.leafe.Handlers.client;

import com.myrpc.leafe.packet.server.rpcResponsePacket;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
@ChannelHandler.Sharable    // 标记该Handler可以多个Channel共享
public class MessageResponseHandler extends SimpleChannelInboundHandler<rpcResponsePacket> {
    public static final MessageResponseHandler INSTANCE = new MessageResponseHandler();
    private MessageResponseHandler(){

    }
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, rpcResponsePacket rpcResponsePacket) throws Exception {

    }
}
