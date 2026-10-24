# -*- coding: utf-8 -*-
r"""
将本地 MinIO 注册为 Windows 服务（开机自启，无需登录）。

安装（管理员 PowerShell）：
    python deploy/minio_service.py --startup auto install
    Start-Service ecom-minio
卸载：
    Stop-Service ecom-minio
    python deploy/minio_service.py remove

服务只做一件事：拉起 minio.exe 并守护它（子进程退出就重启），
收到停止指令时先 terminate 子进程再退出。日志：D:\ecom\minio\logs\minio.log
"""
import os
import subprocess
import sys

import win32event
import win32service
import win32serviceutil
import servicemanager

MINIO_EXE = r"D:\ecom\minio\minio.exe"
DATA_DIR = r"D:\ecom\minio-data"
LOG_DIR = r"D:\ecom\minio\logs"
LOG_FILE = os.path.join(LOG_DIR, "minio.log")
MAX_LOG_BYTES = 10 * 1024 * 1024


def log(msg):
    try:
        os.makedirs(LOG_DIR, exist_ok=True)
        if os.path.exists(LOG_FILE) and os.path.getsize(LOG_FILE) > MAX_LOG_BYTES:
            os.replace(LOG_FILE, LOG_FILE + ".1")
        with open(LOG_FILE, "a", encoding="utf-8") as f:
            f.write(msg.rstrip() + "\n")
    except Exception:
        pass


class MinioService(win32serviceutil.ServiceFramework):
    _svc_name_ = "ecom-minio"
    _svc_display_name_ = "Ecom MinIO"
    _svc_description_ = "MinIO 对象存储（电商系统商品图片），监听 9000 / 控制台 9001"

    def __init__(self, args):
        super().__init__(args)
        self.stop_event = win32event.CreateEvent(None, 0, 0, None)
        self.proc = None

    # ---------- 服务控制 ----------
    def SvcStop(self):
        self.ReportServiceStatus(win32service.SERVICE_STOP_PENDING)
        win32event.SetEvent(self.stop_event)
        if self.proc and self.proc.poll() is None:
            self.proc.terminate()
            try:
                self.proc.wait(timeout=10)
            except subprocess.TimeoutExpired:
                self.proc.kill()
        log("service stopped")

    def SvcDoRun(self):
        servicemanager.LogInfoMsg("ecom-minio: starting")
        log("service started (pid %d)" % os.getpid())
        while win32event.WaitForSingleObject(self.stop_event, 1000) != win32event.WAIT_OBJECT_0:
            if self.proc is None or self.proc.poll() is not None:
                if self.proc is not None:
                    log("minio exited with code %s, restarting in 3s" % self.proc.returncode)
                    if win32event.WaitForSingleObject(self.stop_event, 3000) == win32event.WAIT_OBJECT_0:
                        break
                self.start_child()

    # ---------- 子进程 ----------
    def start_child(self):
        if not os.path.exists(MINIO_EXE):
            log("minio.exe not found: %s" % MINIO_EXE)
            return
        os.makedirs(DATA_DIR, exist_ok=True)
        log_file = open(LOG_FILE, "a", encoding="utf-8", errors="replace")
        self.proc = subprocess.Popen(
            [MINIO_EXE, "server", DATA_DIR, "--address", ":9000", "--console-address", ":9001"],
            stdout=log_file, stderr=subprocess.STDOUT,
            cwd=os.path.dirname(MINIO_EXE),
            creationflags=subprocess.CREATE_NO_WINDOW,
        )
        log("minio started (pid %d)" % self.proc.pid)


if __name__ == "__main__":
    if len(sys.argv) == 1:
        servicemanager.Initialize()
        servicemanager.PrepareToHostSingle(MinioService)
        servicemanager.StartServiceCtrlDispatcher()
    else:
        win32serviceutil.HandleCommandLine(MinioService)
