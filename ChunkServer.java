import java.net.*;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.util.*;
import java.io.*;
import java.nio.file.*;

import broadcast.BroadcastServer;
import broadcast.Peer;

// TODO Restaurar hashtable de chunks p/ persistencia

public class ChunkServer {

    protected Hashtable<Long, Data> chunkHashtable;
    protected int chunkCount;
    private FileHandler fileHandler = new FileHandler();
    private ServerSocket serverConsumer = new ServerSocket(1252);
    private final int TIMEOUT = 5000;

    private final String ip;

    public ChunkServer(String addr) throws IOException, ClassNotFoundException {
        this.ip = addr;
        chunkCount = 0;
        chunkHashtable = fileHandler.recoverChunks();
        Thread receiver = new Thread(new ChunkReceiver());
        Thread listener = new Thread(new RequestListener());
        receiver.start();
        listener.start();
    }


	public boolean deliverChunksToPeers(Metadata mData, LinkedList<String> peers) throws InterruptedException {
		LinkedList<Data> chunks = new LinkedList<>();
        int ccount = 0;
        boolean flag = false;
        for (Data d : mData.getChunks()) {
            chunks.add(d);
        }
		int chunkCount = chunks.size();
		int peerCount = peers.size();
		int cpp = chunkCount/peerCount;
        if (chunkCount % peerCount != 0) flag = true;

        for (int i = 0; i < peerCount; i++) {
            LinkedList<Data> payload = new LinkedList<>();
            String ip1 = peers.get(i);
            String ip2 = null;
            if (i+1 == peerCount) ip2 = peers.get(i-1);
            else ip2 = peers.get(i+1);

            for (int j = 0; j < cpp; j++) {
                if (flag) {
                    j--;
                    flag = false;
                }
                Data tmp = chunks.remove();
                tmp.addPeer(ip1);
                tmp.addPeer(ip2);
                Thread t1 = new Thread(new ChunkSender(tmp, ip1));
                t1.start();
                t1.join();
                Thread t2 = new Thread(new ChunkSender(tmp, ip2));
                t2.start();
                t2.join();
            }
        }


		// for (int i = 0; i < peers.size(); i++) {
        //     System.out.println(i);      //
        //     String ip = peers.get(i);
        //     String ip2 = null;
        //     if (i+1 == peers.size())
        //         ip2 = peers.get(i-1);
        //     else
        //         ip2 = peers.get(i+1);
        //     System.out.println(ip + " " + ip2);
        //     LinkedList<Data> chunksToSend = new LinkedList<Data>();
        //     for (int j = 0; j < cpp; j++) {
        //         Data tmp = chunks.poll();
        //         if (tmp != null)
        //             chunksToSend.add(tmp);
        //     }
        //     if (flag) {
        //         Data tmp = chunks.poll();
        //         if (tmp != null)
        //             chunksToSend.add(tmp);
        //         flag = false;
        //     }
        //     for (Data d : chunksToSend) {
        //         d.addPeer(ip);
        //         d.addPeer(ip2);
        //     }
        //     Thread sender1 = new Thread(new ChunkSender(chunksToSend, ip));
        //     Thread sender2 = new Thread(new ChunkSender(chunksToSend, ip2));
        //     sender1.start();
        //     sender2.start();
        //     sender1.join();
        //     sender2.join();
                return true;
		}


    public Data requestChunk(long hash_chunk, String ip) throws ClassNotFoundException, IOException {
        try {
            System.out.println("request to " + ip);
            Socket s = new Socket(ip, 1251);
            s.setSoTimeout(TIMEOUT); // 5s timeout
            ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
            out.writeObject(hash_chunk);
            out.flush();
            } catch (IOException e) {}
        return requestConsumer();

    	}

    private Data requestConsumer() throws IOException, SocketException, ClassNotFoundException, IOException {
        Data chunk = null;

        Socket s = serverConsumer.accept();
        s.setSoTimeout(TIMEOUT); // 5s timeout
        ObjectInputStream in = new ObjectInputStream(s.getInputStream());
        chunk = (Data) in.readObject();

        return chunk;
    }

    private void storeChunk(Data chunk) throws IOException, ClassNotFoundException {
            chunkHashtable.put(chunk.getHashChunk(), chunk);
            fileHandler.serializeData(chunk);
    }

    private class ChunkSender implements Runnable {
        Thread runner;
        Data chunk;
        String ip;
        int port;

        public ChunkSender(Data chunk, String ip) {
            this.chunk = chunk;
            this.ip = ip;
            runner = new Thread(this);
            runner.start();
            port = 1250;
        }

        public ChunkSender(Data chunk, String ip, int port) {
            this.chunk = chunk;
            this.ip = ip;
            runner = new Thread(this);
            runner.start();
            this.port = port;
        }

        public void run() {

                try {
                    Socket s = new Socket(ip, port);
                    s.setSoTimeout(TIMEOUT);
                    ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
                    out.writeObject(chunk);
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
                    Data chunks = (Data) in.readObject();
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
                    Data d = chunkHashtable.get(hash);
                    Thread sender = new Thread(new ChunkSender(d, s.getInetAddress().getHostAddress(), 1252));
                    sender.start();
                } catch (ClassNotFoundException|IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
