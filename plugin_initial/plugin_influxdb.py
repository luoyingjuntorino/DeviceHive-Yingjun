import os
import json
from devicehive_plugin import Plugin, Handler


class InfluxDBHandler(Handler):
    def __init__(self, api, config):
        super(InfluxDBHandler, self).__init__(api)
        self.config = config
        self.auth_url = config['auth_url']
        self.login = config['login']
        self.password = config['password']
        self.auth_headers = {'Content-Type': 'application/json'}
        self.auth_payload = json.dumps({"login": self.login, "password": self.password})
        self.network_url = config['network_url']
        self.plugin_url = config['plugin_url']
        self.InfluxPluginName = config['influxPluginName']
        self.influxPluginTopicName = config['influxPluginTopicName']
        
    def handle_connect(self):
        print(f'Successfully connected to {self.InfluxPluginName}...')

    def handle_event(self, event):
        if event.action=="notification/insert":
            self.action = "notification/insert"

    def handle_command_insert(self, command):
        print(command.command)

    def handle_command_update(self, command):
        print(command.command)

    def handle_notification(self, notification):
        device_id= notification.device_id
        if self.action == "notification/insert":
            self.action = ""
            parameters = notification.parameters
            print(parameters)

def run_updater():
    config_file = os.path.join(".", "plugin_initial", "config_plugin_test.json")
    config_json = json.load(open(config_file))
    Manager = Plugin(InfluxDBHandler, config_json)
    Manager.connect(
                    proxy_endpoint = config_json['proxyEndpoint'], 
                    topic_name = config_json['influxPluginTopicName'], 
                    auth_url = config_json['base_url'],
                    login = config_json['login'], 
                    password = config_json['password']     
                    )
    
if __name__ == '__main__':
    run_updater()
