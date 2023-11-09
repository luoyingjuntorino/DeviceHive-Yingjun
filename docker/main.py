import psycopg2

def connec():
    # 定义数据库连接参数
    db_params = {
        'user': 'postgres',           # 数据库用户名
        'password': 'mysecretpassword',  # 数据库密码
        'host': '127.0.0.1',          # 数据库主机（Docker容器的主机名或IP地址）
        'port': '5432',               # 数据库端口
        'database': 'postgres'        # 数据库名称
    }


    # 建立数据库连接
    conn = psycopg2.connect(**db_params)

    # 创建一个数据库游标
    cur = conn.cursor()

    # 打印连接成功消息
    print("已成功连接")

    # 获取数据库中所有表的名称
    cur.execute("SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'")
    tables = cur.fetchall()

    # 打印数据库中所有表的名称
    for table in tables:
        print("表名:", table[0])

    # 关闭游标和数据库连接
    cur.close()
    conn.close()

if __name__ == '__main__':
    connec()

