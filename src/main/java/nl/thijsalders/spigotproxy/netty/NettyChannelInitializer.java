package nl.thijsalders.spigotproxy.netty;

import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import nl.thijsalders.spigotproxy.SpigotProxy;
import nl.thijsalders.spigotproxy.haproxy.HAProxyMessage;
import nl.thijsalders.spigotproxy.haproxy.HAProxyMessageDecoder;
import org.bukkit.entity.Player;

public class NettyChannelInitializer extends ChannelInitializer<SocketChannel> {


	private final ChannelInitializer<SocketChannel> oldChildHandler;
	private final Method oldChildHandlerMethod;
	private Field socketAddressField;
	
	public NettyChannelInitializer(ChannelInitializer<SocketChannel> oldChildHandler, String minecraftPackage) throws Exception {
		this.oldChildHandler = oldChildHandler;
		this.oldChildHandlerMethod = oldChildHandler.getClass().getDeclaredMethod("initChannel", Channel.class);
		this.oldChildHandlerMethod.setAccessible(true);

		Class<?> networkManager = Class.forName(minecraftPackage + ".NetworkManager");
		try {
			this.socketAddressField = networkManager.getField("socketAddress");
		} catch (NoSuchFieldException e) {
			this.socketAddressField = networkManager.getField("l");
		}
	}
	
	@Override
	protected void initChannel(SocketChannel channel) throws Exception {
		this.oldChildHandlerMethod.invoke(this.oldChildHandler, channel);
		
		channel.pipeline().addAfter("timeout", "haproxy-decoder", new HAProxyMessageDecoder());
		channel.pipeline().addAfter("haproxy-decoder", "haproxy-handler", new ChannelInboundHandlerAdapter() {
        	@Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                if (msg instanceof HAProxyMessage) {
                    HAProxyMessage message = (HAProxyMessage) msg;

                    String realAddress = message.sourceAddress();
                    int realPort = message.sourcePort();

                    SocketAddress socketAddress = new InetSocketAddress(realAddress, realPort);

                    ChannelHandler handler = channel.pipeline().get("packet_handler");

                    SocketAddress oldAddress = (SocketAddress) socketAddressField.get(handler);
                    if (!socketAddress.equals(oldAddress)) {
                    	Object entityPlayer = handler.getClass().getMethod("getPlayer").invoke(handler);
                    	Player player = (Player) entityPlayer.getClass().getMethod("getBukkitEntity")
								.invoke(entityPlayer);
						SpigotProxy.getInstance().playerProxies.put(player.getUniqueId(), oldAddress);
					}

					socketAddressField.set(handler, socketAddress);
                } else {
                    super.channelRead(ctx, msg);
                }
            }
		});
	}

	public ChannelInitializer<SocketChannel> getOldChildHandler() {
		return oldChildHandler;
	}
}