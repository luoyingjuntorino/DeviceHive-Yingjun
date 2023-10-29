import json
import time
import GPUtil
import psutil
from influxdb_client import InfluxDBClient
from influxdb_client.client.write_api import SYNCHRONOUS

def write_data(url, token, org, bucket, measurement, fileds):
    with InfluxDBClient(url=url, token=token, org=org, debug=False) as client:
        with client.write_api(write_options=SYNCHRONOUS) as write_api:
            try:
                while True:
                    data = {
                        "measurement": measurement[0], 
                        "fields": {
                            fileds[0]: float(GPUtil.getGPUs()[0].temperature), 
                            fileds[1]: float(psutil.cpu_percent())
                        }
                    }
                    write_api.write(bucket=bucket[0], record=data)
                    time.sleep(5)
            except KeyboardInterrupt:  
                print("Exiting the loop.")

if __name__ == "__main__":
    try:
        config = json.load(open('influxdb_config.json'))
    except FileNotFoundError:
        print(f"Error: The file 'influxdb_config.json' does not exist.")
    else:
        print("InfluxDB connector is running...")
        write_data(
            config['url'],
            config['token'],
            config['org'],
            config['bucket'],
            config['measurement'],
            config['fields']
            )
