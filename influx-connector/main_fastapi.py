import os
import json
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from query_influx import Query_influx
from fastapi.responses import StreamingResponse
from fastapi.responses import JSONResponse

app = FastAPI()

class QueryParams(BaseModel):
    query_type: str
    sensor_ids: list
    start_time: str
    stop_time: str

@app.get("/")
def welcome():
    return "welcome"

@app.get("/download_csv")
async def download_csv():
    csv_file_path = "useCase1_2023-11-07_2023-11-07.csv"  # 替换为实际的 CSV 文件路径
    if not os.path.exists(csv_file_path):
        raise HTTPException(status_code=404, detail="CSV 文件不存在")

    def generate():
        with open(csv_file_path, "rb") as file:
            yield from file

    response = StreamingResponse(generate(), media_type="text/csv")
    response.headers["Content-Disposition"] = 'attachment; filename="exported_data.csv"'
    return response

@app.post("/csv/")
async def export_csv(query_params: QueryParams):
    influx_config = "influxdb_config.json"
    try:
        config = json.load(open(influx_config))
    except FileNotFoundError:
        raise HTTPException(status_code=500, detail=f"Error: The file '{influx_config}.json' does not exist.")

    Manager = Query_influx(config, query_params.query_type, query_params.sensor_ids, query_params.start_time, query_params.stop_time)
    Manager.csv_exporter()
    return {"message": "CSV file generated successfully"}

@app.post("/dataframe/")
async def get_dataframe(query_params: QueryParams):
    influx_config = "influxdb_config.json"
    try:
        config = json.load(open(influx_config))
    except FileNotFoundError:
        raise HTTPException(status_code=500, detail=f"Error: The file '{influx_config}.json' does not exist.")

    Manager = Query_influx(config, query_params.query_type, query_params.sensor_ids, query_params.start_time, query_params.stop_time)
    df = Manager.query_pro()
    return JSONResponse(content=df.to_json(orient='records'))


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="localhost", port=8000)