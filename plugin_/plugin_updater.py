'''
works
'''
import re
import json
import requests
from devicehive_plugin import Plugin, Handler


class SimpleHandler(Handler):
    def __init__(self, api, config):
        super(SimpleHandler, self).__init__(api)
        self.config = config
        self.auth_url = config['auth_url']
        self.login = config['login']
        self.password = config['password']
        self.auth_headers = {'Content-Type': 'application/json'}
        self.auth_payload = json.dumps({"login": self.login, "password": self.password})
        self.network_url = config['listNetwork_url']
        self.plugin_url = config['createPlugin_url']
        self.InfluxPluginName = config['InfluxPluginName']
        self.InfluxPluginTopic = config['InfluxPluginTopic']

    def handle_connect(self):
        print('Successfully connected')

    def handle_command_insert(self, command):
        print(command.command)

    def handle_command_update(self, command):
        print(command.command)

    def handle_notification(self, notification):
        if notification.notification == '$device-add' or notification.notification == '$device-update':
            networkid = notification.parameters['networkId']
            numbers = re.findall(r'\d+', self.get_pluginFilter())
            networkIds = [int(num) for num in numbers]
            if networkid not in networkIds:
                networkIds.append(networkid)
                networkIds.sort()
                network_ids_str = "%2C".join(map(str, networkIds))
                result_str = f"networkIds={network_ids_str}"
                self.inactive_plugin(self.InfluxPluginTopic)
                plugin_headers = {'Authorization': 'Bearer ' + self.get_access_token()}
                url = f"{self.plugin_url}?topicName={self.InfluxPluginTopic}&{result_str}&returnCommands=true&returnUpdatedCommands=true&returnNotifications=true&status=ACTIVE"
                response = requests.put(url, headers=plugin_headers, data='')
                if response.status_code == 204:
                    print(f'Update plugin with latest network IDs {networkIds} successful')
                else:   
                    print('Update plugin with latest network IDs failed')
            else:
                print(f'networkid {networkid} in the networkIds {networkIds}')

    def inactive_plugin(self, pluginTopicName):
        plugin_headers = {'Authorization': 'Bearer ' + self.get_access_token()}
        url = f"{self.plugin_url}?topicName={pluginTopicName}&status=INACTIVE"
        response = requests.put(url, headers=plugin_headers, data='')
        if response.status_code == 204:
            print(f'Set plugin status to INACTIVE successful')
            return True
        else:
            print(f'Set plugin status to INACTIVE failed')
            return False

    def get_access_token(self):
        '''
        user login and password to get token
        '''
        response = requests.post(self.auth_url, headers=self.auth_headers, data=self.auth_payload)
        if response.status_code != 201:
            raise Exception("Authentication failed")
        return json.loads(response.text)['accessToken']
    
    def get_pluginFilter(self):
        '''
        get plugin filter
        '''
        list_plugins = self.list_plugins()
        for ele_ in list_plugins:
            if ele_['name'] == self.InfluxPluginName:
                return ele_['filter']
            
    def list_plugins(self):
        '''
        list plugins
        '''
        plugin_headers = {'Authorization': 'Bearer ' + self.get_access_token()}
        response = requests.get(self.plugin_url, headers=plugin_headers)
        if response.status_code != 200:
            raise Exception("Failed to list plugins")
        return json.loads(response.text)


if __name__ == '__main__':

    config_file = "config_plugin.json"
    config_json = json.load(open(config_file))
    plugin = Plugin(SimpleHandler, config_json)
    plugin.connect(proxy_endpoint = config_json['proxyEndpoint'], 
                   topic_name = config_json['ListenPluginTopic'], 
                   auth_url = config_json['base_url'],
                   login = config_json['login'], 
                   password = config_json['password'])