import requests
import json

class Plugin():

    def __init__(self, config):
        '''
        init parameters
        '''
        self.config = config
        self.authApi_url = config['auth_url']
        self.login = config['login']
        self.password = config['password']
        self.authApi_headers = {'Content-Type': 'application/json'}
        self.authApi_payload = json.dumps({ "login": self.login, "password": self.password})
        self.networkApi_url =  config['listNetwork_url']
        self.PluginApi_url = config['createPlugin_url']
    
    def accessTokenGetter(self):
        '''
        user login and password to get token
        '''
        response = requests.post(self.authApi_url, headers=self.authApi_headers, data=self.authApi_payload)
        if response.status_code != 201:
            print("Authentication failed")
            return False
        accessToken = json.loads(response.text)['accessToken']
        return accessToken

    def networkidsGetter(self):
        '''
        return list of available networkids
        '''
        # accessToken = self.accessTokenGetter()
        networkApi_headers = {'Authorization': 'Bearer ' + self.accessTokenGetter()}
        response = requests.get(self.networkApi_url, headers=networkApi_headers)
        if response.status_code != 200:
            print("Failed to get network IDs")
            return False
        networkIds = [str(ele_['id']) for ele_ in json.loads(response.text)]
        networkIds.sort()
        return networkIds
    
    def networkIdsStr(self):
        '''
        setter of network ids to: networkIds=1%2C2%2C3, preparing for plugin updater api url
        '''
        # networkIds_list = type(self.networkidsGetter())
        
        networkIds_str = "%2C".join(self.networkidsGetter())
        result_str = f"networkIds={networkIds_str}"
        return result_str
  
    def pluginLister(self):
        '''
        list plugins
        '''
        pluginApi_headers = {'Authorization': 'Bearer ' + self.accessTokenGetter()}
        response = requests.request("GET", self.PluginApi_url, headers=pluginApi_headers, data='')
        return json.loads(response.text)
    
    def pluginTopicGetter(self):
        '''
        get influx plugin's topic name
        '''
        obj_name = 'plugin_with_Networkids_1_2'
        plugin_list = self.pluginLister()
        for ele_ in plugin_list:
            if ele_['name'] == obj_name:
                return ele_['topicName']

    def pluginDeactive(self):
        '''
        before update plugin have to set plugin status to inactive
        http://localhost:80/plugin/rest/plugin?topicName=plugin_topic_2f08f84f-8947-4507-bfa6-2846984faac5&networkIds=2&returnCommands=true&returnUpdatedCommands=true&returnNotifications=true&status=INACTIVE
        '''
        plugin_topic = self.pluginTopicGetter()
        pluginApi_headers = {'Authorization': 'Bearer ' + self.accessTokenGetter()}
        obj_url = self.PluginApi_url + '?topicName=' + plugin_topic + '&status=INACTIVE'
        response = requests.request("PUT", url=obj_url, headers=pluginApi_headers, data='')
        if response.status_code == 204:
            print('inactive plugin successed')
            return True
        else:
            print('inactive plugin faild')

    def pluginUpdate(self):
        '''
        update target plugin with latest networkIds
        '''
        self.pluginDeactive()
        plugin_topic = self.pluginTopicGetter()
        pluginApi_headers = {'Authorization': 'Bearer ' + self.accessTokenGetter()}
        obj_url = self.PluginApi_url + '?topicName=' + plugin_topic + '&' + self.networkIdsStr() + '&returnCommands=true&returnUpdatedCommands=true&returnNotifications=true&status=ACTIVE'
        response = requests.request("PUT", url=obj_url, headers=pluginApi_headers, data='')
        if response.status_code == 204:
            print('update plugin with latest net. ids successed')
            return True
        else:
            print('update plugin with latest net. ids faild')
    
    def printer(self):
        pass

if __name__ == '__main__':
    config_file = "config_plugin.json"
    config_json = json.load(open(config_file))
    P = Plugin(config_json)
    print(P.pluginUpdate())
