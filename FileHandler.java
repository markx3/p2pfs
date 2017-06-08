import java.net.*;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.util.*;
import java.io.*;
import java.nio.file.*;

public class FileHandler {

    public FileHandler () {

    }

    // TODO Arrumar divisão de chunks.
    public Metadata readFileToBytes(String filename)  throws IOException {
        RandomAccessFile raf = new RandomAccessFile(filename, "r");
        Metadata m = new Metadata(filename, raf.length());
        long sizePerChunk = raf.length()/3;
        long remainingBytes = raf.length() % 3;
        long maxReadBytes = 64 * 1024; // 8KB
        for(int i = 1; i <= 3; i++) {
            if(sizePerChunk < maxReadBytes) {
                long numReads = sizePerChunk / maxReadBytes;
                long numRemainingReads = sizePerChunk % maxReadBytes;
                for(int j = 0; j <numReads; j++) {
                    byte[] d = readWrite(raf, maxReadBytes);
                    m.addChunk(new Data(d));
                }
                if(numRemainingReads > 0) {
                    byte[] d = readWrite(raf, numRemainingReads);
                    m.addChunk(new Data(d));
                }
            } else {
                byte[] d = readWrite(raf, sizePerChunk);
                m.addChunk(new Data(d));
            }

        }
        if(remainingBytes > 0) {
            byte[] d = readWrite(raf, remainingBytes);
            m.addChunk(new Data(d));
        }
        for(int i = 0; i < m.getChunks().size(); i++)
            System.out.println(new String(m.getChunks().get(i).getData(), "UTF-8"));
        raf.close();
        return m;
    }

    public byte[] readWrite(RandomAccessFile raf, long numBytes) throws IOException {
        byte[] buf = new byte[(int) numBytes];
        int val = raf.read(buf);
        if(val != -1) return buf;
        return buf;
    }

    public void serializeMetadata(Object o) throws IOException, ClassNotFoundException {
		FileOutputStream fos = new FileOutputStream("metadata.dat");
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(o);
		oos.flush();
		oos.close();
		fos.close();
	}

	public Hashtable <String,Metadata> recoverMetadata() throws IOException, FileNotFoundException, ClassNotFoundException {
		FileInputStream fis = new FileInputStream("metadata.dat");
		ObjectInputStream ois = new ObjectInputStream(fis);
		Hashtable <String,Metadata> aux = (Hashtable<String,Metadata>) ois.readObject();
		fis.close();
		ois.close();
		return aux;
	}

	public Boolean verifyMetadata() throws IOException, FileNotFoundException {
		File file = new File("metadata.dat");
		if(file.exists()) return true;
		return false;
	}
}
