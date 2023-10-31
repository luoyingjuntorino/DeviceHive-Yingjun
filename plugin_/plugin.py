import json
import requests

class Plugin_listner():

    def __init__(self, config):
        '''
        init parameters
        '''
        self.config = config
        self.auth_url = config['auth_url']
        self.login = config['login']
        self.password = config['password']
        self.auth_headers = {'Content-Type': 'application/json'}
        self.auth_payload = json.dumps({"login": self.login, "password": self.password})
        self.network_url = config['listNetwork_url']
        self.plugin_url = config['createPlugin_url']
        self.plugin_topic = None
        self.pluginListenerName = 'PluginListener'

    def initializer(self):
        '''
        initialize listen plugin
        '''
        url = self.plugin_url + "?"+ "&returnCommands=true&returnUpdatedCommands=true&returnNotifications=true&status=ACTIVE"
        headers = {
                    "Content-Type": "application/json",
                    "Accept": "application/json",
                    "Authorization": "Bearer " + self.get_access_token()
                  }
        data = {
                "name": self.pluginListenerName,
                "description": "Description of " + self.pluginListenerName
               }
        response = requests.post(url+url, headers=headers, json=data)
        if response.status_code == 201:
            return json.loads(response.text)
        else:
            return False
    
    def get_access_token(self):
        '''
        user login and password to get token
        '''
        response = requests.post(self.auth_url, headers=self.auth_headers, data=self.auth_payload)
        if response.status_code != 201:
            raise Exception("Authentication failed")
        return json.loads(response.text)['accessToken']
    
    def get_plugins(self):
        '''
        to get list of plugins
        '''
        pass
    
    def plugin_lister(self):
        '''
        list plugins available
        '''
        plugin_headers = {'Authorization': 'Bearer ' + self.get_access_token()}
        response = requests.get(self.plugin_url, headers=plugin_headers)
        if response.status_code != 200:
            raise Exception("Failed to list plugins")
        return json.loads(response.text)
    
    def get_plugin_topic(self):
        '''
        get influx plugin's topic name
        '''
        if self.plugin_topic is not None:
            return self.plugin_topic
        obj_name = self.listenerPluginName
        plugin_list = self.plugin_lister()
        for ele_ in plugin_list:
            if ele_['name'] == obj_name:
                self.plugin_topic = ele_['topicName']
                return self.plugin_topic
    
if __name__=="__main__":
    config_file = "config_plugin.json"
    config_json = json.load(open(config_file))
    P = Plugin_listner(config_json)
    print(P.plugin_lister())