'''
works
'''
import logging
from devicehive_plugin import Plugin, Handler
import json
import requests


handler = logging.StreamHandler()
handler.setLevel('DEBUG')
logger = logging.getLogger('devicehive_plugin')
logger.addHandler(handler)
logger.setLevel('DEBUG')

class SimpleHandler(Handler):


    def handle_connect(self):
        print('Successfully connected')

    def handle_event(self, event):
        print(event.action)
        print(type(event.data))

    def handle_command_insert(self, command):
        print(command.command)

    def handle_command_update(self, command):
        print(command.command)

    def handle_notification(self, notification):
        print(notification.notification)

def topic_caller():
    payload2=json.dumps({
        "name":"rafaplugin",
        "description":"rafadescription"
    })
    url = "http://localhost:80/auth/rest/token"

    payload = json.dumps({
    "login": "dhadmin",
    "password": "dhadmin_#911"
    })
    headers = {
    'Content-Type': 'application/json'
    }

    response = requests.request("POST", url, headers=headers, data=payload)
    token=json.loads(response.text)['accessToken']
    token_string=f'Bearer {token}'
    headers=({"Authorization":token_string})
    response=requests.post("http://localhost/plugin/rest/plugin?returnCommands=true&returnUpdatedCommands=true&returnNotifications=true",payload2,headers=headers)
    return response.json()

def main():
    p = Plugin(SimpleHandler)
    url = 'ws://localhost/plugin/proxy/'
    plugin_create=topic_caller()
    #topic_name = 'plugin_topic_ee298c42-37e8-4f31-be26-e42b51fcfd9a'
    plugin_access_token = 'eyJhbGciOiJIUzI1NiJ9.eyJwYXlsb2FkIjp7ImEiOm51bGwsImUiOjE2OTgyMjM1MzE4NTYsInQiOjEsInRwYyI6InBsdWdpbl90b3BpY19lNzk5ODM2OS0wNDc3LTRhMzYtOGQ4ZC1lZGZkNjQ0YmM4MjMifX0.DwAvNwbkfYNcoX3A42QtciXuwUWzVTEimw2_SRCuG_4'
    p.connect(url, plugin_create['topicName'], auth_url='http://localhost:80/api/rest',login='dhadmin',password='dhadmin_#911')

if __name__ == '__main__':
    main()