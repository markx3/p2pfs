import java.net.*;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.util.*;
import java.io.*;
import java.nio.file.*;

import broadcast.BroadcastServer;
import broadcast.Peer;

public class ChunkServer {

    protected int chunkCount;
    private FileHandler fileHandler = new FileHandler();
    private ServerSocket serverConsumer = new ServerSocket(1252);
    private final int TIMEOUT = 5000;

    private final String ip;

    public ChunkServer(String addr) throws IOException, ClassNotFoundException {
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
        if (chunkCount % peerCount != 0) cpp++;
		for (int i = 0; i < peers.size(); i++) {
            LinkedList<Data> chunksToSend = new LinkedList<Data>();
            String ip1 = peers.get(i);
            String ip2 = null;
            if (i+1 == peers.size())
                ip2 = peers.get(i-1);
            else
                ip2 = peers.get(i+1);

            for (int j = 0; j < cpp; j++) {
                Data tmp = chunks.poll();
                if (tmp != null)
                    chunksToSend.add(tmp);
            }
            for (Data d : chunksToSend) {
                d.addPeer(ip1);
                d.addPeer(ip2);
            }
            Thread t1 = new Thread(new ChunkSender(chunksToSend, ip1));
            t1.start();
            t1.join();
            Thread t2 = new Thread(new ChunkSender(chunksToSend, ip2));
            t2.start();
            t2.join();
		}
        return true;
	}

    public Data requestChunk(long hash_chunk, LinkedList<String> peers) throws ClassNotFoundException, IOException {
        Data ret = null;
        for (String ip : peers) {
            try {
                System.out.println("request to " + ip);
                Socket s = new Socket(ip, 1251);
                s.setSoTimeout(TIMEOUT); // 5s timeout
                ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
                out.writeObject(hash_chunk);
                out.flush();
                ret = requestConsumer();
                if (ret != null) return ret;
                } catch (IOException e) {}
        	}

        return ret;
}

    private Data requestConsumer() throws IOException, SocketException, ClassNotFoundException, IOException {
        LinkedList<Data> chunk = null;

        Socket s = serverConsumer.accept();
        s.setSoTimeout(TIMEOUT); // 5s timeout
        ObjectInputStream in = new ObjectInputStream(s.getInputStream());
        chunk = (LinkedList<Data>) in.readObject();

        return chunk.poll();
    }

    private void storeChunk(LinkedList<Data> chunks) throws IOException, ClassNotFoundException {
        for (Data chunk : chunks) {
            fileHandler.serializeData(chunk);
        }
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
            //for (Data chunk : chunks) {
                try {
                    Socket s = new Socket(ip, port);
                    s.setSoTimeout(TIMEOUT);
                    ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
                    out.writeObject(chunks);
                    out.flush();
                    Thread.sleep(10);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
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
                    s.setSoTimeout(TIMEOUT);
                    ObjectInputStream in = new ObjectInputStream(s.getInputStream());
                    LinkedList<Data> chunks = (LinkedList<Data>) in.readObject();
                    storeChunk(chunks);
                } catch (ClassNotFoundException|IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class RequestListener implements Runnable {
        private Thread runner;
        private ServerSocket server = new ServerSocket(1251);
        private String ip;

        public RequestListener() throws IOException {}

        public void run() {
            while (true) {
                try {
                    Socket s = server.accept();
                    s.setSoTimeout(TIMEOUT);
                    ObjectInputStream in = new ObjectInputStream(s.getInputStream());
                    Long hash = (Long) in.readObject();
                    Data d = fileHandler.recoverSingleChunk(hash);
                    if (d != null) {
                        LinkedList<Data> ret = new LinkedList<Data>();
                        ret.add(d);
                        Thread sender = new Thread(new ChunkSender(ret, s.getInetAddress().getHostAddress(), 1252));
                    }
                } catch (ClassNotFoundException|IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
