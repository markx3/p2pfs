package broadcast;

import java.util.LinkedList;
import javax.jws.WebService;
import java.util.Calendar;
import java.text.SimpleDateFormat;

@WebService (endpointInterface = "broadcast.BroadcastServer")
public class BroadcastServerImpl implements BroadcastServer {

  private LinkedList<String> peers = new LinkedList<>();

  public BroadcastServerImpl() {
  }

  public boolean helloPeer(String addr) {
    peers.add(addr);
    return true;
  }

  public boolean byePeer(String addr) {
    peers.remove(addr);
    return true;
  }

  public LinkedList<String> getPeers(String addr) {
    return peers;
  }

  public String currentTime() {
      Calendar cal = Calendar.getInstance();
      SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
      return "["+sdf.format(cal.getTime())+"] ";
  }
}
