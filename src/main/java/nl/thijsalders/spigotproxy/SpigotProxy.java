package nl.thijsalders.spigotproxy;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import nl.thijsalders.spigotproxy.netty.NettyChannelInitializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class SpigotProxy extends JavaPlugin {
	
	private static final String CHANNEL_FIELD_NAME;
	private static final String MINECRAFT_PACKAGE;

	public final HashMap<Object, SocketAddress> playerProxies = new HashMap<>();

	static {
		String version = Bukkit.getServer().getClass().getPackage().getName().replace(".",  ",")
				.split(",")[3];
		CHANNEL_FIELD_NAME = getChannelFieldName(version);
		if (CHANNEL_FIELD_NAME == null)
			throw new IllegalStateException("Unsupported server version " + version
					+ ", please see if there are any updates available");
		else
			System.out.println("[SpigotProxy] Detected server version " + version);

		try {
			MINECRAFT_PACKAGE = getMinecraftServer().getClass().getPackage().getName();
		} catch (Exception ex) {
			throw new IllegalStateException("Unsupported server version " + version
					+ ", please see if there are any updates available", ex);
		}
	}
	
	public void onLoad() {
		getLogger().info("Loading " + this.getName() + "...");
		try {
			getLogger().info("Injecting NettyHandler...");
			this.inject();
			getLogger().info("Injection successful!");
		} catch (Exception e) {
			getLogger().info("Injection netty handler failed!");
			if (e instanceof RuntimeException)
				throw (RuntimeException) e;
			else
				throw new RuntimeException(e);
		}
	}

	private void inject() throws Exception {
		Object minecraftServer = getMinecraftServer();
		for (ChannelFuture channelFuture : this.getChannelFutureList(minecraftServer)) {
			ChannelPipeline channelPipeline = channelFuture.channel().pipeline();
			ChannelHandler serverBootstrapAcceptor = channelPipeline.first();
			ChannelInitializer<SocketChannel> oldChildHandler = ReflectionUtils.getDeclaredField(serverBootstrapAcceptor,
					"childHandler");
			ReflectionUtils.setFinalField(serverBootstrapAcceptor, "childHandler",
					new NettyChannelInitializer(oldChildHandler, MINECRAFT_PACKAGE));
		}
	}
	
	public static String getChannelFieldName(String version) {
		String name = "listeningChannels";
		switch (version){
			case "v1_12_R1":
			case "v1_11_R1":
			case "v1_10_R1":
			case "v1_9_R2":
			case "v1_9_R1":
			case "v1_8_R2":
			case "v1_8_R3":
				name = "g";
				break;
			case "v1_14_R1":
			case "v1_13_R1":
			case "v1_13_R2":
			case "v1_8_R1":
				name = "f";
				break;
			case "v1_7_R4":
				name = "e";
				break;
		}
		return name;
	}

	private static Object getMinecraftServer() throws Exception {
		return Bukkit.getServer().getClass().getDeclaredMethod("getServer").invoke(Bukkit.getServer());
	}

	private List<ChannelFuture> getChannelFutureList(Object minecraftServer) throws Exception {
		Method serverConnectionMethod = null;
		for (Method method : minecraftServer.getClass().getSuperclass().getDeclaredMethods()) {
			if (!method.getReturnType().getSimpleName().equals("ServerConnection"))
				continue;
			serverConnectionMethod = method;
			break;
		}

		if (serverConnectionMethod == null)
			throw new NullPointerException("serverConnectionMethod is null");

		Object serverConnection = serverConnectionMethod.invoke(minecraftServer);
		return ReflectionUtils.getDeclaredField(serverConnection, CHANNEL_FIELD_NAME);
	}

	public void onDisable() {
		try {
			getLogger().info("Removing injected netty handler...");
			this.uninject();
			getLogger().info("Netty handler removed!");
		} catch (Exception e) {
			getLogger().info("Failed to remove netty handler!");
			if (e instanceof RuntimeException)
				throw (RuntimeException) e;
			else
				throw new RuntimeException(e);
		}
	}

	private void uninject() throws Exception {
		Object minecraftServer = getMinecraftServer();
		for (ChannelFuture channelFuture : this.getChannelFutureList(minecraftServer)) {
			ChannelPipeline channelPipeline = channelFuture.channel().pipeline();
			ChannelHandler serverBootstrapAcceptor = channelPipeline.first();
			Object childHandler = ReflectionUtils.getDeclaredField(serverBootstrapAcceptor, "childHandler");
			if (childHandler instanceof NettyChannelInitializer) {
				NettyChannelInitializer nettyChannelInitializer = (NettyChannelInitializer) childHandler;
				ReflectionUtils.setFinalField(serverBootstrapAcceptor, "childHandler",
						nettyChannelInitializer.getOldChildHandler());
			}
		}
	}

	/**
	 * Returns <code>true</code> if the <code>player</code> is connected through HAProxy.
	 * @param player An online player.
	 * @return <code>true</code> if the <code>player</code> is connected through HAProxy; <code>false</code> otherwise.
	 */
	public boolean isProxied(@NotNull Player player) {
		return this.getProxyAddress(player) != null;
	}

	/**
	 * Returns <code>true</code> if the <code>player</code> is directly connected without HAProxy.
	 * @param player An online player.
	 * @return <code>true</code> if the <code>player</code> is directly connected without HAProxy; <code>false</code> otherwise.
	 */
	public boolean isConnectedDirectly(@NotNull Player player) {
		return this.getProxyAddress(player) == null;
	}

	/**
	 * Returns the {@link SocketAddress} of HAProxy through which the <code>player</code> is connected.
	 *
	 * @param player An online player who is connected through a proxy.
	 * @return {@link SocketAddress} of HAProxy through which the <code>player</code> is connected.
	 * <code>null</code> if <code>player</code> is connected directly without HAProxy.
	 */
	@Nullable
	public SocketAddress getProxyAddress(@NotNull Player player) {
		try {
			Object entityPlayer = player.getClass().getMethod("getHandle").invoke(player);
			Object networkManager = ReflectionUtils.getDeclaredField(ReflectionUtils.getDeclaredField(entityPlayer,
					"playerConnection"), "networkManager");
			return this.playerProxies.get(networkManager);
		} catch (Throwable t) {
			t.printStackTrace();
			return null;
		}
	}

	/**
	 * Returns SpigotProxy instance.
	 * @return {@link SpigotProxy} instance.
	 */
	public static SpigotProxy getInstance() {
		return SpigotProxy.getPlugin(SpigotProxy.class);
	}

}
