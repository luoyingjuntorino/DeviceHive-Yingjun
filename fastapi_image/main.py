from fastapi import FastAPI, File, UploadFile, APIRouter, Form, HTTPException, status
from fastapi.responses import JSONResponse, FileResponse, StreamingResponse
from datetime import datetime
import pymongo
import zipfly
import os

app = FastAPI()

UPLOAD_FOLDER = "images"  # folder to store
if not os.path.exists(UPLOAD_FOLDER):
    os.makedirs(UPLOAD_FOLDER) 

@app.get("/welcome", tags=["default"])
def root():
    return {"message":"Hello! this is from welcome."}

@app.post("/upload/image/", tags=["image"])
async def upload_image(image: UploadFile = File(...),
                       description: str = Form(...),):
    current_time = datetime.now()
    formatted_time = current_time.strftime("%Y-%m-%d-%H-%M")
    # file_name = f"{formatted_time}.hdr"
    file_name = f"{formatted_time}.jpeg"
    file_path = os.path.join(UPLOAD_FOLDER, file_name)
    # save uploaded image locally(#TODO save it to Docker volumn and image's path save in MongoDB)
    with open(file_path, "wb") as f:
        f.write(image.file.read())
    # save image path to MongoDB
    client = pymongo.MongoClient("localhost", 27017) # connect to Mongo
    db = client["my_database"]  # database name
    images_path_collection = db["images_path"]
    image_doc = {
        "file_name": file_name,
        "path": file_path,
        "description": description
    }
    images_path_collection.insert_one(image_doc)
    #TODO can you update me some image's metadata? single datapoint 
    # can we put image's metadata in the description?
    
    return JSONResponse(content={
        "message": "Image uploaded and saved as {}".format(file_name),
        "description": description,  # metadata of image
        "file_path": file_path

    })

@app.get("/download_images", tags=["image"], description="maybe we have to specify time range, how to do with iamge's meta data?")
async def download_images():
        zip_file_path = "images.zip"  # 请替换为实际的ZIP文件路径
        if not os.path.exists(zip_file_path):
            return "ZIP file not found."
        return FileResponse(zip_file_path, media_type="application/zip", headers={"Content-Disposition": "attachment; filename=images.zip"})

# @app.get("/downloadStreaming_images", tags=["image"], description="maybe we have to specify a time range, how to do with image's metadata?")
# async def download_images():
#     zip_file_path = "images.zip"  # 替换为实际的 ZIP 文件路径
#     if not os.path.exists(zip_file_path):
#         raise HTTPException(status_code=status.HTTP_404_NOT_FOUND)

#     paths ={'fs': zip_file_path}
            
#     zfly = zipfly.ZipFly(paths=paths)
#     generator = zfly.generator()
#     return StreamingResponse(
#         iter(generator),
#         media_type="application/x-zip-compressed",
#         headers={"Content-Disposition": "attachment; filename=images.zip"}
#     )
    # else:
    #     raise HTTPException(status_code=status.HTTP_404_NOT_FOUND)
    # return StreamingResponse(
    #     content=generate(),
    #     media_type="application/zip",
    #     headers={"Content-Disposition": "attachment; filename=images.zip"}
    # )