# Start local MinIO for product images (called by the logon startup shortcut).
# Ports: 9000 API / 9001 console. Data dir: D:\ecom\minio-data
# Note: keep this file ASCII-only -- Windows PowerShell 5.1 reads scripts as ANSI,
# and non-ASCII comments can break parsing without a BOM.
$exe = "D:\ecom\minio\minio.exe"
$dataDir = "D:\ecom\minio-data"

# Skip if something is already listening on 9000 (no duplicate process on second run).
try {
    $client = New-Object System.Net.Sockets.TcpClient
    $client.Connect("127.0.0.1", 9000)
    $client.Close()
    exit 0
} catch { }

Start-Process -FilePath $exe -ArgumentList @(
    "server", $dataDir,
    "--address", ":9000",
    "--console-address", ":9001"
) -WindowStyle Hidden
