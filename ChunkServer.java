import java.net.*;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.util.*;
import java.io.*;
import java.nio.file.*;

import broadcast.BroadcastServer;
import broadcast.Peer;

public class ChunkServer {

    protected Hashtable<Long, Data> chunkHashtable = new Hashtable<Long, Data>();
    protected int chunkCount;
    private FileHandler fileHandler = new FileHandler();

    private final String ip;

    public ChunkServer(String addr) throws IOException {
        this.ip = addr;
        chunkCount = 0;
        Thread receiver = new Thread(new ChunkReceiver());
        Thread listener = new Thread(new RequestListener());
        receiver.start();
        listener.start();
    }


	public boolean deliverChunksToPeers(Metadata mData, LinkedList<String> peers) throws InterruptedException {
		LinkedList<Data> chunks = new LinkedList<>();
        for (Data d : mData.getChunks()) {
            chunks.add(d);
        }
		int chunkCount = chunks.size();
		int peerCount = peers.size();
		int cpp = chunkCount/peerCount;
		for (String ip : peers) {
            LinkedList<Data> chunksToSend = new LinkedList<Data>();
            for (int i = 0; i < cpp; i++) {
                chunksToSend.add(chunks.poll());
            }
            for (Data d : chunksToSend) {
                d.addPeer(ip);
            }
            Thread sender = new Thread(new ChunkSender(chunksToSend, ip));
            sender.start();
            sender.join();
		}
        return true;
	}

    public Data requestChunk(long hash_chunk, LinkedList<String> peers) throws ClassNotFoundException {
        Data ret = null;
        System.out.println("REQUEST CHNK");
        for (String ip : peers) {
            try {
                Socket s = new Socket(ip, 1251);
                //s.setSoTimeout(10000);
                ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
                out.writeObject(hash_chunk);
                out.flush();
                ret = requestConsumer();
                if (ret != null)
                    return ret;
            } catch (IOException e) {}
        }
        return ret;
	}

    private Data requestConsumer() throws IOException, SocketException, ClassNotFoundException {
        ServerSocket server = new ServerSocket(1252);
        Socket s = server.accept();
        s.setSoTimeout(10000);
        ObjectInputStream in = new ObjectInputStream(s.getInputStream());
        Data chunk = (Data) in.readObject();
        return chunk;
    }

    private void storeChunk(Data chunk) throws IOException, ClassNotFoundException {
        chunkHashtable.put(chunk.getHashChunk(), chunk);
        fileHandler.serializeData(chunk);
    }

    private class ChunkSender implements Runnable {
        Thread runner;
        LinkedList<Data> chunks;
        String ip;
        int port;

        public ChunkSender(LinkedList<Data> chunks, String ip) {
            this.chunks = chunks;
            this.ip = ip;
            runner = new Thread(this);
            runner.start();
            port = 1250;
        }

        public ChunkSender(LinkedList<Data> chunks, String ip, int port) {
            this.chunks = chunks;
            this.ip = ip;
            runner = new Thread(this);
            runner.start();
            this.port = port;
        }

        public void run() {
            for (Data chunk : chunks) {
                try {
                    Socket s = new Socket(ip, port);
                    ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
                    out.writeObject(chunk);
                    out.flush();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class ChunkReceiver implements Runnable  {
        private Thread runner;
        private ServerSocket server = new ServerSocket(1250);

        public ChunkReceiver() throws IOException {};

        public void run() {
            while (true) {
                try{
                    Socket s = server.accept();
                    ObjectInputStream in = new ObjectInputStream(s.getInputStream());
                    Data chunk = (Data) in.readObject();
                    storeChunk(chunk);
                } catch (ClassNotFoundException|IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /*private class RequestConsumer implements Runnable {
        private Thread runner;
        private ServerSocket server = new ServerSocket(1252);

        public ResquestConsumer() {}

        public void run() {
            while (true) {
                try {
                    Socket s = server.accept();
                    ObjectInputStream in = new ObjectInputStream(s.getInputStream());
                    Data chunk = (Data) in.readObject();

                }
            }
        }
    }*/

    private class RequestListener implements Runnable {
        private Thread runner;
        private ServerSocket server = new ServerSocket(1251);
        private String ip;

        public RequestListener() throws IOException {}

        public void run() {
            while (true) {
                try {
                    Socket s = server.accept();
                    ObjectInputStream in = new ObjectInputStream(s.getInputStream());
                    Long hash = (Long) in.readObject();
                    Data d = chunkHashtable.get(hash);
                    LinkedList<Data> ret = new LinkedList<Data>();
                    ret.add(d);
                    Thread sender = new Thread(new ChunkSender(ret, s.getInetAddress().getHostAddress(), 1252));
                } catch (ClassNotFoundException|IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
