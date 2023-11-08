import json
import time
import psutil
from influxdb_client import InfluxDBClient, Point
from influxdb_client.client.write_api import ASYNCHRONOUS

def write_data(url, token, org, bucket, measurement):
    with InfluxDBClient(url=url, token=token, org=org, debug=False) as client:
        with client.write_api(write_options=ASYNCHRONOUS) as write_api:
            try:
                while True:
                    cpu_percent_per_cpu = psutil.cpu_percent(interval=1, percpu=True)
                    for cpu, usage in enumerate(cpu_percent_per_cpu):
                        data_point = Point(measurement).tag("sensor_id", f"cpu{cpu}").field("value", usage)
                        data_point = {
                                        "measurement": measurement[0], 
                                        "tags": { "sensor_name": f"cpu{cpu}"},
                                        "fields": {"value": usage}

                                     }
                        write_api.write(bucket=bucket[0], record=data_point)

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
            config['influx_endPoint'],
            config['influx_token'],
            config['influx_org'],
            config['influx_bucket'],
            config['influx_measurement']
            )
