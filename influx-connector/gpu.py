import json
import time
import pytz
import psutil
import GPUtil
from datetime import datetime
from influxdb_client import InfluxDBClient
from influxdb_client.client.write_api import SYNCHRONOUS
from influxdb_client.client.write_api import ASYNCHRONOUS

def write_data(url, token, org, bucket, measurement, tag):
    with InfluxDBClient(url=url, token=token, org=org, debug=False) as client:
        with client.write_api(write_options=ASYNCHRONOUS) as write_api:
            try:
                while True:
                    data = {
                        "measurement": measurement[0], 
                        "tags": {
                            "use_case": tag, 
                            "sensor_name": "gpu_t_sensor"
                        },
                        "fields": {
                            "value":float(GPUtil.getGPUs()[0].temperature)
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
            'use_case2'
            )
