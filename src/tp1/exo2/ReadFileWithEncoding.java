package tp1.exo2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Scanner;

import static java.nio.file.StandardOpenOption.*;

public class ReadFileWithEncoding {

	private static void usage() {
		System.out.println("Usage: ReadFileWithEncoding charset filename");
	}

	private static int truncateLength(FileChannel fc, int length)
			throws IOException {
		return (int) Math.min(length > 0 ? length : fc.size(),
				Integer.MAX_VALUE);
	}


	private static String stringFromFile(Charset cs, Path path) throws IOException {
		var r = new StringBuilder();
		try(var outChannel = FileChannel.open(path, READ)) {
			ByteBuffer buffer = ByteBuffer.allocate(1024);


		   /*while (buffer.remaining() < 1024)
				outChannel.read(buffer);
				buffer.flip();
				var cb = cs.decode(buffer);
				r.append(cb);
				buffer.clear();
			}*/

			while (outChannel.read(buffer) != -1) {
				outChannel.read(buffer);
				buffer.flip();
				var cb = cs.decode(buffer);
				r.append(cb);
				buffer.clear();
			}

			return r.toString();
		}
	}

	public static void main(String[] args) throws IOException {
		if (args.length != 2) {
			usage();
			return;
		}
		var cs = Charset.forName(args[0]);
		var path = Path.of(args[1]);
		System.out.print(stringFromFile(cs, path));
	}
}