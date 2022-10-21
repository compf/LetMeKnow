import struct
from xml.dom.minidom import parse,Node,Element,parseString,Text
import os,pathlib,sys
from cryptography.hazmat.primitives import padding
import cryptography.hazmat.primitives.ciphers
import cryptography.hazmat.primitives.ciphers.algorithms
import cryptography.hazmat.primitives.ciphers.modes
import hashlib
from Cryptodome.Cipher import AES
from Cryptodome.Random import get_random_bytes
from Cryptodome.Util.Padding import pad, unpad
import copy,base64
from typing import Tuple
import cryptography.hazmat.backends

def find_git_root():
    curr_path=__file__
    while not os.path.exists(os.path.join(curr_path,".git")):
        curr_path=os.path.dirname(curr_path)
    return curr_path

def pad(array,size):
    result=[0]*size
    result[:len(array)]=array
    return bytes(result)

class SymetricCipher:
    def encrypt (self,array,mode,key_id,key_provider,mapper):
        iv =os.urandom(self.get_block_size())
        array=pad(array,len(array)+16-len(array)%16)
        mode_splitted=mode.split("/")
        key = key_provider.get_key(key_id,mapper)
        algs=self.get_encryption_agorithm(mode_splitted,key,iv)
        key=key_provider.get_key(key_id,mapper)
        cipher = cryptography.hazmat.primitives.ciphers.Cipher(algs[0],algs[1],cryptography.hazmat.backends.default_backend())
        encryptor = cipher.encryptor()
        return (encryptor.update(array) + encryptor.finalize(),iv)

   
    def decrypt(self,array,iv,mode,key_id,key_provider,mapper):
        mode_splitted=mode.split("/")
        key = key_provider.get_key(key_id,mapper)
        algs=self.get_encryption_agorithm(mode_splitted,key,iv)
        key=key_provider.get_key(key_id,mapper)
        cipher = cryptography.hazmat.primitives.ciphers.Cipher(algs[0],algs[1],cryptography.hazmat.backends.default_backend())
        decryptor = cipher.decryptor()
        return (decryptor.update(array) + decryptor.finalize())

    def get_encryption_agorithm(self,args,key,iv):
        return (cryptography.hazmat.primitives.ciphers.algorithms.AES(key), cryptography.hazmat.primitives.ciphers.modes.CBC(iv),padding.PKCS7(256).padder())
    def get_block_size(self):
        return 16
class KeyProvider:
    def get_key(self,key_id:str,mapper):
         pass
    def get_cipher(self,key_id:str):
        return SymetricCipher()
    def __init__(self) -> None:
        self.found_keys=dict()

def get_encyrption_decryption_keyId(ele:Element)->Tuple[str,str]:
    if ele.hasAttribute("encryptKeyId") and  ele.hasAttribute("decryptKeyId"):
        return  (ele.getAttribute("encryptKeyId") ,  ele.getAttribute("decryptKeyId"))
    elif ele.hasAttribute("encryptKeyId"):
        return (ele.getAttribute("encryptKeyId"),ele.getAttribute("encryptKeyId"))
    elif ele.hasAttribute("decryptKeyId"):
        return (ele.getAttribute("decryptKeyId"),ele.getAttribute("decryptKeyId"))
    else:
        raise Exception("No encryption or decryption key provided")

def hash(array,alg):
    lib=hashlib.sha256()
    lib.update(array)
    return lib.digest()

def get_hash_size(mode):
    return 32

def convert_message_to_xml(mapper:dict,class_name:str,key_provider:KeyProvider)->str:
    BASE_PATH= os.path.join(find_git_root(),"app/shared/")
    with open(BASE_PATH+class_name+".xml") as f:
        doc=parse(f)
        root=doc.documentElement
    convert_message_to_xml_rec(mapper,key_provider,root,doc)
    return root.toxml()



def convert_tostring(value:object,type:str)->str:
    return str(value)

def convert_message_to_xml_rec(mapper,key_provider,root:Element,doc):
    is_encrypted=root.getAttribute("mode")!="plain"
    for ele in root.childNodes:
        if ele.nodeType==Node.ELEMENT_NODE:
            if ele.tagName=="single":
                if is_cryptography_key(ele.getAttribute("id")):
                    value=key_provider.get_key(ele.getAttribute("id"),mapper)
                else:
                    value=convert_tostring(mapper[ele.getAttribute("id")],ele.getAttribute("type")).encode()
                ele.setAttribute("len",str(len(value)))
                if is_encrypted:
                    encryption_key_id=get_encyrption_decryption_keyId(root)[0]
                    cipher=key_provider.get_cipher(encryption_key_id)
                    encrypted=cipher.encrypt(value,ele.getAttribute("mode"),encryption_key_id,key_provider,mapper)
                    value=base64.b64encode(encrypted[0]).decode()
                    ele.setAttribute("iv",base64.b64encode(encrypted[1]).decode())
                if type(value) is not str:
                    value=value.decode()
                ele.appendChild(doc.createTextNode(value))
            elif ele.tagName=="block":
                convert_message_to_xml_rec(mapper,key_provider,ele,doc)
    
def get_block_size():
    return 16

def convert_xml_to_message(xmlStr:str,key_provider:KeyProvider)->dict:
    mapper=dict()
    root=parseString(xmlStr).documentElement
    convert_xml_to_message_rec(key_provider,mapper,root)
    return mapper

def parse_str_value(strVal:str,type:str):
    if type.endswith("s"):
        return strVal
    elif type.upper()=="Q" or type.upper()=="I" or type.upper()=="H":
        return int(strVal)
    else:
        return float(strVal)

def is_cryptography_key(id:str):
    return id=="DECRYPTION_KEY" or id=="USER_PASSWORD"

def convert_xml_to_message_rec(key_provider:KeyProvider,mapper:dict,root:Element):
    is_encrypted=root.getAttribute("mode")!="plain"
    for ele in root.childNodes:
        if ele.nodeType==Node.ELEMENT_NODE:
            if ele.tagName=="single":
                value=ele.childNodes[0].data
                if is_encrypted:
                    value=base64.b64decode(value.encode())
                    iv=base64.b64decode(ele.getAttribute("iv").encode())
                    decryption_key_id=get_encyrption_decryption_keyId(root)[1]
                    cipher=key_provider.get_cipher(decryption_key_id)
                    value=cipher.decrypt(value,iv,ele.getAttribute("mode"),decryption_key_id,key_provider,mapper)
               
                if is_cryptography_key(ele.getAttribute("id")):
                    value=base64.b64decode(value)
                    key_provider.found_keys[ele.getAttribute("id")]=value
                    mapper[ele.getAttribute("id")]=value
                else:
                    if type( value) is not str:
                        value=value.decode()
                    value=parse_str_value(value,ele.getAttribute("type"))
                    mapper[ele.getAttribute("id")]=value

            elif ele.tagName=="block":
                convert_xml_to_message_rec(key_provider,mapper,ele)


    