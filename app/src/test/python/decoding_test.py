from Cryptodome.Cipher import AES
from Cryptodome.Random import get_random_bytes
from Cryptodome.Util.Padding import pad, unpad
import base64
text='UmOkgf0heGKPI0/QhflM4Q=='
iv=bytearray(16)
key="123".ljust(16,'\0')


decoded=base64.b64decode(text)
cipher = AES.new(key.encode(), AES.MODE_CBC, iv)
decrypted= cipher.decrypt(decoded)
print(decrypted)

