import os

os.system('docker build -t yingjunluo/devicehive-auth -f dockerfiles/devicehive-auth.Dockerfile .')
os.system('docker build -t yingjunluo/devicehive-plugin -f dockerfiles/devicehive-plugin.Dockerfile .')
os.system('docker build -t yingjunluo/devicehive-frontend -f dockerfiles/devicehive-frontend.Dockerfile .')
os.system('docker build -t yingjunluo/devicehive-backend -f dockerfiles/devicehive-backend.Dockerfile .')
os.system('docker build -t yingjunluo/devicehive-hazelcast -f dockerfiles/devicehive-hazelcast.Dockerfile .')