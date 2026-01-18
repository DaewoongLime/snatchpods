const bleno = require('@abandonware/bleno');
const { execSync } = require('child_process');

// 1. 우리만의 고유 채널 ID (UUID)
// 랜덤 생성된 것이니 그대로 쓰셔도 됩니다.
const SERVICE_UUID = '12345678-1234-1234-1234-1234567890ab';
const STATUS_CHAR_UUID = '0000cccc-0000-1000-8000-00805f9b34fb'; // 상태 알림용
const COMMAND_CHAR_UUID = '0000bbbb-0000-1000-8000-00805f9b34fb'; // 명령 수신용

// 내 에어팟 주소 (아까 구해둔 주소로 꼭 바꾸세요!)
const AIRPODS_MAC_ADDRESS = "04-99-b9-43-98-53"; 

let updateValueCallback = null; // 안드로이드에게 데이터 쏠 때 쓰는 함수

// --- [로직 1] 상태 확인용 Characteristic (Notify/Read) ---
class StatusCharacteristic extends bleno.Characteristic {
    constructor() {
        super({
            uuid: STATUS_CHAR_UUID,
            properties: ['read', 'notify'], // 읽기 및 알림 가능
            value: null
        });
    }

    // 안드로이드가 구독(Subscribe)을 시작하면 호출됨
    onSubscribe(maxValueSize, callback) {
        console.log('📱 안드로이드가 연결되었습니다! (Subscribed)');
        updateValueCallback = callback;
    }

    // 안드로이드가 구독을 끊으면 호출됨
    onUnsubscribe() {
        console.log('📱 안드로이드 연결 해제 (Unsubscribed)');
        updateValueCallback = null;
    }
}

// --- [로직 2] 명령 수신용 Characteristic (Write) ---
class CommandCharacteristic extends bleno.Characteristic {
    constructor() {
        super({
            uuid: COMMAND_CHAR_UUID,
            properties: ['write'], // 쓰기 전용
        });
    }

    // 안드로이드가 명령을 보낼 때 호출됨
    onWriteRequest(data, offset, withoutResponse, callback) {
        const command = data[0]; // 첫 번째 바이트 확인 (예: 0x01)
        console.log(`📩 명령 수신: ${command}`);

        if (command === 0x01) { // 0x01: 연결해라!
            console.log("🎧 에어팟 연결 시도 중...");
            try {
                // blueutil로 연결 실행
                execSync(`blueutil --connect ${AIRPODS_MAC_ADDRESS}`);
                console.log("✅ 에어팟 연결 완료");
            } catch (e) {
                console.error("❌ 연결 실패:", e.message);
            }
        }
        
        callback(this.RESULT_SUCCESS); // 성공 응답
    }
}

// --- [메인 로직] 서비스 실행 및 상태 모니터링 ---
const statusChar = new StatusCharacteristic();
const commandChar = new CommandCharacteristic();

bleno.on('stateChange', (state) => {
    console.log(`블루투스 상태: ${state}`);
    if (state === 'poweredOn') {
        // 이름과 UUID를 주변에 뿌림 (Advertising)
        bleno.startAdvertising('AirPods-Manager', [SERVICE_UUID]);
    } else {
        bleno.stopAdvertising();
    }
});

bleno.on('advertisingStart', (error) => {
    if (!error) {
        console.log('📡 Advertising 시작... 안드로이드에서 검색 가능!');
        
        // 서비스를 등록
        bleno.setServices([
            new bleno.PrimaryService({
                uuid: SERVICE_UUID,
                characteristics: [statusChar, commandChar]
            })
        ]);

        // 1초마다 오디오 상태 체크 루프 시작
        startStatusLoop();
    }
});

function startStatusLoop() {
    let lastState = -1; // 이전 상태 저장 (중복 전송 방지)

    setInterval(() => {
        if (!updateValueCallback) return; // 구독자가 없으면 굳이 체크 안 함

        try {
            // 현재 오디오 장치 이름 가져오기
            const output = execSync('SwitchAudioSource -c').toString().trim();
            // 스피커가 포함되어 있으면 BUSY(1), 아니면 FREE(0)
            const currentState = output.includes("Speakers") ? 1 : 0;

            // 상태가 변했을 때만 안드로이드로 전송
            if (currentState !== lastState) {
                console.log(`상태 변경 감지: ${currentState === 1 ? "⛔ BUSY (스피커)" : "✅ FREE (대기중)"}`);
                
                const data = Buffer.alloc(1);
                data.writeUInt8(currentState, 0);
                updateValueCallback(data); // Push!
                
                lastState = currentState;
            }
        } catch (e) {
            console.error("오디오 상태 확인 에러:", e.message);
        }
    }, 1000); // 1초 간격
}