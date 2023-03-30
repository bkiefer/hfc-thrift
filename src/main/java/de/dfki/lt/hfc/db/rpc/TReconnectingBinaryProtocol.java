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

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.transport.TTransport;

/**
 *
 * @author Christian.Buerckert@DFKI.de
 */
public class TReconnectingBinaryProtocol extends TBinaryProtocol {

  private long closeTime = Long.MAX_VALUE;

  public TReconnectingBinaryProtocol(TTransport trans) {
    super(trans);
  }

  @Override
  public synchronized void writeMessageBegin(TMessage message)
      throws TException {
    if (!trans_.isOpen()) {
      trans_.open();
    }
    super.writeMessageBegin(message);
  }

}
