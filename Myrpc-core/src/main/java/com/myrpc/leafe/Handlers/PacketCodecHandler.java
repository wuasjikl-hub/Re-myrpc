package com.myrpc.leafe.Handlers;

import com.myrpc.leafe.packet.Packet;
import com.myrpc.leafe.Handlers.packetCode.PacketCodec;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
@Slf4j
@ChannelHandler.Sharable
public class PacketCodecHandler extends MessageToMessageCodec<ByteBuf, Packet> {
    public static final PacketCodecHandler INSTANCE = new PacketCodecHandler();

    private PacketCodecHandler() {

    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> out) {
        try {
            out.add(PacketCodec.INSTANCE.decode(byteBuf));
        }catch (Exception e){
            log.error("解码异常",e);
        }
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Packet packet, List<Object> out) {
        ByteBuf byteBuf = ctx.channel().alloc().ioBuffer();
        try {
            PacketCodec.INSTANCE.encode(byteBuf, packet);
        } catch (Exception e) {
            log.error("编码异常", e);
        }
        out.add(byteBuf);
    }
}
