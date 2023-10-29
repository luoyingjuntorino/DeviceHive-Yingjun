import requests
import json

class PluginInitializer():

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

    def pluginLister(self):
        '''
        list plugins
        '''
        pass
    
    def objPluginGetter(self):
        '''
        check if target plugin exists
        '''
        pass
    
    def pluginCreater(self):
        '''
        if target plugin not exists, create it.
        '''
        pass

if __name__ "__main__":