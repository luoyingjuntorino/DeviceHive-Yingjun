import os
import pytz
import warnings
from influxdb_client import InfluxDBClient
from influxdb_client.client.warnings import MissingPivotFunction

warnings.simplefilter("ignore", MissingPivotFunction)

token = "FAJAw3GAnDDEJe7inzqEfBOPSDE6CimObKf0stTW6B6aEuv05NZw5bSE0QgJZQmpGUC_7b16v4QLcLIJR41mKw=="
org = "polito"
client = InfluxDBClient(url="http://localhost:8086", token=token, org=org, debug=False)
bucket = "iot"
start_time = "2023-11-07T15:30:00.000000Z"
stop_time = "2023-11-07T15:40:00.000000Z"
cpu_list = ['cpu0', 'cpu1', 'cpu2', 'cpu3', 'cpu4', 'cpu5', 'cpu6', 'cpu7', 'cpu8', 'cpu9']
filter_expression = ' or '.join([f'r["sensor_name"] == "{sensor}"' for sensor in cpu_list])
export_type = ''
export_type = 'useCase1' #by use case or by userid: dhuser01
def filter_expression():
       pass
def query(self, bucket, start_time, stop_time, filter_expression):
        query_ = f'from(bucket: "{bucket}") \
                        |> range(start: {start_time}, stop: {stop_time}) \
                        |> filter(fn: (r) => {filter_expression}) \
                        |> filter(fn: (r) => r["_field"] == "value")'

query_api = client.query_api()
result = query_api.query_data_frame(query=str(query))
df = result[['_time', '_value', '_measurement', 'sensor_name']]

def convert_to_rome_time(row):
        dt = row['_time']
        rome_tz = pytz.timezone('Europe/Rome')
        rome_time = dt.astimezone(rome_tz)
        row['_time'] = rome_time
        return row

df = df.apply(convert_to_rome_time, axis=1)

directory = os.path.abspath('exported_csv')
if not os.path.exists(directory):
    os.makedirs(directory)

start_date_str = start_time.split('T')[0]
stop_date_str = stop_time.split('T')[0]


csv_file_name = f'{export_type}_{start_date_str}_{stop_date_str}.csv'


df.to_csv(os.path.join(directory, csv_file_name), index=False)