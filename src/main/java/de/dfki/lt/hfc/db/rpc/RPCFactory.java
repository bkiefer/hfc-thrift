/*
 * The Creative Commons CC-BY-NC 4.0 License
 *
 * http://creativecommons.org/licenses/by-nc/4.0/legalcode
 *
 * Creative Commons (CC) by DFKI GmbH
 *  - Christian Bürckert <Christian.Buerckert@DFKI.de>
 *  - Yannick Körber <Yannick.Koerber@DFKI.de>
 *  - Magdalena Kaiser <Magdalena.Kaiser@DFKI.de>

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package de.dfki.lt.hfc.db.rpc;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TSocket;

/**
 *
 * @author Christian.Buerckert@DFKI.de
 */
public class RPCFactory {

  private static Object construct(Class<?> clazz, Object... args)
      throws InstantiationException, IllegalAccessException,
      IllegalArgumentException, InvocationTargetException {
    for (Constructor c : clazz.getConstructors()) {
      if (c.getParameterTypes().length == args.length) {
        boolean fit = true;
        for (int i = 0; i < args.length; i++) {
          if (!c.getParameterTypes()[i].isInstance(args[i])) {
            fit = false;
            break;
          }
        }
        if (fit) {
          return c.newInstance(args);
        }
      }
    }
    throw new RuntimeException("No suitable Constructor found");
  }

  /**
   * Creates a Synchronous Service Client based on a Thrift Client Interface
   * Class.
   *
   * @param <ServiceClient>
   * @param clientClazz provide ServiceName.Client.class
   * @param host provide a host
   * @param port provide a port
   * @return
   */
  public static <ServiceClient> ServiceClient createSyncClient(
      Class<ServiceClient> clientClazz, String host, int port) {
    try {
      TSocket socket = new TSocket(host, port);
      TBinaryProtocol protocol = new TReconnectingBinaryProtocol(socket);
      return clientClazz.getConstructor(TProtocol.class).newInstance(protocol);
    } catch (Exception e) {
      throw new RuntimeException("Create Synchronous Client failed", e);
    }
  }

  /**
   * Creates an RPC Server for a specific service
   *
   * @param <I>
   * @param serviceClazz
   * @param port
   * @param handler
   * @return
   */
  public static <I> TServer createSyncServer(Class<?> serviceClazz, int port,
      I handler) {
    try {
      Class<?> processorClass = Class
          .forName(serviceClazz.getName() + "$Processor");
      TProcessor p = (TProcessor) construct(processorClass, handler);
      TServerTransport transport = new TServerSocket(port);
      TThreadPoolServer server = new TThreadPoolServer(
          new TThreadPoolServer.Args(transport).processor(p));
      return server;
    } catch (Exception e) {
      throw new RuntimeException("Cannot create Server ", e);
    }
  }

}
