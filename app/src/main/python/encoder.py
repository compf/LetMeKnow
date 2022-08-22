import struct
import json
def get_struct_string(json_path:str,message_name:str)->str:
    with open(json_path) as f:
        json_obj=json.load(f)
        
