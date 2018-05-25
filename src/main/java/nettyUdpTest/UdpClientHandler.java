package nettyUdpTest;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;

import java.util.concurrent.TimeUnit;

class UdpClientHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    @Override
    public void channelRead0(ChannelHandlerContext channelHandlerContext, DatagramPacket datagramPacket) throws Exception {
        String response = datagramPacket.content().toString(CharsetUtil.UTF_8);

        System.out.println("server say:"+response);

        TimeUnit.SECONDS.sleep(1);

        String data = response.split("\\.")[1];
        data = String.valueOf(Integer.parseInt(data)+1);
        channelHandlerContext.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(
                "你好！server."+data  , CharsetUtil.UTF_8), datagramPacket.sender()));
        //channelHandlerContext.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        cause.printStackTrace();
    }

}