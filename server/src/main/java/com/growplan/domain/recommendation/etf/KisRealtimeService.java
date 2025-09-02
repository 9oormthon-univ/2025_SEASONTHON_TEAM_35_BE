package com.growplan.domain.recommendation.etf;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import okio.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class KisRealtimeService {

    private final KisApprovalClient approvalClient;
    private final KisEndpoints kisEndpoints;
    private final ETFQuoteStore quoteStore;
    private final String env, custtype, appKey, secretkey;
    private final List<String> domestic;
    private final Map<String, String> overseas;
    private final ObjectMapper om = new ObjectMapper();

    // 로거 필드 추가
    private static final Logger log = LoggerFactory.getLogger(KisRealtimeService.class);

    public KisRealtimeService(KisApprovalClient approvalClient, ETFQuoteStore etfQuoteStore,
                              KisProperties props, KisEndpoints kisEndpoints) {
        this.approvalClient = approvalClient;
        this.quoteStore = etfQuoteStore;
        this.kisEndpoints = kisEndpoints;
        this.env = props.getEnv();
        this.custtype = props.getCusttype();
        this.appKey = props.getAppkey();
        this.secretkey = props.getSecretkey();
        this.domestic = props.getDomestic();
        this.overseas = props.getOverseas();
    }

//    @Bean
//    ApplicationRunner run() {
//        return args -> {
//            String approvalKey = approvalClient.issueApprovalKey(appKey, secretkey).block();
//            System.out.println("approval_key = " + approvalKey);
//
//            String wsUrl = "prod".equalsIgnoreCase(env)
//                    ? "ws://ops.koreainvestment.com:21000/tryitout"
//                    : "ws://ops.koreainvestment.com:31000/tryitout";
//
//            OkHttpClient client = new OkHttpClient.Builder()
//                    .pingInterval(25, TimeUnit.SECONDS)
//                    .build();
//
//            Request req = new Request.Builder().url(wsUrl).build();
//
//            WebSocketListener listener = new WebSocketListener() {
//                @Override public void onOpen(WebSocket ws, Response resp) {
//                    System.out.println("[WS OPEN] " + resp);
//                    for (String code : domestic) {
//                        ws.send(buildSubscribeJson(approvalKey, "H0STCNT0", code));
//                        sleep(150);
//                    }
//                    for (Map.Entry<String, String> e : overseas.entrySet()) {
//                        ws.send(buildSubscribeJson(approvalKey, "HDFSCNT0", e.getValue()));
//                        sleep(150);
//                    }
//                }
//                @Override public void onMessage(WebSocket ws, String text) {
//                    if (text.startsWith("{")) {
//                        System.out.println("[ACK] " + text);
//                    } else {
//                        String[] parts = text.split("\\|", 4);
//                        if (parts.length >= 4) {
//                            String trId = parts[1];
//                            String payload = parts[3];
//                            String[] fields = payload.split("\\^");
//                            String key = fields.length > 0 ? fields[0] : "?";
//                            String last = fields.length > 2 ? fields[2] : "?";
//                            String change = fields.length > 4 ? fields[4] : "?";
//                            String rate = fields.length > 5 ? fields[5] : "?";
//                            System.out.printf("[TICK] %s %s last=%s chg=%s rate=%s raw=%s%n",
//                                    trId, key, last, change, rate, payload);
//                        } else {
//                            System.out.println("[MSG] " + text);
//                        }
//                    }
//                }
//                @Override public void onMessage(WebSocket ws, ByteString bytes) {
//                    System.out.println("[BIN] " + bytes.hex());
//                }
//                @Override public void onClosing(WebSocket ws, int code, String reason) {
//                    System.out.println("[CLOSING] " + code + " " + reason);
//                    ws.close(1000, null);
//                }
//                @Override public void onFailure(WebSocket ws, Throwable t, Response r) {
//                    System.out.println("[FAIL] " + t.getMessage());
//                }
//            };
//
//            client.newWebSocket(req, listener);
//            Thread.currentThread().join();
//        };
//    }

    @Bean
    ApplicationRunner run() {
        return args -> {
            String approvalKey = approvalClient.issueApprovalKey(appKey, secretkey).block();
//            String wsUrl = "prod".equalsIgnoreCase(env)
//                    ? "ws://ops.koreainvestment.com:21000/tryitout"
//                    : "ws://ops.koreainvestment.com:31000/tryitout";
            String wsUrl = kisEndpoints.wsUrl();

            OkHttpClient client = new OkHttpClient.Builder()
                    .pingInterval(25, TimeUnit.SECONDS)
                    .build();

            Request req = new Request.Builder().url(wsUrl).build();

            client.newWebSocket(req, new WebSocketListener() {
                @Override public void onOpen(WebSocket ws, Response resp) {

                    log.info("[WebSocket Opened] Response: {}", resp); // 연결 성공 로그

//                    domestic.forEach(code -> {
//                        ws.send(buildSubscribeJson(approvalKey, "H0STCNT0", code));
//                        sleep(120);
//                    });
//                    overseas.forEach((alias, trKey) -> {
//                        ws.send(buildSubscribeJson(approvalKey, "HDFSCNT0", trKey));
//                        sleep(120);
//                    });

                    domestic.forEach(code -> {
                        String json = buildSubscribeJson(approvalKey, "H0STCNT0", code);
                        log.info("[Sending Subscription] {}", json); // 전송 직전 로그 추가!
                        ws.send(json);
                        sleep(250);
                    });

                    overseas.forEach((alias, trKey) -> {
                        String json = buildSubscribeJson(approvalKey, "HDFSCNT0", trKey);
                        log.info("[Sending Subscription] {}", json); // 전송 직전 로그 추가!
                        ws.send(json);
                        sleep(250);
                    });
                }

                @Override public void onMessage(WebSocket ws, String text) {

                    log.info("[WebSocket Message] {}", text);
                    if (text.startsWith("{")) {
                        // 여기서 구독 성공/실패 응답을 확인할 수 있어요.
                        // 성공 예: {"header":...,"body":{"rt_cd":"0", "msg1":"SUBSCRIPTION SUCCESSFUL"}}
                        // 실패 예: {"header":...,"body":{"rt_cd":"1", "msg1":"해당하는 종목이 없습니다."}}
                        return;
                    }

                    if (text.startsWith("{")) {
                        // SUBSCRIBE SUCCESS, PINGPONG 등
                        return;
                    }
                    String[] parts = text.split("\\|", 4);
                    if (parts.length < 4) return;

                    String trId    = parts[1];
                    String payload = parts[3];
                    String[] f = payload.split("\\^");

                    if ("H0STCNT0".equals(trId)) {
                        // 국내: key(0), 현재가(2), 전일대비(4), 등락률(5)
                        String key = safe(f,0);
                        quoteStore.upsertDomestic(key, safe(f,2), safe(f,4), safe(f,5));
                    } else if ("HDFSCNT0".equals(trId)) {
                        // 해외: key(0)=EXCD.SYMB, 대부분 현재가(2), 전일대비(4), 등락률(5)
                        String trKey = safe(f,0);
                        quoteStore.upsertOverseas(trKey, safe(f,2), safe(f,4), safe(f,5));
                    }
                }

                @Override public void onMessage(WebSocket ws, ByteString bytes) { /* no-op */ }
                @Override public void onFailure(WebSocket ws, Throwable t, Response r) { t.printStackTrace(); }
                @Override public void onClosing(WebSocket ws, int code, String reason) { ws.close(1000, null); }
            });

            // 데모: 메인 스레드 유지
            Thread.currentThread().join();
        };
    }

    private static void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException ignored) {} }
    private String safe(String[] a, int i) { return (i>=0 && i<a.length) ? a[i] : null; }

    private String buildSubscribeJson(String approvalKey, String trId, String trKey) {
        Map<String, Object> header = Map.of(
                "approval_key", approvalKey,
                "custtype", custtype,
                "tr_type", "1",
                "content-type", "utf-8"
        );
        Map<String, Object> input = Map.of("tr_id", trId, "tr_key", trKey);
        Map<String, Object> body = Map.of("input", input);
        Map<String, Object> root = Map.of("header", header, "body", body);
        try { return om.writeValueAsString(root); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}
