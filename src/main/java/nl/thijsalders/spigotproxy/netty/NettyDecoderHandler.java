package nl.thijsalders.spigotproxy.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;

public class NettyDecoderHandler extends MessageToMessageDecoder<Object> {

	private final NettyInjectHandler handler;
	
	public NettyDecoderHandler(NettyInjectHandler handler) {
		this.handler = handler;
	}

	@Override
	protected void decode(ChannelHandlerContext context, Object packet, List<Object> out) throws Exception {
		this.handler.packetReceived(this, context, packet);
		out.add(packet);
	}

}
