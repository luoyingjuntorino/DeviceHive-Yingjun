import json
import os
import requests

class PluginsInitializer():
    '''
    initializing ListenerPlugin and InfluxPlugin
    '''
    def __init__(self, config_file):
        '''
        init parameters
        '''
        self.config_file = config_file
        with open(config_file, 'r') as file:
            self.config = json.load(file)
        self.login = self.config["login"]
        self.password = self.config["password"]
        self.auth_url = self.config["auth_url"]
        self.plugin_url = self.config["plugin_url"]
        self.influxPluginName = self.config["influxPluginName"]
        self.listenerPluginName = self.config["listenerPluginName"]
        self.auth_payload = json.dumps({"login": self.login, "password": self.password})

    def accessToker_getter(self):
        '''
        user login and password to get token
        '''
        headers = {'Content-Type': 'application/json'}
        response = requests.post(self.auth_url, headers=headers, data=self.auth_payload)
        if response.status_code != 201:
            raise Exception("Authentication failed")
        return json.loads(response.text)['accessToken']
        
    def influx_plugin_init(self):
        '''
        initializing influx plugin
        '''
        url = self.plugin_url + "?networkIds=1&returnCommands=true&returnUpdatedCommands=true&returnNotifications=true&status=ACTIVE"
        headers = {
                    "Content-Type": "application/json",
                    "Accept": "application/json",
                    "Authorization": "Bearer " + self.accessToker_getter()
                  }
        data = {
                "name": self.influxPluginName,
                "description": "Description of " + self.influxPluginName
               }
        response = requests.post(url+url, headers=headers, json=data)

        if response.status_code == 201:
            print("Create influx plugin initialize Success")
            topicName = json.loads(response.text)['topicName']
            self.config["influxPluginTopicName"] = topicName
            with open(self.config_file, 'w') as file:
                json.dump(self.config, file, indent=2)
            return True
        else:
            print("Create influx plugin initialize Failed")
            return False

    def listener_plugin_init(self):
        '''
        initializing listener plugin
        '''
        url = self.plugin_url + "?&returnCommands=true&returnUpdatedCommands=true&returnNotifications=true&status=ACTIVE"
        headers = {
                    "Content-Type": "application/json",
                    "Accept": "application/json",
                    "Authorization": "Bearer " + self.accessToker_getter()
                  }
        data = {
                "name": self.listenerPluginName,
                "description": "Description of " + self.listenerPluginName
               }
        response = requests.post(url+url, headers=headers, json=data)

        if response.status_code == 201:
            print("Create listener plugin initialize Success")
            topicName = json.loads(response.text)['topicName']
            self.config["listenerPluginTopicName"] = topicName
            with open(self.config_file, 'w') as file:
                json.dump(self.config, file, indent=2)
            return True
        else:
            print("Create listener plugin initialize Failed")
            return False
        
def run_initializer():
    # config_file = os.path.join(".", "plugin_initial", "config_plugin.json")
    config_file = "config_plugin.json"
    Manager = PluginsInitializer(config_file)
    Manager.influx_plugin_init()
    Manager.listener_plugin_init()
        

if __name__ == "__main__":
    run_initializer()

