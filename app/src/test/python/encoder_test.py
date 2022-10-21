import unittest
import sys,os
import queue
import threading
import cryptography.hazmat.backends
from cryptography.hazmat.primitives.asymmetric import padding
from cryptography.hazmat.primitives import hashes
import base64
from  cryptography.hazmat.primitives import serialization
def find_git_root():
    curr_path=__file__
    while not os.path.exists(os.path.join(curr_path,".git")):
        curr_path=os.path.dirname(curr_path)
    return curr_path
sys.path.insert(1, os.path.join(find_git_root(),"app/src/main/python/"))
import encoder,asymetric_cipher,key_providers



sample_data={
    "messageType":1,
    "messageId":2,
    "toH":1,
    "toL":1,
    "fromH":10,
    "fromL":18,
    "time":5112,
    "authentication":base64.b64encode(key_providers.SymetricKeyProvider().get_key(None,None)).decode()
}
class StubServer:
    def __init__(self) -> None:
        self.queue=queue.SimpleQueue()
        self.other:StuBServer=None #type: ignore
    def send(self,data:bytes):
        self.other.queue.put(data)
    def receive(self):
        return self.queue.get()

sample_data_copy=sample_data.copy()
class StubKeyProvider(encoder.KeyProvider):
    def get_key(self,key_id: str, mapper):
        return bytearray(range(16))
class MyTest(unittest.TestCase):
    def test_data_processing(self):
        array=encoder.convert_message_to_xml(sample_data,"UserMessage",key_providers.SymetricKeyProvider())
        sample_data_copy=encoder.convert_xml_to_message(array,"UserMessage",key_providers.SymetricKeyProvider())
        self.assertEqual(sample_data,sample_data_copy)
 
    def test_signup(self):
        client=StubServer()
        server=StubServer()
        client.other=server
        server.other=client
        key_provider_client=key_providers.AsymetricKeyProvider()
        key_provider_client.found_keys["USER_PASSWORD"]="test123".ljust(16).encode()
        key_provider_client.found_keys["DECRYPTION_KEY"]= os.urandom(16)

        def server_thread_handler():
            signUpData=server.receive()
            key_provider_server=key_providers.AsymetricKeyProvider()
            requestMessage=encoder.convert_xml_to_message(signUpData,"SignUpMessage",key_provider_server)
            assert requestMessage["USER_PASSWORD"]==b"test123".ljust(16) and requestMessage["userName"]=="compf"
            key_provider_server.found_keys["USER_PASSWORD"]=requestMessage["USER_PASSWORD"]
            key_provider_server.found_keys["DECRYPTION_KEY"]=requestMessage["DECRYPTION_KEY"]
            responseMessage={"messageType":6,"userH":1,"userL":1,"time":60}
            bytesToSent=encoder.convert_message_to_xml(responseMessage,"SignUpMessageResponse",key_provider_server)
            server.send(bytesToSent)
        signUpTestData={"messageType":5,"time":18123,"userName":"compf","USER_PASSWORD":"test123"}
        server_thread=threading.Thread(target=server_thread_handler)
        signupRequestBytes=encoder.convert_message_to_xml(signUpTestData,"SignUpMessage",key_provider_client)
        client.send(signupRequestBytes)
        server_thread.run()
        response_bytes=client.receive()
        response=encoder.convert_xml_to_message(response_bytes,"SignUpMessageResponse",key_provider_client)
        assert response["userL"]==1 and response["userH"]==1



if __name__ == '__main__':
    unittest.main()