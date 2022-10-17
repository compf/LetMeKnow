import struct
from xml.dom.minidom import parse,Node,Element
import os,pathlib,sys
from cryptography.hazmat.primitives import padding
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
import hashlib
import cryptography.hazmat.backends
def find_git_root():
    curr_path=__file__
    while not os.path.exists(os.path.join(curr_path,".git")):
        curr_path=os.path.dirname(curr_path)
    return curr_path


class KeyProvider:
    def get_key(self,key_id:str,mapper):
         pass
def convert_to_bytes(mapper:dict,class_name:str,key_provider:KeyProvider)->bytearray:
    BASE_PATH= os.path.join(find_git_root(),"app/shared_res/")
    with open(BASE_PATH+class_name+".xml") as f:
        doc=parse(f)
        root=doc.documentElement
        prefix=root.getAttribute("typePrefix") if root.hasAttribute("typePrefix") else ""
    return convert_to_bytes_rec(mapper,root,key_provider,prefix)
def get_encryption_agorithm(args,key,iv):
    return (algorithms.AES(key),modes.CBC(iv),padding.PKCS7(128).padder())
def decrypt(array,iv,key_provider,mode,key_id,mapper):
    key = key_provider.get_key(key_id,mapper)
    mode_splitted=mode.split("/")
    algs=get_encryption_agorithm(mode_splitted,key,iv)
    array=algs[2].update(array)+algs[2].finalize()
    cipher = Cipher(algs[0],algs[1],cryptography.hazmat.backends.default_backend())
    decryptor = cipher.decryptor()
    return decryptor.update(array) + decryptor.finalize()


def encrypt (array,key_provider,mode,key_id,mapper):
    key = key_provider.get_key(key_id,mapper)

    iv = os.urandom(16)
    mode_splitted=mode.split("/")
    algs=get_encryption_agorithm(mode_splitted,key,iv)
    array=algs[2].update(array)+algs[2].finalize()
    cipher = Cipher(algs[0],algs[1],cryptography.hazmat.backends.default_backend())

    encryptor = cipher.encryptor()

    return (encryptor.update(array) + encryptor.finalize(),iv)

    #decryptor = cipher.decryptor()

    #decryptor.update(ct) + decryptor.finalize()
   
def hash(array,alg):
    lib=hashlib.sha256()
    lib.update(array)
    return lib.digest()
def get_hash_size(mode):
    return 32
def convert_to_bytes_rec(mapper,root,key_provider,prefix:str):
    objects=[]
    result=bytearray()
    format_string=prefix
    for ele in root.childNodes:
        if ele.nodeType==Node.ELEMENT_NODE:
            if ele.tagName=="single":
                objects.append(mapper[ele.getAttribute("id")])
                format_string+=ele.getAttribute("type")
    result+=struct.pack(format_string,*objects)
    if len(root.getElementsByTagName("block"))>0:
        ele=root.getElementsByTagName("block")[0]
        block_bytearray=convert_to_bytes_rec(mapper,ele,key_provider,prefix)
        size_type=ele.getAttribute("sizeType")
        if ele.getAttribute("mode")!="plain":
            encrypted=encrypt(block_bytearray,key_provider,ele.getAttribute("mode"),ele.getAttribute("keyId"),mapper)
            block_bytearray=encrypted[0]
            result+=encrypted[1]
        result+=struct.pack(size_type,len(block_bytearray))
        result+=block_bytearray
        if root.hasAttribute("hashType"):
            result+=hash(result,root.getAttribute("hashType"))
    return result
def get_block_size():
    return 16
def convert_to_message(array:bytearray,class_name:str,key_provider:KeyProvider)->dict:
    BASE_PATH= os.path.join(find_git_root(),"app/shared_res/")
    with open(BASE_PATH+class_name+".xml") as f:
        doc=parse(f)
        mapper=dict()
        root=doc.documentElement
        prefix=root.getAttribute("typePrefix") if root.hasAttribute("typePrefix") else ""
    convert_to_message_rec(array,0,key_provider,mapper,root,prefix)
    return mapper
def convert_to_message_rec(array:bytearray,offset:int,key_provider:KeyProvider,mapper:dict,root:Element,prefix:str):
    curr_offset=offset
    format_string=prefix
    for n in root.childNodes:
        if n.nodeType==Node.ELEMENT_NODE and n.tagName=="single":
            format_string+=n.getAttribute("type")
    objects=struct.unpack(format_string,array[offset:offset+struct.calcsize(format_string)])
    counter=0
    for n in root.childNodes:
        if n.nodeType==Node.ELEMENT_NODE and n.tagName=="single":
            mapper[n.getAttribute("id")]=objects[counter]
            counter+=1
    curr_offset=offset+struct.calcsize(format_string)
    if len(root.getElementsByTagName("block"))>0:
        ele=root.getElementsByTagName("block")[0]
        iv=bytearray(get_block_size())
        size_type=ele.getAttribute("sizeType")
        if ele.getAttribute("mode")!="plain":
            iv=array[curr_offset:curr_offset+len(iv)]
            curr_offset+=len(iv)
        size=struct.unpack(size_type,array[curr_offset:curr_offset+struct.calcsize(size_type)])[0]
        curr_offset+=struct.calcsize(size_type)
        block_bytearray=array[curr_offset:curr_offset+size]
        curr_offset+=size
        if root.hasAttribute("hashType"):
            hash_bytearray=array[curr_offset:curr_offset+get_hash_size(root.getAttribute("hashType"))]
            processed_count=curr_offset-offset
            compare_bytearray=array[offset:curr_offset]
            hashed=hash(compare_bytearray,root.getAttribute("hashType"))
            if hashed!=hash_bytearray:
                raise ValueError("Hash not equal")
        if ele.getAttribute("mode")!="plain":
            curr_offset-=len(block_bytearray)
            block_bytearray=decrypt(block_bytearray,iv,key_provider,ele.getAttribute("mode"),ele.getAttribute("keyId"),mapper)
            array[curr_offset:curr_offset+len(block_bytearray)]=block_bytearray
        convert_to_message_rec(array,curr_offset,key_provider,mapper,ele,prefix)
