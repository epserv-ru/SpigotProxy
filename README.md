# SpigotProxy

SpigotProxy is a Spigot plugin that enables compatibility with HAProxy's PROXY v2 protocol.

# Deprecated

This plugin is no longer needed because Paper 1.18.2 now has PROXY v2 protocol compatibility built-in since build 344 [[31ccc57]](https://github.com/PaperMC/Paper/commit/31ccc579b5cab625b8e0c4ee4d521ffb6bf984b2). This project was a fork of [riku6460/SpigotProxy](https://github.com/riku6460/SpigotProxy), because riku6460's project seemed abandoned and was no longer receiving updates, but now it does and is being actively updated, so use [riku6460's project](https://github.com/riku6460/SpigotProxy) if you're still looking for updates of this project.

This fork is now archived. Feel free to contact me on Discord by `@metabrix` if you have any questions.

## What's HAProxy?

[HAProxy](http://www.haproxy.org/) is a high performance, open-source Load Balancer and Reverse Proxy for TCP-based services.


## What's the PROXY protocol?

The PROXY protocol was designed to transmit a HAProxy client's connection information to the destination through other proxies or NAT routing (or both). HAProxy must be configured to send PROXY packets to your Spigot backend(s) in the `backend` or `server` configuration using the `send-proxy-v2` option.


## Installing HAProxy 1.5 / 1.6 on Ubuntu or Debian

HAProxy 1.5 or above is required to use the PROXY protocol.

Please see [https://haproxy.debian.net](https://haproxy.debian.net) for instructions on how to obtain a more recent package of HAProxy on Ubuntu or Debian.

## Example configuration

<pre>
listen minecraft
	bind :25565
	mode tcp
	balance leastconn
	option tcp-check
	server minecraft1 192.168.0.1:25565 check-send-proxy check send-proxy-v2
	server minecraft2 192.168.0.2:25565 check-send-proxy check send-proxy-v2
	server minecraft3 192.168.0.3:25565 check-send-proxy check send-proxy-v2
	server minecraft4 192.168.0.4:25565 check-send-proxy check send-proxy-v2
	server minecraft5 192.168.0.5:25565 check-send-proxy check send-proxy-v2
	server minecraft6 192.168.0.6:25565 check-send-proxy check send-proxy-v2
	server minecraft7 192.168.0.7:25565 check-send-proxy check send-proxy-v2
	server minecraft8 192.168.0.8:25565 check-send-proxy check send-proxy-v2
</pre>


## How do I know it's working?

In the Spigot server (with late-bind on false), you should see the actual IP of players connecting through HAProxy.

## Note

This plugin is inspired by [/MinelinkNetwork/BungeeProxy](https://github.com/MinelinkNetwork/BungeeProxy). Go to that repo to see the bungee version of this plugin
