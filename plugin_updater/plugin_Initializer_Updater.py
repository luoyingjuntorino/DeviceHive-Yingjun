import requests
import json

class Plugin:
    '''
    initializer&updater of influx plugin 
    '''

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
        self.InfluxPluginName = config['InfluxPluginName']
        self.plugin_topic = None  
        
    def get_access_token(self):
        '''
        user login and password to get token
        '''
        response = requests.post(self.auth_url, headers=self.auth_headers, data=self.auth_payload)
        if response.status_code != 201:
            raise Exception("Authentication failed")
        return json.loads(response.text)['accessToken']

    def get_network_ids(self):
        '''
        return list of available networkids
        '''
        network_headers = {'Authorization': 'Bearer ' + self.get_access_token()}
        response = requests.get(self.network_url, headers=network_headers)
        if response.status_code != 200:
            raise Exception("Failed to get network IDs")
        network_ids = [str(ele_['id']) for ele_ in json.loads(response.text)]
        network_ids.sort()
        return network_ids

    def network_ids_str(self):
        '''
        setter of network ids to: networkIds=1%2C2%2C3, preparing for plugin creater&updater api url
        '''
        network_ids_list = self.get_network_ids()
        network_ids_str = "%2C".join(network_ids_list)
        result_str = f"networkIds={network_ids_str}"
        return result_str

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
        obj_name = self.InfluxPluginName
        plugin_list = self.plugin_lister()
        for ele_ in plugin_list:
            if ele_['name'] == obj_name:
                self.plugin_topic = ele_['topicName']
                return self.plugin_topic

    def plugin_inital(self):
        '''
        initialize influx plugin if not exists
        '''
        url = self.plugin_url + "?" + self.network_ids_str() + "&returnCommands=true&returnUpdatedCommands=true&returnNotifications=true&status=ACTIVE"
        headers = {
                    "Content-Type": "application/json",
                    "Accept": "application/json",
                    "Authorization": "Bearer " + self.get_access_token()
                  }
        data = {
                "name": self.InfluxPluginName,
                "description": "Description of " + self.InfluxPluginName
               }
        response = requests.post(url+url, headers=headers, json=data)

        if response.status_code == 201:
            print("Success")
            return json.loads(response.text)['topicName']
        else:
            print("Failed", response.status_code)
            return False

    def set_plugin_status(self, status):
        '''
        update plugin status
        before update plugin have to set plugin status to inactive
        '''
        plugin_topic = self.get_plugin_topic()
        plugin_headers = {'Authorization': 'Bearer ' + self.get_access_token()}
        obj_url = f"{self.plugin_url}?topicName={plugin_topic}&status={status}"
        response = requests.put(obj_url, headers=plugin_headers, data='')
        if response.status_code == 204:
            print(f'Set plugin status to {status} successful')
            return True
        else:
            print(f'Set plugin status to {status} failed')
            return False

    def plugin_update(self):
        '''
        update target plugin with latest networkIds 
        '''
        plugin_topic = self.get_plugin_topic()
        if plugin_topic is None:
            print("we are going to initializing...")
            intializer = self.plugin_inital()
            if intializer is not None:
                return True
        self.set_plugin_status('INACTIVE')
        plugin_topic = self.get_plugin_topic()
        plugin_headers = {'Authorization': 'Bearer ' + self.get_access_token()}
        obj_url = f"{self.plugin_url}?topicName={plugin_topic}&{self.network_ids_str()}&returnCommands=true&returnUpdatedCommands=true&returnNotifications=true&status=ACTIVE"
        response = requests.put(obj_url, headers=plugin_headers, data='')
        if response.status_code == 204:
            print('Update plugin with latest network IDs successful')
            return True
        else:
            print('Update plugin with latest network IDs failed')
            return False

    def printer(self):
        pass

if __name__ == '__main__':
    config_file = "config_plugin.json"
    config_json = json.load(open(config_file))
    P = Plugin(config_json)
    print(P.plugin_update())
