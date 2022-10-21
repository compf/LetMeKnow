import encoder,asymetric_cipher
from  cryptography.hazmat.primitives import serialization
import cryptography.hazmat.backends

class SymetricKeyProvider(encoder.KeyProvider):
    
    def get_key(self,key_id: str, mapper):
        return self.found_keys[key_id]
class AsymetricKeyProvider(SymetricKeyProvider):
    def __init__(self) -> None:
        super().__init__()
    def get_cipher(self,key_id):
        if key_id=="SERVER_PUBLIC" or key_id=="SERVER_PRIVATE":
            return asymetric_cipher.AsymetricCipher()
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
        else:
            return self.found_keys[key_id]