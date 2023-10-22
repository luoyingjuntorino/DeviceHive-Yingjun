import os

os.system('docker login')

os.system('docker push yingjunluo/devicehive-auth')
os.system('docker push yingjunluo/devicehive-plugin')
os.system('docker push yingjunluo/devicehive-frontend')
os.system('docker push yingjunluo/devicehive-backend')
os.system('docker push yingjunluo/devicehive-hazelcast')