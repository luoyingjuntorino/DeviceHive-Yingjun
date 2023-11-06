FROM python:3.9-slim

WORKDIR "/code"

COPY . .

RUN pip3 install requests 

CMD ["python3", "hello.py"]