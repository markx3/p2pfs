package broadcast;

import java.util.LinkedList;
import javax.jws.WebService;
import javax.jws.WebMethod;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.Style;

@WebService
@SOAPBinding(style = Style.DOCUMENT)
public interface BroadcastServer {
  @WebMethod boolean helloPeer(String addr);
  @WebMethod boolean byePeer(String addr);
  @WebMethod LinkedList<String> getPeers(String addr);
}
