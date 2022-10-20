import http.server,sys,os
import struct
def find_git_root():
    curr_path=__file__
    while not os.path.exists(os.path.join(curr_path,".git")):
        curr_path=os.path.dirname(curr_path)
    return curr_path
sys.path.insert(1, os.path.join(find_git_root(),"app/src/main/python/"))
import encoder,asymetric_cipher,key_providers
class MyRequestHandler(http.server.BaseHTTPRequestHandler):
    def do_POST(self):
        print("received")
        length=struct.unpack(">i",self.rfile.read(4))[0]
        print(length)
        input=self.rfile.read(length)
        input=input.decode()
        key_provider=key_providers.AsymetricKeyProvider()
        encoder.convert_xml_to_message(input,"SignUpMessage",key_provider)

        responseMessage={"messageType":6,"userH":1,"userL":1,"time":60}
        xml_str=encoder.convert_message_to_xml(responseMessage,"SignUpMessageResponse",key_provider)
        
        self.write(struct.pack(">i",xml_str))
        self.wfile.write(xml_str.encode())

server=http.server.HTTPServer(("0.0.0.0",1997),MyRequestHandler)
server.serve_forever()