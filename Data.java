import java.util.*;
import java.io.Serializable;

import broadcast.Peer;

class Data implements Serializable {
	private LinkedList<String> peers = new LinkedList<>();
	private byte[] data = new byte[1024*64];
	private long hash_chunk;

	public Data(byte[] d) {
		this.peers.add("root");
		this.data = d;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public void addPeer(String s) {
		peers.add(s);
	}

	public byte[] getData() {
		return this.data;
	}

	public LinkedList<String> getPeers() {
		return this.peers;
	}

	public long getHashChunk() {
		return this.hash_chunk;
	}

	public void setHashChunk(long hash_chunk) {
		this.hash_chunk = hash_chunk;
	}
}
