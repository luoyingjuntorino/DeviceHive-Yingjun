from devicehive_plugin import Plugin, Handler
import logging

handler = logging.StreamHandler()
handler.setLevel('DEBUG')
logger = logging.getLogger('devicehive_plugin')
logger.addHandler(handler)
logger.setLevel('DEBUG')

class SimpleHandler(Handler):


    # def handle_connect(self):
    #     print('Successfully connected')

    # def handle_event(self, event):
    #     print(event.data)

    def handle_command_insert(self, command):
        print(command.command)

    def handle_command_update(self, command):
        print(command.command)

    def handle_notification(self, notification):
        print(notification.parameters)

def main(url, topic_name, auth_url, refresh_token):
    plugin = Plugin(SimpleHandler)
    plugin.connect(url, topic_name, auth_url=auth_url, refresh_token=refresh_token)

if __name__ == '__main__':
    url = 'ws://localhost/plugin/proxy'
    topic_name = 'plugin_topic_05c00373-6968-4278-aef7-88c954ce2b01f'
    auth_url = 'http://localhost:80//api/rest'
    refresh_token = 'eyJhbGciOiJIUzI1NiJ9.eyJwYXlsb2FkIjp7ImEiOlswXSwiZSI6MjAxNDMyNjAwMDAwMCwidCI6MCwidSI6MSwibiI6WyIqIl0sImR0IjpbIioiXSwiY3AiOlsiKiJdfX0.vCyRX-NFW-rJ3ChXEpbqRA1ugQezStitk--Xh-m7TH8'
   
    main(url, topic_name, auth_url, refresh_token)