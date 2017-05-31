package broadcast;

import java.util.LinkedList;
import javax.jws.WebService;
import java.util.Calendar;
import java.text.SimpleDateFormat;

@WebService (endpointInterface = "broadcast.BroadcastServer")
public class BroadcastServerImpl implements BroadcastServer {

  private LinkedList<Peer> peers = new LinkedList<>();

  public BroadcastServerImpl() {
    // Se a lista de peers iniciar vazia, dá problema na hora de adicionar o primeiro.
    peers.add(new Peer("root"));
  }

  public boolean helloPeer(String addr) {
    Peer newP = new Peer(addr);
    int timeoutSum = 0;
    for (Peer p : peers)
      timeoutSum += p.timeout;
    newP.timeout = (timeoutSum/peers.size());
    peers.add(newP);
    return true;
  }

  public boolean byePeer(String addr) {
    for (Peer p : peers) {
      if (p.addr == addr)
        peers.remove(p);
        return true;
    }
    return false;
  }

  public LinkedList<String> getPeers(String addr) {
    LinkedList<String> ret = new LinkedList<>();
    Peer tmp = null;
    int timeoutSum = 0;
    for (Peer p : peers) {
      if (p.addr == addr) {
        tmp = p;
        p.timeout++;
      }
      timeoutSum += p.timeout;
      ret.add(p.addr);
    }
    if (tmp.timeout < (timeoutSum/peers.size()) - 1 )
      peers.remove(tmp);
    ret.remove(tmp.addr);
    return ret;
  }

  public String currentTime() {
      Calendar cal = Calendar.getInstance();
      SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
      return "["+sdf.format(cal.getTime())+"] ";
  }
}
