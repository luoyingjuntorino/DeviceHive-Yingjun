'''
works
'''
import logging
from devicehive_plugin import Plugin, Handler
'''
http://localhost/plugin/rest/plugin?names=&networkIds=1&returnCommands=false&returnNotifications=true&returnUpdatedCommands=false
http://localhost/plugin/rest/plugin?names=&networkIds=1&returnCommands=true&returnNotifications=true&returnUpdatedCommands=true
'''

# handler = logging.StreamHandler()
# handler.setLevel('DEBUG')
# logger = logging.getLogger('devicehive_plugin')
# logger.addHandler(handler)
# logger.setLevel('DEBUG')


url = 'ws://localhost/plugin/proxy'
'''
plugin_topic_95024c15-cf58-4bad-ba00-3208a43ed933 this topic listen to Net2 and Net default
'''
topic_name = 'plugin_topic_c0c57bd8-e967-48e4-96c2-65f074f8779e' 
# plugin_access_token = 'eyJhbGciOiJIUzI1NiJ9.eyJwYXlsb2FkIjp7ImEiOm51bGwsImUiOjE2OTgyNzAwODI4NTcsInQiOjEsInRwYyI6InBsdWdpbl90b3BpY182MGVjMDc4NC0xNDFhLTRiODYtYWRiYi05ZGNkNDMxZGM4ODAifX0.x5PPmDozaaQB63Oa5Z5KrqDY1j0aUSRlrR34owQE8ak'
auth_url = 'http://localhost:80/api/rest'


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

    def handle_notification(self, notification):
        print(notification.notification)
        print(notification.parameters)
        if notification.notification == '$device-add':
            print('we got new $device-add')
        if notification.notification == '$device-update':
            print('we got new $device-update')


def main():
    p = Plugin(SimpleHandler)
    # p.connect(url, topic_name, plugin_access_token=plugin_access_token)
    p.connect(url, topic_name, auth_url=auth_url,
               login='dhadmin', password='dhadmin_#911')

if __name__ == '__main__':
    main()

'''2023-10-26 16:55:31 rdbms-image-dh_plugin-1      | 2023-10-26 14:55:31.130 [XNIO-2 task-19] INFO  dhadmin c.d.auth.JwtPermissionEvaluator - Successfully checked for permission MANAGE_PLUGIN'''