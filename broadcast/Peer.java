package broadcast;

import java.util.LinkedList;
import javax.jws.WebService;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.io.Serializable;

public class Peer implements Serializable{
	String addr;
	int timeout;

	public Peer (String addr) {
		this.addr = addr;
		timeout = 0;
	}

	public void incrementTimeout() {
		timeout++;
	}

	public Peer getThis() {
		return this;
	}
}