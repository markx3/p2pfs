import java.net.*;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.util.*;
import java.io.*;
import java.nio.file.*;

public class FileHandler {

    private final int BUF_SIZE = 64*1024;

    public FileHandler () {

    }


    public Metadata readFileToBytes(File file) throws IOException {

        FileInputStream fis = new FileInputStream(file.getPath());
        Metadata mdata = new Metadata(file.toPath().getFileName().toString(), fis.getChannel().size());
        byte[] buf = new byte[BUF_SIZE];
        int read = 0;
        while ((read = fis.read(buf)) > 0) {
            mdata.addChunk(new Data(buf));
        }
        return mdata;
    }

    public void restoreFile(LinkedList<Data> dataList, String filename) throws FileNotFoundException, IOException {
        FileOutputStream stream = new FileOutputStream(filename);
        try {
            for (Data d : dataList) {
                if (d.getData() != null) {
                    stream.write(d.getData());
                    stream.flush();
                }
            }
            stream.close();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    public void serializeMetadata(Metadata o) throws IOException, ClassNotFoundException {
		FileOutputStream fos = new FileOutputStream("metadata/" + o.getFilename() + ".sdi");
		ObjectOutputStream oos = new ObjectOutputStream(fos);
        o.eraseChunks();
		oos.writeObject(o);
		oos.flush();
		oos.close();
		fos.close();
	}

    public void serializeData(Data o) throws IOException, ClassNotFoundException {
        FileOutputStream fos = new FileOutputStream("chunks/" + Long.toString(o.getHashChunk()) + ".chunk");
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(o);
        oos.flush();
        oos.close();
        fos.close();
    }

    // DEPRECATED
    // recoverChunks has been deprecated since our last version
    // and will be deleted in the future
    // REASON: Stopped storing all chunks in memory.
    //                                          Still wondering why we did that.
    public Hashtable <Long, Data> recoverChunks() throws IOException, FileNotFoundException, ClassNotFoundException {
        LinkedList <String> files = sdiFiles("chunks", ".chunk");
        Hashtable <Long, Data> aux = new Hashtable<>();
        for (String fname : files) {
            FileInputStream fis = new FileInputStream(fname);
            ObjectInputStream ois = new ObjectInputStream(fis);
            Data tmp = (Data) ois.readObject();
            aux.put(tmp.getHashChunk(), tmp);
            fis.close();
            ois.close();
        }
        return aux;
    }

    public Data recoverSingleChunk(long hash_chunk) throws IOException, FileNotFoundException, ClassNotFoundException {
        String fname = "chunks/" + Long.toString(hash_chunk) + ".chunk";

        FileInputStream fis = new FileInputStream(fname);
        ObjectInputStream ois = new ObjectInputStream(fis);
        Data ret = (Data) ois.readObject();
        fis.close();
        ois.close();

        return ret;
    }

	public Hashtable <String,Metadata> recoverMetadata() throws IOException, FileNotFoundException, ClassNotFoundException {
        LinkedList<String> files = sdiFiles("metadata", ".sdi");
        Hashtable <String,Metadata> aux = new Hashtable<String,Metadata>();
        for (String fname : files) {
            FileInputStream fis = new FileInputStream(fname);
    		ObjectInputStream ois = new ObjectInputStream(fis);
            Metadata tmp = (Metadata) ois.readObject();
            aux.put(tmp.getFilename(), tmp);
            fis.close();
    		ois.close();
        }
		return aux;
	}

    public LinkedList<String> sdiFiles(String directory, String r) {
        LinkedList<String> files = new LinkedList<String>();
        File dir = new File(directory);
        for (File file : dir.listFiles()) {
            if (file.getName().endsWith(r)) {
                files.add(directory + "/" + file.getName());
            }
        }
        return files;
    }

	public Boolean verifyMetadata() throws IOException, FileNotFoundException {
		File file = new File("metadata.dat");
		if(file.exists()) return true;
		return false;
	}
}
