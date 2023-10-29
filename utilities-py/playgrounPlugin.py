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
plugin_access_token = 'eyJhbGciOiJIUzI1NiJ9.eyJwYXlsb2FkIjp7ImEiOlsxNl0sImUiOjE2OTg1OTY1NjAyNDksInQiOjEsInRwYyI6InBsdWdpbl90b3BpY18yZjA4Zjg0Zi04OTQ3LTQ1MDctYmZhNi0yODQ2OTg0ZmFhYzUifX0.2xSizDcrK6hGDJGn7lFA3naTfrMV1lWeLAtUSwjAReQ'


class SimpleHandler(Handler):
    # def handle_connect(self):
    #     print('Successfully connected')

    def handle_event(self, event):
        print(event.action)
        print(type(event.data))

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
        
def main():
    p = Plugin(SimpleHandler)
    p.connect(url, topic_name, plugin_access_token=plugin_access_token)


if __name__ == '__main__':
    main()