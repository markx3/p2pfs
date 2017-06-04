package broadcast;

import java.util.LinkedList;
import javax.jws.WebService;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.logging.*;

@WebService (endpointInterface = "broadcast.BroadcastServer")
public class BroadcastServerImpl implements BroadcastServer {
  private static final Logger LOGGER = Logger.getLogger(BroadcastServerImpl.class.getName());
  private LinkedList<String> peers = new LinkedList<>();

  public BroadcastServerImpl() {
    LOGGER.setLevel(Level.ALL);
    ConsoleHandler handler = new ConsoleHandler();
    handler.setFormatter(new SimpleFormatter());
    handler.setLevel(Level.ALL);
    LOGGER.addHandler(handler);

  }

  public boolean helloPeer(String addr) {
    peers.add(addr);
    LOGGER.info("["+addr+"] is now online! :)");
    return true;
  }

  public boolean byePeer(String addr) {
    peers.remove(addr);
    LOGGER.info("["+addr+"] abbandoned us! :(");
    return true;
  }

  public LinkedList<String> getPeers(String addr) {
    LOGGER.info("["+addr+"] want the peer list! :O");
    return peers;
  }

  public String currentTime() {
      Calendar cal = Calendar.getInstance();
      SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
      return "["+sdf.format(cal.getTime())+"] ";
  }
}
