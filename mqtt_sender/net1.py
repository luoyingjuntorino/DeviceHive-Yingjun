import uuid
import json
import time
import random
import paho.mqtt.client as mqtt

SERVER_HOST = 'localhost'
ACCESS_TOKEN = 'eyJhbGciOiJIUzI1NiJ9.eyJwYXlsb2FkIjp7ImEiOlswXSwiZSI6MjAxNDMyNjAwMDAwMCwidCI6MSwidSI6MSwibiI6WyIqIl0sImR0IjpbIioiXSwiY3AiOlsiKiJdfX0.v0F4bnnIwVRn6qCuukhAp0uXUgrQgGJ54HlghBTAWlg'
DEVICE_ID = 'dev1'


class MQTTDemo(object):

    def __init__(self, url, access_token, device_id):
        self._client_id = str(uuid.uuid4())
        self._connected = False
        self._device_id = device_id

        self._client = mqtt.Client(self._client_id)
        self._accessToken = access_token
        self._client.connect(url)
        self._client.on_connect = self._on_connect
        self._client.on_message = self._on_message
        self._client.on_disconnect = self._on_disconnect
        self._client.loop_start()

    def _on_connect(self, client, userdata, flags, rc):
        print('Connected with rc=%s' % rc)
        client.subscribe('dh/response/authenticate@%s' % self._client_id)
        self._publish('dh/request', {
            'action': 'authenticate',
            'token': self._accessToken,
            'requestId': str(uuid.uuid4())
        })

    def _on_message(self, client, userdata, message):
        print('New message: %s' % message.payload)
        js = json.loads(message.payload)
        if js['action'] == 'authenticate':
            if js['status'] == 'success':
                client.subscribe('dh/response/device/save@%s' % self._client_id)
                client.subscribe('dh/response/notification/insert@%s' % self._client_id)
                self._connected = True
            else:
                print("Failed to authenticate.")

    def _on_disconnect(self, client, userdata, rc):
        print('Disconnected with rc=%s' % rc)
        self._connected = False

    def _publish(self, topic, payload):
        payload['requestId'] = str(uuid.uuid4())
        self._client.publish(topic, json.dumps(payload))

    def run(self):
        while not self._connected:
            time.sleep(0.01)
        time.sleep(1.0)

        # self._publish('dh/request', {
        #     'action': 'device/save',
        #     'deviceId': self._device_id,
        #     'device': {
        #         'name': self._device_id
        #     }
        # })

        while self._connected:
            temperature = round(random.uniform(20.0, 30.0), 2)
            humidity = round(random.uniform(40.0, 60.0), 2)
            self._publish('dh/request', {
                'action': 'notification/insert',
                'deviceId': self._device_id,
                'notification': {
                    'notification': 'dev1',
                    'parameters':
                                {
                                    'temperature': float(temperature),
                                    'humidity':float(humidity),
                                }
                }
            })
            time.sleep(10)


if __name__ == '__main__':
    d = MQTTDemo(SERVER_HOST, ACCESS_TOKEN, DEVICE_ID)
    d.run()

'''
2023-10-26 16:49:44 net2 start to publish.
'''