'''
works
'''
import logging
from devicehive_plugin import Plugin, Handler


handler = logging.StreamHandler()
handler.setLevel('DEBUG')
logger = logging.getLogger('devicehive_plugin')
logger.addHandler(handler)
logger.setLevel('DEBUG')


# url = 'wss://playground.devicehive.com/plugin/proxy'
url = 'ws://localhost/plugin/proxy'
topic_name = 'plugin_topic_2f08f84f-8947-4507-bfa6-2846984faac5'
auth_url = 'http://localhost:80/api/rest'
access_token = 'eyJhbGciOiJIUzI1NiJ9.eyJwYXlsb2FkIjp7ImEiOlswXSwiZSI6MjAxNDMyNjAwMDAwMCwidCI6MSwidSI6MSwibiI6WyIqIl0sImR0IjpbIioiXSwiY3AiOlsiKiJdfX0.v0F4bnnIwVRn6qCuukhAp0uXUgrQgGJ54HlghBTAWlg'



class SimpleHandler(Handler):
    # def handle_connect(self):
    #     print('Successfully connected')

    # def handle_event(self, event):
    #     print(event.action)
    #     print(type(event.data))

    # def handle_command_insert(self, command):
    #     print(command.command)

    # def handle_command_update(self, command):
    #     print(command.command)

    # def handle_notification(self, notification):
    #     print(notification.notification)
    def handle_command_insert(self, command):
        print(command.command)

    def handle_command_update(self, command):
        print(command.command)

    def handle_notification(self, notification):
        print(notification.parameters)
        print(notification.notification)
        print(notification.device_id)
        
def main():
    p = Plugin(SimpleHandler)
    p.connect(url, topic_name, auth_url=auth_url,
               access_token=access_token)
    


if __name__ == '__main__':
    main()