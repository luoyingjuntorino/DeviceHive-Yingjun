# 使用基础镜像（例如Python官方镜像）
FROM python:3.9-slim

# 设置工作目录
WORKDIR /app

# 将本地代码复制到容器中
COPY . /app

# 安装应用程序依赖
RUN pip install -r requirements.txt

# 启动应用程序
CMD ["python", "main.py"]
