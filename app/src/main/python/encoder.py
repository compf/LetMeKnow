import struct
from xml.dom.minidom import parse,Node,Element,parseString
import os,pathlib,sys
from cryptography.hazmat.primitives import padding
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
import hashlib
import copy,base64
from typing import Tuple
import cryptography.hazmat.backends
def find_git_root():
    curr_path=__file__
    while not os.path.exists(os.path.join(curr_path,".git")):
        curr_path=os.path.dirname(curr_path)
    return curr_path

class MyCipher:
    def encrypt (self,array,mode,key_id,key_provider,mapper):
        iv = os.urandom(16)
        mode_splitted=mode.split("/")
        key=key_provider.get_key(key_id,mapper)

        algs=self.get_encryption_agorithm(mode_splitted,key,iv)
        array=algs[2].update(bytes(array))+algs[2].finalize()
        cipher = Cipher(algs[0],algs[1],cryptography.hazmat.backends.default_backend())

        encryptor = cipher.encryptor()

        return (encryptor.update(bytes(array)) + encryptor.finalize(),iv)
    def decrypt(self,array,iv,mode,key_id,key_provider,mapper):
        mode_splitted=mode.split("/")
        key=key_provider.get_key(key_id,mapper)
        algs=self.get_encryption_agorithm(mode_splitted,key,iv)
        array=algs[2].update(bytes(array))+algs[2].finalize()
        cipher = Cipher(algs[0],algs[1],cryptography.hazmat.backends.default_backend())
        decryptor = cipher.decryptor()
        return decryptor.update(bytes(array)) + decryptor.finalize()
    def get_encryption_agorithm(self,args,key,iv):
        return (algorithms.AES(key),modes.CBC(iv),padding.PKCS7(128).padder())
    def get_block_size(self):
        return 16
class KeyProvider:
    def get_key(self,key_id:str,mapper):
         pass
    def get_cipher(self,key_id:str):
        return MyCipher()





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
    convert_message_to_xml_rec(mapper,template,key_provider,root)
    root.setAttribute("hash","")
    str_value=root.toxml()
    hashVal=base64.encodebytes(hash(str_value.encode(),root.getAttribute("hashType"))).decode()
    print()
    print("to xml",hashVal,root.toxml())
    root.setAttribute("hash",hashVal)
    return root.toxml()
def find_matching_ele(template_ele:Element,root:Element)->Element:
    assert template_ele.localName is not None
    elements=[e for e in root.getElementsByTagName(template_ele.localName) if e.nodeType==Node.ELEMENT_NODE ]
    return elements[0]
def convert_tostring(value:object,type:str)->str:
    return str(value)
def convert_message_to_xml_rec(mapper,template,key_provider,root:Element):
    is_encrypted=template.getAttribute("mode")!="plain"
    for ele in template.childNodes:
        if ele.nodeType==Node.ELEMENT_NODE:
            matching_ele=find_matching_ele(ele,root)
            if ele.tagName=="single":
                value=convert_tostring(mapper[ele.getAttribute("id")],ele.getAttribute("type"))
                if is_encrypted:
                    encryption_key_id=get_encyrption_decryption_keyId(root)[0]
                    cipher=key_provider.get_cipher(encryption_key_id)
                    encrypted=cipher.encrypt(value.encode(),ele.getAttribute("mode"),encryption_key_id,key_provider,mapper)
                    value=base64.encodebytes(encrypted[0]).decode()
                    matching_ele.setAttribute("iv",base64.encodebytes(encrypted[1]).decode())
                matching_ele.nodeValue=value
            elif ele.tagName=="block":
                convert_message_to_xml_rec(mapper,ele,key_provider,matching_ele)
    
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
    compHash=base64.encodebytes(hash(root_str.encode(),root.getAttribute("hashType"))).decode()
    print()
    print("from xml",compHash,root.toxml())

    if compHash!=hashVal:
        raise ValueError("Hashes are not equal")
    return mapper
def parse_str_value(strVal:str,type:str):
    if type.endswith("s"):
        return strVal
    elif type.upper()=="Q" or type.upper()=="I" or type.upper()=="H":
        return int(strVal)
    else:
        return float(strVal)
def convert_xml_to_message_rec(template:Element,key_provider:KeyProvider,mapper:dict,root:Element):
    is_encrypted=template.getAttribute("mode")!="plain"

    for ele in root.childNodes:
        if ele.nodeType==Node.ELEMENT_NODE:
            if ele.tagName=="single":
                value=ele.nodeValue
                if is_encrypted:
                    value=base64.decodebytes(ele.nodeValue)
                    iv=base64.decodebytes(ele.getAttribute("iv"))
                    decryption_key_id=get_encyrption_decryption_keyId(root)[1]
                    cipher=key_provider.get_cipher(decryption_key_id)
                    value=cipher.decrypt(value,iv,ele.getAttribute("mode"),decryption_key_id,key_provider,mapper).decode()
                    value=parse_str_value(value,ele.getAttribute("type"))
                mapper[ele.getAttribute("id")]=value
            elif ele.tagName=="block":
                convert_xml_to_message_rec(template,key_provider,mapper,ele)


    