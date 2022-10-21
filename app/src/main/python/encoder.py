import struct
from xml.dom.minidom import parse,Node,Element,parseString,Text
import os,pathlib,sys
from cryptography.hazmat.primitives import padding
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
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
    #for i in range(len(array)):
        #result[i]=array[i]
    result[:len(array)]=array

    return bytes(result)
class MyCipher:
    def encrypt (self,array,mode,key_id,key_provider,mapper):
        iv =bytearray(16)
        array=pad(array,len(array)+16-len(array)%16)
        mode_splitted=mode.split("/")
        key=key_provider.get_key(key_id,mapper)
        cipher = AES.new(key, AES.MODE_CBC, iv)
        return (cipher.encrypt(array),iv)

   
    def decrypt(self,array,iv,mode,key_id,key_provider,mapper):
        mode_splitted=mode.split("/")
        key=key_provider.get_key(key_id,mapper)
        cipher = AES.new(key, AES.MODE_CBC, iv)
        return cipher.decrypt(array)

    def get_encryption_agorithm(self,args,key,iv):
        return (algorithms.AES(key),modes.CBC(iv),padding.PKCS7(256).padder())
    def get_block_size(self):
        return 16
class KeyProvider:
    def get_key(self,key_id:str,mapper):
         pass
    def get_cipher(self,key_id:str):
        return MyCipher()
    def __init__(self) -> None:
        self.found_keys=dict()





    #decryptor = cipher.decryptor()

    #decryptor.update(ct) + decryptor.finalize()
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
    template=copy.deepcopy(root)
    convert_message_to_xml_rec(mapper,template,key_provider,root,doc)
    root.setAttribute("hash","")
    str_value=root.toxml()
    hashVal=base64.b64encode(hash(str_value.encode(),root.getAttribute("hashType"))).decode()


    root.setAttribute("hash",hashVal)
    return root.toxml()
def find_matching_ele(template_ele:Element,root:Element)->Element:
    assert template_ele.localName is not None
    elements=[e for e in root.getElementsByTagName(template_ele.localName) if e.nodeType==Node.ELEMENT_NODE and e.getAttribute("id")==template_ele.getAttribute("id") ]
    return elements[0]
def convert_tostring(value:object,type:str)->str:
    return str(value)
def convert_message_to_xml_rec(mapper,template,key_provider,root:Element,doc):
    is_encrypted=template.getAttribute("mode")!="plain"
    for ele in template.childNodes:
        if ele.nodeType==Node.ELEMENT_NODE:
            matching_ele=find_matching_ele(ele,root)
            if ele.tagName=="single":
                if is_cryptography_key(ele.getAttribute("id")):
                    value=key_provider.get_key(ele.getAttribute("id"),mapper)
                else:
                    value=convert_tostring(mapper[ele.getAttribute("id")],ele.getAttribute("type")).encode()
                matching_ele.setAttribute("len",str(len(value)))
                if is_encrypted:
                    encryption_key_id=get_encyrption_decryption_keyId(root)[0]
                    cipher=key_provider.get_cipher(encryption_key_id)
                    encrypted=cipher.encrypt(value,ele.getAttribute("mode"),encryption_key_id,key_provider,mapper)
                    value=base64.b64encode(encrypted[0]).decode()
                    matching_ele.setAttribute("iv",base64.b64encode(encrypted[1]).decode())
                if type(value) is not str:
                    value=value.decode()
                matching_ele.appendChild(doc.createTextNode(value))
            elif ele.tagName=="block":
                convert_message_to_xml_rec(mapper,ele,key_provider,matching_ele,doc)
    
def get_block_size():
    return 16
def convert_xml_to_message(xmlStr:str,class_name:str,key_provider:KeyProvider)->dict:
    BASE_PATH= os.path.join(find_git_root(),"app/shared/")
    with open(BASE_PATH+class_name+".xml") as f:
        doc=parse(f)
        mapper=dict()
        template=doc.documentElement
        root=parseString(xmlStr).documentElement

    convert_xml_to_message_rec(template,key_provider,mapper,root)
    hashVal=root.getAttribute("hash")
    root.setAttribute("hash","")
    root_str=root.toxml()
    compHash=base64.b64encode(hash(root_str.encode(),root.getAttribute("hashType"))).decode()

    if False:
        raise ValueError("Hashes are not equal")
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
def convert_xml_to_message_rec(template:Element,key_provider:KeyProvider,mapper:dict,root:Element):
    is_encrypted=template.getAttribute("mode")!="plain"

    for ele in root.childNodes:
        if ele.nodeType==Node.ELEMENT_NODE:
            matching_ele=find_matching_ele(ele,root)
            if ele.tagName=="single":
                value=ele.childNodes[0].data
                if is_encrypted:
                    value=base64.b64decode(value.encode())
                    iv=base64.b64decode(ele.getAttribute("iv").encode())
                    decryption_key_id=get_encyrption_decryption_keyId(root)[1]
                    cipher=key_provider.get_cipher(decryption_key_id)
                    value=cipher.decrypt(value,iv,ele.getAttribute("mode"),decryption_key_id,key_provider,mapper)
                    val_length=int(ele.getAttribute("len"))
                    #value=value[:val_length]
               
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
                convert_xml_to_message_rec(matching_ele,key_provider,mapper,ele)


    