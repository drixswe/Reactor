package ink.reactor.server.network;

import ink.reactor.api.player.Player;
import ink.reactor.api.player.connection.PacketOutbound;
import ink.reactor.api.player.connection.PlayerConnection;
import ink.reactor.chat.component.ChatComponent;

import ink.reactor.protocol.ConnectionState;
import ink.reactor.protocol.outbound.configuration.PacketOutConfigDisconnected;
import ink.reactor.protocol.outbound.login.PacketOutLoginDisconnect;
import ink.reactor.protocol.outbound.play.PacketOutPing;
import ink.reactor.protocol.outbound.play.PacketOutPlayDisconnected;
import io.netty.channel.socket.SocketChannel;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class PlayerConnectionImpl implements PlayerConnection {

    private final SocketChannel channel;
    private Player player;

    private boolean isFirstConfig = true;

    private long lastPing;

    public volatile ConnectionState state = ConnectionState.HANDSHAKE;
    private final KeepAliveManager keepAliveManager = new KeepAliveManager(this);
    private volatile boolean online = true;

    public PlayerConnectionImpl(SocketChannel channel) {
        this.channel = channel;
    }

    public void sendPacket(final PacketOutbound packet) {
        if (!online) {
            return;
        }
        if (channel.eventLoop().inEventLoop()) {
            channel.writeAndFlush(packet);
            return;
        }
        channel.eventLoop().execute(() -> channel.writeAndFlush(packet));
    };

    public void sendPackets(final PacketOutbound... packets) {
        if (!online) {
            return;
        }
        if (channel.eventLoop().inEventLoop()) {
            for (final PacketOutbound packet : packets) {
                channel.write(packet);
            }
            channel.flush();
            return;
        }

        channel.eventLoop().execute(() -> {
            for (final PacketOutbound packet : packets) {
                channel.write(packet);
            }
            channel.flush();
        });
    };

    public void ping() {
        if (state == ConnectionState.PLAY) {
            lastPing = System.currentTimeMillis();
            sendPacket(PacketOutPing.INSTANCE);
        }
    }

    @Override
    public void disconnect() {
        this.online = false;
        this.player = null;
        this.channel.close();
    }

    public void disconnect(final ChatComponent... reason) {
        switch (state) {
            case LOGIN -> sendPacket(new PacketOutLoginDisconnect(reason));
            case PLAY -> sendPacket(new PacketOutPlayDisconnected(reason));
            case CONFIGURATION -> sendPacket(new PacketOutConfigDisconnected(reason));
        }
        disconnect();
    };
}