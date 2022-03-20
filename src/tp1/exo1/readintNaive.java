package tp1.exo1;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Scanner;

public class readintNaive {

    public static void main(String[] args) throws IOException {
        Path path = Paths.get(args[1]);
        ByteBuffer buff = ByteBuffer.allocate(Integer.BYTES);  // 4 bytes
        try(FileChannel fc = FileChannel.open(path, StandardOpenOption.CREATE,
                StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            Scanner scan = new Scanner(System.in)) {
            while (scan.hasNextInt()) {
                buff.putInt(scan.nextInt());
                buff.flip();
                fc.write(buff);
                buff.clear();
            }
        }

    }
}
