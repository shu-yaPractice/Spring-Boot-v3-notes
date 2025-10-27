// ===== 📍FEATURE: 多種支付整合📍 =====

// 🎨 <設計模式> 结合 Strategy Pattern（策略模式）+ Factory Pattern（工廠模式）


// <執行流程> 當用戶端 請求支付: 建立 支付 -> 捕獲 支付 (因應 第3方金流支付常分 授權->實際扣款 2階段)

// ===== 1️⃣ 建立 支付 createPayment ===== 
    // s1. 請求參數 傳入必要資訊
        // 詳: 後端取得 user + 前端傳入 PaymentRequestDTO


    // s2. 防呆 : 👁️用order uuid 查詢，若訂單不存在就拋出異常 

    // s3. 初始化 Payment 實體 -> 儲存 所有更新的欄位值
        // ✨ 欄位transactionId, status, redirectUrl :  透過 PaymentGatewayFactory ，調用 PaymentGateway 的 createPayment方法 取得回應

    // s4. ❗ 如果是貨到付款支付方式，➕直接儲存 更新的order status為PROCESSING 、updateAt


    // s5. 返回給前端回應 
        // 詳: 成: 透過 Mapper 轉換格式為PaymentResponseDTO <-> 敗: 錯誤訊息


// ===== 2️⃣ 捕獲 支付 capturePayment (非同步回調機制) ===== 
    // s1. 請求參數 傳入必要資訊 
        // 詳: 前端傳入 PaymentGatewayRequestDTO


    // s2. 用 transactionId 查詢 支付紀錄

    // s3. ✨透過 PaymentGatewayFactory ，調用 PaymentGateway 的 capturePayment方法 取得支付最終狀態

    // s4. 儲存 更新的 payment status、transactionId

    // s5. 依支付最終結果，更新 Order
            // 詳: 成: 儲存 更新的order status、updateAt <-> 敗: 取消訂單


    // s6. 返回給前端回應 
        // 詳: 成: 透過 Mapper 轉換格式為PaymentCaptureResponseDTO -> 敗: 錯誤訊息
