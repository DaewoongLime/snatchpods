# server.py
from flask import Flask
import subprocess

app = Flask(__name__)

# 아까 복사한 에어팟 MAC 주소를 여기에 넣으세요
AIRPODS_MAC_ADDRESS = "04-99-b9-43-98-53"

def run_command(command):
    try:
        subprocess.run(command, shell=True, check=True)
        return True
    except subprocess.CalledProcessError:
        return False

@app.route('/connect')
def connect_airpods():
    # 1초 정도 딜레이가 있을 수 있습니다.
    print(f"Connecting to {AIRPODS_MAC_ADDRESS}...")
    success = run_command(f"blueutil --connect {AIRPODS_MAC_ADDRESS}")
    return "Connected!" if success else "Failed to connect"

@app.route('/disconnect')
def disconnect_airpods():
    print(f"Disconnecting {AIRPODS_MAC_ADDRESS}...")
    success = run_command(f"blueutil --disconnect {AIRPODS_MAC_ADDRESS}")
    return "Disconnected!" if success else "Failed to disconnect"

if __name__ == '__main__':
    # 0.0.0.0은 외부(내 폰)에서 접속을 허용한다는 뜻입니다.
    app.run(host='0.0.0.0', port=5001)