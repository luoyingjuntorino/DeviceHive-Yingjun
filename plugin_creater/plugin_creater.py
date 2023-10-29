import requests
import json
from datetime import datetime

def create_plugin_with_network_ids(config_file):
    with open(config_file, 'r') as config:
        config_data = json.load(config)

    # Authenticate and get access token
    auth_url = config_data['auth_url']
    login = config_data['login']
    password = config_data['password']
    auth_payload = json.dumps({
        "login": login,
        "password": password
    })
    headers = {'Content-Type': 'application/json'}
    response = requests.post(auth_url, headers=headers, data=auth_payload)
    if response.status_code != 201:
        print("Authentication failed")
        return
    accessToken = json.loads(response.text)['accessToken']

    # Get network IDs
    network_url = config_data['listNetwork_url']
    headers = {'Authorization': 'Bearer ' + accessToken}
    response = requests.get(network_url, headers=headers)
    if response.status_code != 200:
        print("Failed to get network IDs")
        return
    networkIds = [str(ele_['id']) for ele_ in json.loads(response.text)]
    networkIds.sort()
    networkIds_str = "%2C".join(networkIds)

    # Create plugin
    result_str = f"networkIds={networkIds_str}"
    ########## not works
    # result_str = 'names=test%2Cdev' 
    ##########
    plugin_url = config_data['createPlugin_url'] + "?" + result_str + "&returnCommands=true&returnUpdatedCommands=true&returnNotifications=true"
    headers = {
        "Content-Type": "application/json",
        "Accept": "application/json",
        "Authorization": "Bearer " + accessToken
    }

    # get plugin create date
    datetime_ = datetime.now().strftime("%Y_%m_%d_%H_%M")
    config_data['plugin_data']['description'] = f'plugin created at {datetime_}'
    data = config_data['plugin_data']
    data["name"] += "_" + "_".join(networkIds)
    response = requests.post(plugin_url, headers=headers, json=data)

    if response.status_code == 201:
        print("Success")
        print("Response:", json.loads(response.text))
    else:
        print("Failed", response.status_code)
        print("Response:", response.text)

if __name__ == "__main__":
    config_file = "config_plugin.json"
    create_plugin_with_network_ids(config_file)
