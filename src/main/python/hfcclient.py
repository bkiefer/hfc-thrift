from hfc_db import HfcDbService
from thrift import Thrift
from thrift.transport import TSocket
from thrift.transport import TTransport
from thrift.protocol import TBinaryProtocol

class HfcClient(HfcDbService.Client):
    def __init__(self, hostname, port):
        # Make socket
        self.transport = TSocket.TSocket(hostname, port)

        # Buffering is critical. Raw sockets are very slow
        self.transport = TTransport.TBufferedTransport(self.transport)

        # Wrap in a protocol
        protocol = TBinaryProtocol.TBinaryProtocolAccelerated(self.transport)

        # Create a client to use the protocol encoder
        super().__init__(protocol)

    def connect(self):
        # Connect!
        self.transport.open()

    def ping(self):
        super().ping()
        print('ping()')

    # Close!
    def disconnect(self):
        self.transport.close()

# Only for testing
# start the HFC server first:
# bin/startServer src/test/data/test.yml 
if __name__ == '__main__':
    try:
        client = HfcClient('localhost', 9090)
        client.connect()
        qr = client.selectQuery('select ?uri where ?uri <rdf:type> <owl:Class> ?_')
        print(qr.table.rows[1][0])
        client.disconnect()

    except Thrift.TException as tx:
        print('%s' % tx.message)