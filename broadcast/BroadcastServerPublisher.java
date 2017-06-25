package broadcast;

import javax.xml.ws.Endpoint;

public class BroadcastServerPublisher {
  public static void main(String[] args) {
    Endpoint.publish("http://" + Config.wsIp + ":9876/p2pfs",
    new BroadcastServerImpl());
  }
}
