import sys,os
import queue
import threading
import cryptography.hazmat.backends
from cryptography.hazmat.primitives.asymmetric import padding
from cryptography.hazmat.primitives import hashes
import base64
import encoder
from  cryptography.hazmat.primitives import serialization
class AsymetricCipher(encoder.MyCipher):
    def encrypt (self,array,mode,key_id,key_provider,mapper):
        iv = bytearray([0]*16)
        key=key_provider.get_key(key_id,mapper)

        return ( key.encrypt(bytes(array)),iv)

    def decrypt(self,array,iv,mode,key_id,key_provider,mapper):
        iv = bytearray([0]*16)
        key=key_provider.get_key(key_id,mapper)
        return ( key.decrypt(bytes(array)))