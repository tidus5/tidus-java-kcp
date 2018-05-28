package nettyUdpExample;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;

public class SearchServer {
    public static void main(String[] args) {
        SearchServer.initServer();
    }

    public static void initServer() {//udp服务端，接受客户端发送的广播
        try {
            Bootstrap b = new Bootstrap();
            EventLoopGroup group = new NioEventLoopGroup();
            b.group(group)
                    .channel(NioDatagramChannel.class)
                    .option(ChannelOption.SO_BROADCAST, true)
                    .handler(new UdpServerHandler());
            b.bind(AppConstants.SEARCH_PORT).sync().channel().closeFuture().await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static class UdpServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {


        @Override protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) throws Exception {
            ByteBuf buf = packet.copy().content();
            byte[] req = new byte[buf.readableBytes()];
            buf.readBytes(req);
            String body = new String(req, "UTF-8");
            System.out.println(body);//打印收到的信息

            //向客户端发送消息
            String json = "给客户端发送的内容";

            // 由于数据报的数据是以字符数组传的形式存储的，所以传转数据
            byte[] bytes = json.getBytes("UTF-8");
            DatagramPacket data = new DatagramPacket(Unpooled.copiedBuffer(bytes), packet.sender());
            ctx.writeAndFlush(data);//向客户端发送消息
        }
    }

}