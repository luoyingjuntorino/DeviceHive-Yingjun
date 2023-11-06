FROM python:3.9-slim

WORKDIR "/plugin_initial"

COPY . .

RUN pip install -r requirements.txt

CMD ["python3", "main.py"]