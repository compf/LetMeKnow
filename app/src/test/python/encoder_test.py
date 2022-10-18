import unittest
import sys,os
import queue
import threading
import cryptography.hazmat.backends
from cryptography.hazmat.primitives.asymmetric import padding
from cryptography.hazmat.primitives import hashes
def find_git_root():
    curr_path=__file__
    while not os.path.exists(os.path.join(curr_path,".git")):
        curr_path=os.path.dirname(curr_path)
    return curr_path
sys.path.insert(1, os.path.join(find_git_root(),"app/src/main/python/"))
import encoder
import base64
from  cryptography.hazmat.primitives import serialization
class StubKeyProvider(encoder.KeyProvider):
    def get_key(self,key_id: str, mapper):
        return bytearray(range(16))
class ExtendedKeyProvider(StubKeyProvider):
    def __init__(self) -> None:
        super().__init__()
        self.password="123"
    def get_cipher(self,key_id):
        if key_id=="SERVER_PUBLIC" or key_id=="SERVER_PRIVATE":
            return ExtendedCipher()
        else:
            return super().get_cipher(key_id)
    def get_key(self,key_id,mapper):
        if key_id=="CLIENT_CLIENT":
            return super().get_key(key_id,mapper)
        elif key_id=="SERVER_PRIVATE":
            with open("/home/compf/.ssh/id_rsa","rb") as f:
                key= serialization.load_ssh_private_key(f.read(),password=None,backend=cryptography.hazmat.backends.default_backend())
                return key
        elif key_id=="SERVER_PUBLIC":
            with open("/home/compf/.ssh/id_rsa.pub","rb") as f:
                return serialization.load_ssh_public_key(f.read(),backend=cryptography.hazmat.backends.default_backend())
        elif key_id=="DECRYPTION_KEY":
            return self.password
        else:
            return None

sample_data={
    "messageType":1,
    "messageId":2,
    "toH":1,
    "toL":1,
    "fromH":10,
    "fromL":18,
    "time":5112,
    "authentication":StubKeyProvider().get_key(None,None)
}
class StubServer:
    def __init__(self) -> None:
        self.queue=queue.SimpleQueue()
        self.other:StuBServer=None #type: ignore
    def send(self,data:bytes):
        self.other.queue.put(data)
    def receive(self):
        return self.queue.get()
class ExtendedCipher(encoder.MyCipher):
    def encrypt (self,array,mode,key_id,key_provider,mapper):
        iv = bytearray([0]*16)
        key=key_provider.get_key(key_id,mapper)

        return ( key.encrypt(bytes(array),padding.OAEP(mgf=padding.MGF1(algorithm=hashes.SHA256()),algorithm=hashes.SHA256(),label=None)),iv)

    def decrypt(self,array,iv,mode,key_id,key_provider,mapper):
        iv = bytearray([0]*16)
        key=key_provider.get_key(key_id,mapper)
        return ( key.decrypt(bytes(array),padding.OAEP(mgf=padding.MGF1(algorithm=hashes.SHA256()),algorithm=hashes.SHA256(),label=None)))
sample_data_copy=sample_data.copy()
class MyTest(unittest.TestCase):
    def test_data_processing(self):
        array=encoder.convert_message_to_xml(sample_data,"UserMessage",StubKeyProvider())
        sample_data_copy=encoder.convert_xml_to_message(array,"UserMessage",StubKeyProvider())
        self.assertEqual(sample_data,sample_data_copy)
 
    def test_signup(self):
        client=StubServer()
        server=StubServer()
        client.other=server
        server.other=client
        key_provider_client=ExtendedKeyProvider()
        key_provider_client.password=os.urandom(16)
        def server_thread_handler():
            signUpData=server.receive()
            key_provider_server=ExtendedKeyProvider()
            requestMessage=encoder.convert_xml_to_message(signUpData,"SignUpMessage",key_provider_server)
            key_provider.password=requestMessage["DECRYPTION_KEY"]
            responseMessage={"messageType":6,"userH":1,"userL":1}
            bytesToSent=encoder.convert_message_to_xml(responseMessage,"SignUpMessageResponse",key_provider_server)
            server.send(bytesToSent)
        signUpTestData={"messageType":5,"time":18123,"DECRYPTION_KEY":key_provider_client.password,"userName":"compf".ljust(128).encode(),"authentication":"test123".ljust(128).encode()}
        server_thread=threading.Thread(target=server_thread_handler)
        signupRequestBytes=encoder.convert_message_to_xml(signUpTestData,"SignUpMessage",key_provider_client)
        client.send(signupRequestBytes)
        server_thread.run()
        response_bytes=client.receive()
        response=encoder.convert_xml_to_message(response_bytes,"SignUpMessageResponse",key_provider_client)
        assert response["userL"]==1 and response["userH"]==1



if __name__ == '__main__':
    unittest.main()