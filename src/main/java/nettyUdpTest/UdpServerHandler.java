package nettyUdpTest;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;

public class UdpServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {


    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, DatagramPacket datagramPacket) throws Exception {
        // 因为Netty对UDP进行了封装，所以接收到的是DatagramPacket对象。
        String req = datagramPacket.content().toString(CharsetUtil.UTF_8);
        System.out.println("client say:"+req);

        String data = req.split("\\.")[1];

        channelHandlerContext.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(
                    "你好！client."+data  , CharsetUtil.UTF_8), datagramPacket.sender()));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        cause.printStackTrace();
    }


}