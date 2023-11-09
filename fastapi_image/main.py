from fastapi import FastAPI, File, Query,Depends, UploadFile, APIRouter, Form, HTTPException, status
from fastapi.responses import JSONResponse, StreamingResponse
from fastapi.security import OAuth2PasswordBearer
from datetime import datetime
from zipfly import ZipFly
import requests
import pymongo
import os


oauth2_scheme = OAuth2PasswordBearer(tokenUrl="token")
app = FastAPI()

UPLOAD_FOLDER = "images"  # folder to store
if not os.path.exists(UPLOAD_FOLDER):
    os.makedirs(UPLOAD_FOLDER) 

async def authenticate_user(token: str = Depends(oauth2_scheme)):
    token_bearer = f'Bearer {token}'
    response = requests.put("https://playground.devicehive.com:443/api/rest/user/current",
                            headers={"Authorization": token_bearer}, json={})
    if response.ok:
        return {"token": token_bearer}
    else:
        raise HTTPException(status_code=response.status_code)

@app.get("/welcome", tags=["default"])
def root():
    return {"message":"Hello! this is from welcome."}

@app.post("/authenticate", tags=["authenticate"])
async def auth(token: str):
    token_bearer = f'Bearer {token}'
    response = requests.get("http://localhost:80/api/rest/user/current",
                            headers={"Authorization": token_bearer}, json={})
    if response.status_code == 200:
        return {"msg": "authorized", "token":token_bearer}
    else:
        raise HTTPException(status_code=response.status_code)

@app.post("/upload/image/", tags=["image"])
async def upload_image(image: UploadFile = File(...),
                       description: str = Form(...),):
    current_time = datetime.now()
    formatted_time = current_time.strftime("%Y-%m-%d-%H-%M")
    file_name = f"{formatted_time}.hdr"
    # file_name = f"{formatted_time}.jpeg"
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

@app.get("/download_zip1", tags=["image"], description="Download ZIP with all image files")
async def download_images():
    path_images = os.path.join(os.getcwd(), "images")
    if os.path.exists(path_images):
        print(path_images)
        paths = []
        for path, subdirs, files in os.walk(path_images):
            for name in files:
                file_location = os.path.join(path, name)
                paths.append({'fs': file_location, 'n': name})
        zfly = ZipFly(paths=paths)
        generator = zfly.generator()
        return StreamingResponse(iter(generator), media_type="application/x-zip-compressed", headers={"Content-Disposition": "attachment; filename=images.zip"})
    else:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND)

@app.get("/download_zip2", tags=["image"], description="Download ZIP with selected image files")
async def download_images(image_names: list = Query(..., description="List of image file names to download")):
    path_images = os.path.join(os.getcwd(), "images")
    if os.path.exists(path_images):
        paths = []

        for name in image_names:
            file_location = os.path.join(path_images, name)
            if os.path.exists(file_location):
                paths.append({'fs': file_location, 'n': name})
            else:
                return HTTPException(status_code=404, detail=f"Image file '{name}' not found")

        zfly = ZipFly(paths=paths)
        generator = zfly.generator()
        return StreamingResponse(iter(generator), media_type="application/x-zip-compressed", headers={"Content-Disposition": "attachment; filename=images.zip"})
    else:
        return HTTPException(status_code=404, detail="Images folder not found")


