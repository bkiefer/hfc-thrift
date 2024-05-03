from thrift import Thrift
from thrift.protocol import TBinaryProtocol
from thrift.transport import TSocket
from thrift.transport import TTransport

from hfc_thrift.hfc_db import HfcDbService


class HfcClient(HfcDbService.Client):
    def __init__(self, hostname: str, port: int) -> None:
        # Make socket
        self.transport = TSocket.TSocket(hostname, port)

        # Buffering is critical. Raw sockets are very slow
        self.transport = TTransport.TBufferedTransport(self.transport)

        # Wrap in a protocol
        protocol = TBinaryProtocol.TBinaryProtocolAccelerated(self.transport)

        # Create a client to use the protocol encoder
        super().__init__(protocol)

    def connect(self) -> None:
        # Connect!
        self.transport.open()

    def ping(self) -> None:
        super().ping()
        print('ping()')

    # Close!
    def disconnect(self) -> None:
        self.transport.close()


def connect(host: str = 'localhost', port: int = 9090) -> HfcClient:
    """
    classmapping is a map from class uri to (simple) name
    ns is the namespace where new instances are created
    """

    hfc = HfcClient(host, port)
    hfc.connect()
    return hfc


# Only for testing
# start the HFC server first:
# bin/startServer src/test/data/test.yml
if __name__ == '__main__':
    try:
        client = connect('localhost', 7777)
        qr = client.selectQuery('select ?uri where ?uri <rdf:type> <owl:Class> ?_')
        print(qr.table.rows[1][0])  # type: ignore
        client.disconnect()

    except Thrift.TException as tx:
        print('%s' % tx.message)  # type: ignore
