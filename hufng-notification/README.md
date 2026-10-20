# hufng-notification — WebSocket cho client (tham khảo, chưa implement)

Tài liệu tham khảo trước khi thêm kênh WebSocket cho `hufng-notification` (dashboard
xem live alert, bên cạnh Telegram/Discord đã có). Trạng thái: **chưa code**,
ghi lại kiến thức + quyết định thiết kế để dùng khi bắt tay implement.

## 1. Raw WebSocket vs STOMP — chọn cái nào

| | Raw WebSocket | STOMP over WebSocket |
|---|---|---|
| API Spring | `WebSocketHandler`/`TextWebSocketHandler` + `WebSocketConfigurer` | `@EnableWebSocketMessageBroker` + `SimpMessagingTemplate` |
| Khái niệm "topic"/subscribe | Không có sẵn — tự quản lý danh sách session, tự viết logic "gửi cho ai" | Có sẵn — client `subscribe("/topic/...")`, broker tự route đúng người nghe |
| Dependency | `spring-boot-starter-websocket` (nhẹ) | `spring-boot-starter-websocket` + message broker (in-memory hoặc relay ra RabbitMQ/ActiveMQ) |
| Phù hợp khi | Broadcast đơn giản, mọi client nhận như nhau | Cần lọc theo nhiều tiêu chí (per-device, per-user), nhiều loại subscription |

**Quyết định cho `hufng-notification`**: dùng **raw WebSocket, broadcast toàn bộ**
— dashboard hiện tại chỉ cần xem live mọi alert, chưa cần lọc theo device/user.
Nâng cấp lên STOMP sau này (nếu cần lọc) không tốn nhiều công chuyển đổi vì
logic push message giữ nguyên, chỉ đổi tầng vận chuyển.

## 2. Kiến trúc dự kiến

```
RESOURCE_ALERT (Kafka)
      │
      ▼
ResourceAlertConsumer ── (đã có, xem consumer/ResourceAlertConsumer.java)
      │
      ├─▶ NotificationRouter.send(channel, content) ──▶ Telegram/Discord (đã có)
      │
      └─▶ WebSocketAlertService (MỚI, channel()=ALL)
                │
                ▼
          broadcast tới mọi WebSocketSession đang mở tại /ws/alerts
```

`WebSocketAlertService` implement `NotificationService` với `channel()` trả về
`ALL` — theo đúng pattern router hiện có (`NotificationRouter` lọc provider
theo `channel()`, không cần sửa gì ở `ResourceAlertConsumer`/`NotificationRouter`).
Nghĩa là: **mọi alert dù route Telegram hay Discord đều được đẩy thêm qua
WebSocket** — WS là kênh cộng thêm, không thay thế 2 kênh kia.

## 3. Những điều cần lưu ý khi làm WebSocket trong Spring

### 3a. `WebSocketSession.sendMessage()` không thread-safe

Nếu 2 thread cùng gọi `session.sendMessage(...)` trên **cùng 1 session** đồng
thời (ví dụ: 1 alert tới từ Kafka listener thread trong lúc heartbeat/ping
thread khác cũng đang gửi) → race condition, có thể corrupt frame hoặc
`IllegalStateException`. Phải tự đồng bộ hoá theo từng session — cách phổ biến
nhất là bọc session bằng `ConcurrentWebSocketSessionDecorator` (Spring có sẵn),
nó tự serialize các lần gửi + có buffer limit/timeout để không block vô hạn
nếu client chậm nhận.

### 3b. Nhiều instance = mỗi instance chỉ thấy session của chính nó (vấn đề quan trọng nhất với project này)

`hufng-notification` được thiết kế để scale nhiều instance cùng consumer group
Kafka (mỗi instance nhận message từ 1 tập partition riêng của `RESOURCE_ALERT`).
Nếu WebSocket session chỉ lưu trong memory của từng instance (`List<WebSocketSession>`
cục bộ) thì **client A đang kết nối tới instance #1 sẽ không bao giờ nhận được
alert mà Kafka route message tới instance #2** — vì instance #2 không biết
instance #1 đang có client nào đang mở, chỉ broadcast được cho session cục bộ
của chính nó.

Cách xử lý (chọn 1 khi thật sự cần multi-instance, chưa cần nếu demo 1 instance):
- **Redis Pub/Sub** — mỗi instance vừa là publisher vừa là subscriber trên 1
  channel Redis chung; instance nào nhận alert từ Kafka thì publish lên Redis,
  mọi instance (kể cả chính nó) nhận lại qua subscribe rồi mới broadcast cho
  session cục bộ. `hufng-common`'s `RedisService`/`BaseRedisConfig` đã có sẵn
  Redis connection, chỉ cần thêm pub/sub listener.
- **STOMP + external broker relay** (RabbitMQ có plugin STOMP) — broker ngoài
  tự lo việc route message tới đúng instance đang giữ session của client đó.
  Đây là lý do STOMP "phổ biến hơn trong production" — nó giải quyết sẵn đúng
  bài toán này, đổi lại thêm 1 hạ tầng phải vận hành.
- **Sticky session + không cần fan-out** — nếu chấp nhận mỗi client luôn dính
  1 instance cố định (load balancer sticky session) VÀ mỗi instance đọc đủ mọi
  partition (concurrency = số partition ngay trong 1 instance, không scale
  ngang) thì không cần fan-out gì cả — nhưng đánh đổi mất khả năng scale ngang
  thật của consumer.

### 3c. Handshake, CORS, origin

Trình duyệt chặn WebSocket handshake cross-origin y hệt CORS của HTTP thường —
`WebSocketConfigurer` cần khai `setAllowedOrigins(...)` tường minh (không có
mặc định permissive như REST `@CrossOrigin` dev-friendly). Quên khai → handshake
fail ngay ở bước upgrade HTTP→WS, lỗi khó đọc phía client (`WebSocket connection
... failed`), không có body response rõ ràng như lỗi HTTP thường.

### 3d. Auth lúc handshake — không gửi được custom header từ browser

Browser's `WebSocket` API **không cho set header tuỳ ý** (không giống
`fetch`/`XMLHttpRequest` gắn được `Authorization: Bearer ...`). Cách phổ biến:
truyền token qua **query param** lúc connect (`wss://.../ws/alerts?token=...`)
rồi verify trong `HandshakeInterceptor.beforeHandshake(...)` trước khi upgrade
connection — không dùng cookie nếu client là app di động/không cùng domain.

### 3e. Idle timeout, proxy/load balancer, và ping/pong keep-alive

Connection WebSocket giữ mở lâu (khác HTTP request/response ngắn) — nhiều
tầng hạ tầng trung gian (Nginx, ALB, Cloudflare) có timeout mặc định cho
connection "im lặng" quá lâu (không có traffic) và tự đóng nó, dù cả server
lẫn client vẫn tưởng đang kết nối. Cần:
- Server gửi **ping frame** định kỳ (Spring's `WebSocketHandlerDecorator` hoặc
  tự lên lịch `@Scheduled`) để giữ connection "có traffic" qua mắt proxy.
- Cấu hình timeout phía proxy đủ dài (`proxy_read_timeout` ở Nginx) nếu tự
  quản hạ tầng, hoặc theo giới hạn của managed load balancer (ALB mặc định
  idle timeout 60s, thường phải tăng lên cho WebSocket).

### 3f. Session "chết" không tự dọn

Client đóng tab/mất mạng đột ngột không phải lúc nào cũng trigger
`afterConnectionClosed(...)` ngay lập tức (TCP timeout có thể mất vài phút để
phát hiện). Danh sách session trong memory cần tự dọn định kỳ (kiểm tra
`session.isOpen()` trước khi gửi, bắt exception khi `sendMessage()` fail rồi
remove khỏi danh sách) — không dọn sẽ leak memory dần theo thời gian, đặc biệt
với traffic có nhiều client vào-ra liên tục.

### 3g. Test không cần client thật

- `./gradlew test`: `WebSocketHandler` là 1 class Java thuần, test được bằng cách
  gọi trực tiếp `handleTextMessage(session, message)` với `WebSocketSession`
  mock (Mockito) — không cần dựng connection thật, giống cách `RuleEngineServiceTest`
  đang test logic thuần bằng mock.
- Test thủ công/manual: `wscat -c ws://localhost:8082/notification/api/ws/alerts`
  (cần `npm install -g wscat`), hoặc tab Network của DevTools trình duyệt, hoặc
  Postman (hỗ trợ WebSocket từ bản v8+).

## 4. Việc cần làm khi implement (checklist, chưa làm)

1. Thêm dependency `spring-boot-starter-websocket` vào `build.gradle.kts`.
2. `WebSocketConfig implements WebSocketConfigurer` — đăng ký handler tại
   `/ws/alerts` (nhớ cộng `server.servlet.context-path` hiện có
   `/notification/api` → path đầy đủ `/notification/api/ws/alerts`),
   `setAllowedOrigins(...)` tường minh.
3. `AlertBroadcastHandler extends TextWebSocketHandler` — giữ danh sách session
   bằng `CopyOnWriteArraySet<WebSocketSession>` (thread-safe cho add/remove khi
   nhiều connection vào-ra đồng thời), bọc mỗi session bằng
   `ConcurrentWebSocketSessionDecorator` trước khi lưu (mục 3a).
4. `WebSocketAlertService implements NotificationService` (`channel() = ALL`) —
   nhận `content`, serialize JSON, gọi `AlertBroadcastHandler.broadcast(...)`.
   Đăng ký như 1 bean bình thường, `NotificationRouter` tự inject vào danh sách
   provider hiện có, không cần sửa `NotificationRouter`/`ResourceAlertConsumer`.
5. (Khi thật sự cần multi-instance) thêm Redis Pub/Sub fan-out — mục 3b.
6. Test: unit test `AlertBroadcastHandler`/`WebSocketAlertService` bằng mock
   session (mục 3g); test thủ công bằng `wscat` sau khi chạy `docker compose up`
   + publish thử 1 message vào `RESOURCE_ALERT`.

## 5. Ghi chú thêm (Q&A khi tìm hiểu)

### 5a. STOMP quản lý session/subscription thế nào, và scale nhiều instance ra sao

STOMP quản lý theo 2 tầng:

- **Tầng 1 — WebSocket session**: giống hệt raw WebSocket, mỗi client vẫn là
  1 `WebSocketSession` vật lý gắn với đúng 1 instance đang giữ connection đó.
- **Tầng 2 — subscription registry**: khi client gửi frame
  `SUBSCRIBE /topic/alerts/device-123`, Spring lưu lại map "destination →
  danh sách session đang subscribe" để biết gửi cho ai. Map này lưu ở đâu phụ
  thuộc broker được cấu hình:

**Mặc định — Simple Broker (in-memory), KHÔNG giải quyết được multi-instance:**

```java
config.enableSimpleBroker("/topic"); // hay dùng trong tutorial
```

`SimpleBrokerMessageHandler` giữ subscription map **trong memory của chính
JVM đó**. Client subscribe qua instance #1 → map chỉ tồn tại trong RAM của
instance #1. Instance #2 nhận alert từ Kafka rồi gọi
`simpMessagingTemplate.convertAndSend(...)` → chỉ gửi được cho session mà
**instance #2 tự biết** — tức 0 session, vì client đó subscribe qua instance
#1. Đây là hiểu lầm phổ biến: "STOMP có sẵn subscribe/topic nên chắc tự scale
được" — **sai**, Simple Broker dính đúng vấn đề mục 3b, chỉ là bị che giấu kỹ
hơn vì trông "chuyên nghiệp" hơn raw WebSocket.

**Muốn scale thật — STOMP Broker Relay (đổi sang broker ngoài):**

```java
config.enableStompBrokerRelay("/topic", "/queue")
      .setRelayHost("rabbitmq-host")
      .setRelayPort(61613); // STOMP plugin port của RabbitMQ
```

Mỗi instance Spring không tự giữ subscription registry nữa — chỉ đóng vai
**relay mỏng**: mọi SUBSCRIBE/SEND của client được forward qua 1 connection
TCP bền tới RabbitMQ (bật STOMP plugin) hoặc ActiveMQ. Broker ngoài mới là
nơi giữ "ai subscribe gì" thật, tự fan-out message tới đúng relay/instance
đang giữ session của từng subscriber, bất kể instance nào publish. Đánh đổi:
thêm 1 hạ tầng phải tự vận hành (RabbitMQ/ActiveMQ), thêm 1 hop latency qua
network, broker trở thành single point of failure nếu không cluster.

**Gotcha thêm**: `SimpUserRegistry` (dùng khi gửi theo user qua
`convertAndSendToUser(...)`) cũng mặc định per-instance như Simple Broker —
Spring không có bản distributed sẵn, muốn "gửi cho user X dù đang connect ở
instance nào" phải tự giải quyết tương tự (Redis, hoặc dựa vào chính broker
relay ở trên).

Tóm gọn: **STOMP không tự động giải quyết bài toán multi-instance** — nó chỉ
giải quyết đẹp hơn KHI trả thêm chi phí dùng broker ngoài; dùng Simple Broker
mặc định thì dính y hệt vấn đề raw WebSocket đã nêu ở mục 3b.

### 5b. `ConcurrentHashMap` có thay được Redis Pub/Sub/STOMP relay ở mục 3b không?

Không — 2 khái niệm khác trục nhau, hay bị nhầm làm 1:

- **Thread-safe** (`ConcurrentHashMap` giải quyết) = an toàn khi nhiều
  **thread trong cùng 1 process/JVM** cùng đọc/ghi. Trong `hufng-notification`
  sẽ có nhiều thread cùng đụng danh sách session: Tomcat's I/O thread gọi
  `afterConnectionEstablished`/`afterConnectionClosed` khi client connect/
  disconnect (xem mục 5c), và thread của `@KafkaListener`
  (`ResourceAlertConsumer`) gọi broadcast khi có alert mới. `HashMap` thường ở
  đây sẽ vỡ thật (structural corruption, `ConcurrentModificationException`
  khi đang iterate để broadcast mà thread khác vừa remove).
- **Distributed** (`ConcurrentHashMap` KHÔNG giải quyết) = nhiều
  **process/JVM khác nhau** (nhiều instance) cùng nhìn thấy 1 trạng thái
  chung. Dù `ConcurrentHashMap` an toàn tuyệt đối, nó vẫn chỉ sống trong heap
  của đúng 1 JVM — instance #2 không hề biết instance #1 có map đó, đừng nói
  tới đọc/ghi chung.

Nên `ConcurrentHashMap<String sessionId, WebSocketSession>` là lựa chọn đúng
để lưu session **trong 1 instance** (thay cho `CopyOnWriteArraySet` ở
checklist mục 4.3 — add/remove/lookup trung bình O(1) so với
`CopyOnWriteArraySet` phải copy lại toàn bộ mảng mỗi lần add/remove, tốn kém
nếu client connect/disconnect liên tục; `.values()` để iterate lúc broadcast
vẫn an toàn, weakly consistent), nhưng **không thay thế được** Redis Pub/Sub
hay STOMP Broker Relay ở mục 3b — đó là 2 tầng vấn đề độc lập, giải quyết
xong tầng này không có nghĩa tầng kia tự hết.

### 5c. Tomcat trong Spring là gì

Tomcat là 1 **web server / servlet container** (dự án Apache) — thứ thực sự
lắng nghe port TCP, nhận HTTP request, quản lý thread pool xử lý request, rồi
mới chuyển vào cho code Spring xử lý. Spring không tự viết web server, nó
dùng Tomcat (hoặc Jetty/Undertow) làm tầng vận chuyển bên dưới.

`spring-boot-starter-web` (có trong `hufng-ingest`/`hufng-report`/
`hufng-notification`) kéo theo `spring-boot-starter-tomcat`, và Spring Boot
**nhúng thẳng Tomcat vào trong JVM process của app** ("embedded Tomcat") —
khác Java EE truyền thống (cài Tomcat riêng, build `.war`, deploy vào đó).
`./gradlew :hufng-notification:bootRun` chạy ra đúng 1 process; bên
trong process đó Tomcat tự khởi động, lắng nghe port khai ở `server.port`.

```
Client (browser/curl)
      │  HTTP request tới :8082
      ▼
Tomcat (embedded, chạy trong process hufng-notification)
      │  nhận TCP connection, parse HTTP, lấy 1 thread từ thread pool để xử lý
      ▼
DispatcherServlet (Spring MVC's front controller)
      │  route theo @RequestMapping
      ▼
@RestController method (vd NotificationController)
```

Tomcat còn implement sẵn Jakarta WebSocket API — khi client gửi request có
header `Upgrade: websocket`, Tomcat tự xử lý bước "nâng cấp" từ HTTP thành
WebSocket connection, giữ nguyên TCP connection sống lâu dài (khác HTTP
request/response ngắn), và tự cấp **1 thread I/O riêng** để lắng nghe
connect/disconnect/nhận message trên connection đó — đây chính là "Tomcat's
I/O thread" nhắc tới ở mục 5b.

Có thể swap Tomcat sang Jetty/Undertow (loại trừ `spring-boot-starter-tomcat`,
thêm `spring-boot-starter-jetty`/`-undertow`) — cả 3 implement cùng chuẩn
Servlet API + WebSocket API nên thường chỉ cần đổi dependency, không cần sửa
code Spring MVC/WebSocket. Hiện `hufng-iot` chưa có lý do gì để đổi khỏi
Tomcat mặc định.
