"""
青禾商城 · 压测数据恢复
======================
写链路压测后清理 dev 库脏数据，使 qinghedb 接近压测前状态：
  - 恢复目标 SKU 初始库存：pd004=366（吞吐目标）、pd045=150（边界目标）
  - 删除压测账号 perftest_main 产生的订单及其库存流水
运行：python perf/restore_data.py
"""
import os
import pymysql

DB_CFG = dict(
    host=os.getenv("DB_HOST", "127.0.0.1"),
    port=3306,
    user="root",
    passwd=os.getenv("DB_PWD", "1234"),
    db=os.getenv("DB_NAME", "qinghedb"),
    charset="utf8mb4",
)
TEST_USER = os.getenv("TEST_USER", "perftest_main")
PID_TP = os.getenv("PID_TP", "pd004")
PID_TP_INIT = int(os.getenv("PID_TP_INIT", "366"))
PID_EDGE = os.getenv("PID_EDGE", "pd045")
PID_EDGE_INIT = int(os.getenv("PID_EDGE_INIT", "150"))


def main():
    conn = pymysql.connect(**DB_CFG)
    cur = conn.cursor()
    # 取压测账号 id
    cur.execute("SELECT id FROM user WHERE user_name=%s", (TEST_USER,))
    row = cur.fetchone()
    if row:
        uid = row[0]
        # 先删该用户订单关联的库存流水（stock_log 无 FK，安全删）
        cur.execute(
            "DELETE FROM stock_log WHERE order_number IN (SELECT order_number FROM `order` WHERE user_id=%s)",
            (uid,),
        )
        n_log = cur.rowcount
        # 删该用户订单
        cur.execute("DELETE FROM `order` WHERE user_id=%s", (uid,))
        n_ord = cur.rowcount
        print("已删除压测账号订单=%d 条，关联库存流水=%d 条" % (n_ord, n_log))
    else:
        print("未找到压测账号 %s，跳过订单清理" % TEST_USER)
    # 恢复库存初始值
    cur.execute("UPDATE product_detail SET stock=%s WHERE id=%s", (PID_TP_INIT, PID_TP))
    cur.execute("UPDATE product_detail SET stock=%s WHERE id=%s", (PID_EDGE_INIT, PID_EDGE))
    conn.commit()
    cur.execute("SELECT id, stock FROM product_detail WHERE id IN (%s,%s)", (PID_TP, PID_EDGE))
    for r in cur.fetchall():
        print("恢复库存: %s -> %s" % r)
    cur.close()
    conn.close()
    print("✅ 数据恢复完成")


if __name__ == "__main__":
    main()
