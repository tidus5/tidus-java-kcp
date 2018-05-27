package nettyKcpTest;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.CharsetUtil;
import kcp.KCP;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TestNettyKCPConnection {
	private KCP kcp;
	private InetSocketAddress targetAddress;
	private Channel channel;
	private byte[] data = new byte[1000];
	private ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();

	public TestNettyKCPConnection(int conversationId, String targetIp, int targetPort, int localPort) {

		targetAddress = new InetSocketAddress(targetIp, targetPort);

		kcp = new KCP(conversationId) {
			// 设置发送消息底层方法
			@Override protected void output(byte[] buffer, int size) {
				//根据回话id， 找到目标channel
				Channel ch = channel;
				if(ch!= null && ch.isActive()) {
					ByteBuf buf = Unpooled.wrappedBuffer(buffer, 0, size);
					ch.writeAndFlush(new DatagramPacket(buf, targetAddress));
				}
			}
		};

		Bootstrap b = new Bootstrap();
		channel = b.group(new NioEventLoopGroup())
				//都只能设置Allocator的类型，无法直接设置ByteBufAllocator分配的ByteBuf类型
				.option(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator.DEFAULT)
				.channel(NioDatagramChannel.class)
				.option(ChannelOption.SO_BROADCAST, true)
				.handler(new SimpleChannelInboundHandler<DatagramPacket>() {
					//设置收到 消息的处理方法
			@Override protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket datagramPacket) throws Exception {
				try{
				if(kcp != null) {
					int len = datagramPacket.content().readableBytes();
					byte[] arr = new byte[len];
					datagramPacket.content().getBytes(0, arr);
					kcp.Input(arr);
				}}catch (Exception e){
					e.printStackTrace();
				}
			}
		}).bind(localPort).channel();

		update();
	}

	public void update(){
		service.scheduleAtFixedRate(new Runnable() {
			@Override public void run() {
				kcp.Update(System.currentTimeMillis());
				checkMessageRecieved();

			}
		},0,10,TimeUnit.MILLISECONDS);
	}

	public void checkMessageRecieved(){
		int len = kcp.Recv(data);
		if (len > 0) {
			String str = new String(data, 0, len, CharsetUtil.UTF_8);
			System.out.println("收到：" + str);
		}
	}

	public void sendMessage(byte[] buffer){
		service.execute(new Runnable() {
			@Override public void run() {
				kcp.Send(buffer);
			}
		});

	}

	public static void main(String[] args) {

		TestNettyKCPConnection server = new TestNettyKCPConnection(1,"localhost",8800,9900);

		TestNettyKCPConnection client = new TestNettyKCPConnection(1,"localhost",9900,8800);

		try {
			TimeUnit.SECONDS.sleep(1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		while(true)
		client.sendMessage("client say Hello".getBytes(CharsetUtil.UTF_8));
		//server.sendMessage("server say Hello".getBytes(CharsetUtil.UTF_8));


	}
}
