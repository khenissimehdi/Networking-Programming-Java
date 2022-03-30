package tp5.exo2;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.stream.IntStream;

public class ServerLongSumUDP {
    private static final Logger logger = Logger.getLogger(ServerLongSumUDP.class.getName());
    private static final int BUFFER_SIZE = 1024;

    private final DatagramChannel dc;
    private final ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
    private final HashMap<InetSocketAddress, DataTree> memory = new HashMap<>();


    private long[] extractAnOp(ByteBuffer buffer) {
        long[] res = new long[4];
        buffer.flip();
        buffer.get();
        res[0] = buffer.getLong();
        res[1] = buffer.getLong();
        res[2] = buffer.getLong();
        res[3] = buffer.getLong();
        buffer.clear();
        return res;
    }

    private ByteBuffer craftAnAck(InetSocketAddress ip, long idPosOper, long sessionId) {
        var ackPacket =  ByteBuffer.allocateDirect(18);
        byte b = 2;
        ackPacket.put(b);
        ackPacket.putLong(sessionId);
        ackPacket.putLong(idPosOper);
        ackPacket.flip();

        return ackPacket;
    }



    private long[] craftARes(InetSocketAddress ip, long sessionId) {
        long[] res = new long[2];
        var data = memory.get(ip).getIfPresent(sessionId);
        if(!data.weGotEverything()) {
            return null;
        }
        long sum = IntStream.range(0,(int)data.getTotalOper()).mapToLong(data::getValueAt).sum();
        res[0] = sessionId;
        res[1] = sum;
        return res;
    }

    private ByteBuffer craftResPacket(long[] res) {
        var resPacket = ByteBuffer.allocate(18);
        byte b = 3;
        resPacket.put(b);;
        resPacket.putLong(res[0]);
        resPacket.putLong(res[1]);
        resPacket.flip();
        return resPacket;
    }


    private long[] createSession(InetSocketAddress ip ,ByteBuffer buffer) {
       var infos = extractAnOp(buffer);
       memory.putIfAbsent(ip, new DataTree());
       memory.get(ip).addIfAbsent(infos[0],  new DataLeaf(infos[0], infos[2])).setValueAt((int) infos[1], infos[3]);
       return infos;
    }

    private class DataTree {
        private final HashMap<Long,DataLeaf> tree = new HashMap<>();

        public DataLeaf addIfAbsent(long sessionId, DataLeaf leaf) {
            tree.putIfAbsent(sessionId, leaf);
            return tree.get(sessionId);
        }

        public DataLeaf getIfPresent(long sessionId) {
            return tree.get(sessionId);
        }

    }

    private class DataLeaf {
        private final long sessionId;
        private final long totalOper;
        private final long[] values;
        private long doneOperations;


        public DataLeaf(long sessionId, long totalOper) {
            this.sessionId = sessionId;
            this.totalOper = totalOper;
            this.values = new long[(int) totalOper];
        }

        public boolean weGotEverything() {
            return doneOperations == totalOper;
        }

        public long getSessionId() {
            return sessionId;
        }

        public long getTotalOper() {
            return totalOper;
        }

        public void setValueAt(int index, long value ) {
            if( values[index] == 0 ) {
                values[index] = value;
                doneOperations++;
            }

        }

        public long getValueAt(int index) {
            return values[index];
        }

    }

    public ServerLongSumUDP(int port) throws IOException {
        dc = DatagramChannel.open();
        dc.bind(new InetSocketAddress(port));
        logger.info("ServerLongSumUDP started on port " + port);
    }


    public void serve() throws IOException {
        try {
            while (!Thread.interrupted()) {
                buffer.clear();
                var sendIp = (InetSocketAddress) dc.receive(buffer);
                var infos = createSession(sendIp, buffer);

                var packet = craftAnAck(sendIp, infos[1], infos[0]);
                dc.send(packet, sendIp);

                var res = craftARes(sendIp, infos[0]);
                if(res != null) {
                    var resPaket = craftResPacket(res);
                    System.out.println("Got res " + res[1] );
                    dc.send(resPaket, sendIp);
                }

            }
        } finally {
            dc.close();
        }
    }

    public static void usage() {
        System.out.println("Usage : ServerIdUpperCaseUDP port");
    }

    public static void main(String[] args) throws IOException {
        System.out.println(Long.BYTES);
        System.out.println(Byte.BYTES);
        if (args.length != 1) {
            usage();
            return;
        }

        var port = Integer.parseInt(args[0]);

        if (!(port >= 1024) & port <= 65535) {
            logger.severe("The port number must be between 1024 and 65535");
            return;
        }

        try {
            new ServerLongSumUDP(port).serve();
            return;
        } catch (BindException e) {
            logger.severe("Server could not bind on " + port + "\nAnother server is probably running on this port.");
            return;
        }
    }

}
