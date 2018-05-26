package nettyKcpTest;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.CharsetUtil;

import java.net.InetSocketAddress;

public class SearchClient {

    public static final int MessageReceived = 0x99;
    private int scanPort;

    public static void main(String[] args) {
        SearchClient client = new SearchClient(9999);
        client.sendPackage();
    }

    public SearchClient(int scanPort) {
            this.scanPort = scanPort;
    }


    private static class CLientHandler extends SimpleChannelInboundHandler<DatagramPacket> {
        @Override protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) throws Exception {
            String  body =  packet.content().toString(CharsetUtil.UTF_8);
            System.out.println(body);
        }
    }

    public void sendPackage() {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioDatagramChannel.class)
                    .option(ChannelOption.SO_BROADCAST, true)
                    .handler(new CLientHandler());

            Channel ch = b.bind(0).sync().channel();

            ch.writeAndFlush(new DatagramPacket(
                    Unpooled.copiedBuffer("Searh:", CharsetUtil.UTF_8),
                    new InetSocketAddress("255.255.255.255", scanPort))).sync();


            // QuoteOfTheMomentClientHandler will close the DatagramChannel when a
            // response is received.  If the channel is not closed within 5 seconds,
            // print an error message and quit.
            if (!ch.closeFuture().await(5000)) {
                System.err.println("Search request timed out.");
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        finally {
            group.shutdownGracefully();
        }
    }
}