import os
import pytz
import json
import warnings
from influxdb_client import InfluxDBClient
from influxdb_client.client.warnings import MissingPivotFunction
warnings.simplefilter("ignore", MissingPivotFunction)


class Query_influx():
    '''
    Microservices of InlfuxDB data exporter
    '''
    def __init__(self, config, query_type, sensor_ids, start, stop):
        '''
        init parameters 
        '''
        self.config = config
        self.timezone = self.config["timezone"]
        self.influx_endPoint = self.config["influx_endPoint"]
        self.influx_token = self.config["influx_token"]
        self.influx_org = self.config["influx_org"]
        self.influx_bucket = self.config["influx_bucket"][0]
        self.sub_directory = self.config["sub_directory"]
        self.query_type = query_type
        self.sensor_ids = sensor_ids
        self.start_time = start
        self.stop_time = stop

    def filter_expression(self):
        '''
        To build a filter that able to filter out group of sensors by sensor ids
        '''
        filter = ' or '.join([f'r["sensor_name"] == "{sensor_id}"' for sensor_id in self.sensor_ids])
        return filter
    

    def query_pre(self):
        '''
        prepare query language
        '''
        query_ = f'from(bucket: "{self.influx_bucket}") \
                        |> range(start: {self.start_time}, stop: {self.stop_time}) \
                        |> filter(fn: (r) => {self.filter_expression()}) \
                        |> filter(fn: (r) => r["_field"] == "value")'
        return query_
    
    def query_pro(self):
        '''
        process query
        '''
        client = self.Influx_client()
        query_api = client.query_api()
        result = query_api.query_data_frame(query=str(self.query_pre()))
        result_df = result[['_time', '_value', '_measurement', 'sensor_name']]
        return result_df
    
    def convert_to_rome_time(self, row):
        '''
        function of convert query data timezone from UTC to Pre-setted timezone
        '''
        dt = row['_time']
        rome_tz = pytz.timezone(self.timezone)
        rome_time = dt.astimezone(rome_tz)
        row['_time'] = rome_time
        return row

    def apply_tz_convert(self):
        '''
        apply timezone converter function to dataframe
        '''
        df = self.query_pro()
        df = df.apply(self.convert_to_rome_time, axis=1)
        return df
    
    def csv_exporter(self):
        '''
        save csv localy
        '''
        f_fileName = self.fileName_generator()
        self.mkdir_check()
        df = self.apply_tz_convert()
        df.to_csv(os.path.join(self.sub_directory, f'{f_fileName}.csv'), index=False)
    
    def dataframe_exporter(self):
        '''
        return dataframe with converted timezone 
        '''
        df = self.apply_tz_convert()
        return df
    
    def mkdir_check(self):
        '''
        check dir.
        '''
        directory = os.path.abspath(self.sub_directory)
        if not os.path.exists(directory):
            os.makedirs(directory)
        return True
    
    def fileName_generator(self):
        '''
        generate string of csv file name
        query type can be: specific use Case, or user name maybe?
        '''
        start_date_str = self.start_time.split('T')[0]
        stop_date_str = self.stop_time.split('T')[0]
        file_name = f'{self.query_type}_{start_date_str}_{stop_date_str}'
        return file_name

    def Influx_client(self):
        '''
        connect to influx client
        '''
        client = InfluxDBClient(url=self.influx_endPoint, token=self.influx_token, org=self.influx_org, debug=False)
        return client

# def run():
#     influx_config = "influxdb_config.json"
#     try:
#         config = json.load(open(influx_config))
#     except FileNotFoundError:
#         print(f"Error: The file '{influx_config}.json' does not exist.")
#     '''
#     TODO
#     below infos should get from streamLit, here just for testing
#     '''
#     query_type = "useCase1"
#     sensor_ids = ['cpu0', 'cpu1']
#     start_time = "2023-11-07T21:30:00.000000Z" #UTC
#     stop_time = "2023-11-07T22:30:00.000000Z" #UTC
#     Manager = Query_influx(config, query_type, sensor_ids, start_time, stop_time)
#     Manager.csv_exporter()
#     ''''''
#     df = Manager.dataframe_exporter()
#     df.to_csv(os.path.join('exported_csv', 'checker.csv'), index=False)

# if __name__=="__main__":
#     run()
