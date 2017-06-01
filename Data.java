import java.util.*;
import java.io.Serializable;

import broadcast.Peer;

class Data implements Serializable {
	private LinkedList<Peer> peers = new LinkedList<>();
	private byte[] data = new byte[1024*64];

	public Data(byte[] d) {
		this.peers.add(new Peer("root"));
		this.data = d;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public void addPeer(Peer p) {
		peers.add(p);
	}

	public byte[] getData() {
		return this.data;
	}

	public LinkedList<Peer> getPeers() {
		return this.peers;
	}
}