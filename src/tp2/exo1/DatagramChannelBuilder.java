package tp2.exo1;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;

public class DatagramChannelBuilder {

    public static DatagramChannel openChannel() throws IOException {
        DatagramChannel datagramChannel = DatagramChannel.open();
        return datagramChannel;
    }

    public static DatagramChannel bindChannel(SocketAddress local) throws IOException {
        return openChannel().bind(local);
    }

    public static void sendMessage(DatagramChannel client, String msg, SocketAddress serverAddress, Charset charset) throws IOException {
        ByteBuffer buffer =  charset.encode(msg);
        client.send(buffer, serverAddress);
    }
}