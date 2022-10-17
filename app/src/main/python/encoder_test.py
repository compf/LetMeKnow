import unittest
import encoder
import base64
class StubKeyProvider(encoder.KeyProvider):
    def get_key(self,key_id: str, mapper):
        return bytearray(range(16))
sample_data={
    "messageType":1,
    "messageId":2,
    "toH":1,
    "toL":1,
    "fromH":10,
    "fromL":18,
    "time":5112
}
sample_data_copy=sample_data.copy()
class MyTest(unittest.TestCase):
    def test_data_processing(self):
        array=encoder.convert_to_bytes(sample_data,"UserMessage",StubKeyProvider())
        sample_data_copy=encoder.convert_to_message(array,"UserMessage",StubKeyProvider())
        self.assertEqual(sample_data,sample_data_copy)
    def test_decode_from_base64(self):
        BASE64="AQAKAAAAAAAAABIAAAAAAAAAAQAAAAAAAAABAAAAAAAAAKkfKi6TzBAxiEU0RPMpGK0QAAAAu256jTbjkppnCxAx/7bcuThmboQLi8tqikMekZoKO5RqyFSfgBIHBTSiZx6bOMvN"
        array=bytearray(base64.b64decode(BASE64))
       
        sample_data_copy=encoder.convert_to_message(array,"UserMessage",StubKeyProvider())
        self.assertEqual(sample_data,sample_data_copy)




if __name__ == '__main__':
    unittest.main()