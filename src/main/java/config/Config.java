/* 開發流程常「功能驅動 」-> 可先開發完整的業務功能（Controller, Service, Repository, Model），再配置規則 */

// ===== 💻 Code template: Config.java =====
/*
整個Class
*/

@Configuration
// <🔦> Spring 容器啟動階段- 在 ApplicationContext 初始化時被掃描
//  💻 ➕@Bean- 手動註冊Bean (序: 不照 定義順序 <-> 取決 依賴關係)

@RequiredArgsConstructor // ➕可選 
/*🔦Lombok → 自動生成 含特定參數的建構子
    讓Spring 看到建構子有參數 →> DI ，建構子注入方式
*/
/* 
   💻 搭配  注入private final 的其他Bean
            方法 調用其他Bean的方法
*/

/*
Class內部
*/
private final 型別 其他Bean
// <定性> 成員變數，欄位/屬性 、also is 被注入的依賴
// 💡 DI時機: @Bean 方法的實作 需要它檔案Bean -> 透過建構子 自動注入此依賴項 // <-> 不DI: @Bean 方法只 new 自建物件


@Value
// 從Spring Environment (如 讀取properties 檔)中讀取屬性值 ， 注入環境變數

@Bean 
// 必 return 物件實例 (🟥使用特定語法 🟧自行new物件)


// ===== 1️⃣ 常先建 整個應用共享Bean: ApplicationConfig.java =====

@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

    private final UserRepository userRepository;

    @Bean
    UserDetai<->適合傳統 Web:lsService userDetailsService() {
        return username -> userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found!"));
    }
    // UserDetailsService 是函數式介面 (只一個抽象方法)
    // username ->        是 lambda 表達式,編譯器 自動轉成實作類別
    // orElseThrow        Spring Security 的契約要求，拋出異常 (UsernameNotFoundException)
    // return   介面實作物件 ( lambda 編譯器生成實作)

    @Bean
    PasswordEncoder getPasswordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
    // return  new新物件
}

// ===== 2️⃣ 常建API前，定義安全核心: SecurityConfig.java =====
// 設定 一個應用 AOP 思想的安全框架  (定義規則 被以APO方式應用)

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtFilter jwtFilter;
        private final OAuth2AuthSuccessHandler oAuth2AuthSuccessHandler;
        private final LogoutResultHandler logoutResultHandler;

        @Value("${frontend.url}")
        private String frontendUrl;

        /* Runtime 請求處理流程
        1. CORS Preflight：OPTIONS 請求先過 CORS Filter
        2. JwtFilter 攔截：驗證 JWT token，寫入 SecurityContext
        3. AuthorizeHttpRequests：檢查 URL 是否 permitAll 或需要 authenticated
        4. 失敗處理：觸發 customAuthenticationEntryPoint 回傳 401 JSON

        特殊流程:
        OAuth2 登入成功：觸發 oAuth2AuthSuccessHandler
        Logout：清除 authentication，執行 logoutResultHandler
        */

        /*
        核心
        */
        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                        .cors(cors -> {
                        })
                        // 🔦 啟用 CORS 支援
                        /*
                        詳:  <⏰>：每個 HTTP 請求都先經過 CorsFilter
                             空 Lambda：先用預設配置，再自動配用 corsConfigurationSource 的 Bean 配置
                        */
                        .csrf(csrf -> csrf.disable())
                        // 🔦 停用 CSRF (Cross-Site Request Forgery) 保護
                        // 💡 停用時機: 身分認證 依賴 JWT，不依賴 Session Cookie
                        .sessionManagement(session -> session
                                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                        /* 💡 參數 IF_REQUIRED: Spring Security 需要時才建Session // 💡兼容 支援有狀態的 OAuth2 登入流程 + 無狀態的 JWT API 請求
                                  STATELESS：完全不建立 Session（純 JWT API ) 
                                  ALWAYS：每次請求都建立 <-> NEVER：不主動建立，但用既有的
                        */
                        .oauth2Login(oauth2 -> oauth2
                                        .successHandler(oAuth2AuthSuccessHandler))
                        // 🔦 啟用 OAuth2 登入
                        .logout(logout -> logout.logoutUrl(API_LOGOUT)
                                        .logoutSuccessHandler(logoutResultHandler)
                                        .clearAuthentication(true))
                        /*
                        詳: 指定登出 API 路徑 -> 自定義回傳 JSON (因無狀態 API)（<->適合傳統 Web: 預設 重導向）-> 清除 SecurityContext 的 Authentication 物件
                        */
                        .exceptionHandling(exceptions -> exceptions
                                        .authenticationEntryPoint(customAuthenticationEntryPoint()))
                        // 🔦 處理「未認證」異常  e.g. JWT token 無效or過期、請求需認證但未夾帶token、JwtFilter 認證失敗
                        // 自定義回傳 JSON (因無狀態 API) (<->適合傳統 Web: 預設重導向登入頁)
                        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                        /* 🔦 <在Filter Chain位置> 執行期，把 jwtFilter 加在 指定對象前
                                                                        (如 Spring Security 標準認證流程 ，e.g. 傳統表單登入) 
                                                                        (Token-based Authentication，若  API Token 有效則立即完成認證 😄 API 的無狀態性 + 效率)*/
                        .authorizeHttpRequests(authorize -> authorize  // Lambda 參數
                                        .requestMatchers(API_PUBLIC_ALL).permitAll()
                                        // 定義 公開端點 -> 規則: 跳過認證 

                                        .anyRequest().authenticated()); 
                                        // 定義 其餘請求 -> 規則: 需認證
                        // 🔦 定義 請求時的規則

                return http.build();
                // 🔦 建構並回傳 `DefaultSecurityFilterChain` 物件
                /*
                詳:   1. 組裝所有 Filter
                      2. 建立 Filter Chain 實例
                      3. 註冊到 Spring Security Filter Chain Proxy
                      4. Spring MVC 的 `DelegatingFilterProxy` 委派給這個 Filter Chain
                */
        }

        // <🔦> 禁用自動註冊 <-> 確保 jwt filter 只在 Spring Security 註冊1次
        @Bean
        public FilterRegistrationBean<JwtFilter> jwtFilterRegistration(JwtFilter jwtFilter) {
                FilterRegistrationBean<JwtFilter> registration = new FilterRegistrationBean<>(jwtFilter);
        
                registration.setEnabled(false);
                
                return registration;
        }

        /*
        支援
        */

        // <🔦>  提供 CORS 配置 給 CORS Filter
        // <⏰>  在  Spring Security 最前面 過濾跨域請求
        @Bean
        CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration config = new CorsConfiguration();

                config.setAllowedOrigins(List.of(
                                frontendUrl,
                                API_DNS));
                config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                config.setAllowedHeaders(List.of("*"));
                config.setAllowCredentials(true);
                config.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", config);

                return source;
        }

        // <🔦> 負責認證邏輯
        // 詳: 委派給內部的 AuthenticationProvider 鏈驗證。Provider 檢查 token 或憑證，驗證成功: 回傳 Authentication 物件 <->失敗: 拋錯 AuthenticationException
        // <⏰> 需要驗證時主動調用 (e.g. OAuth2)
        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
                        throws Exception {
                return authenticationConfiguration.getAuthenticationManager();
        }

        // <🔦>  處理 認證失敗後的 統一回應格式
        // <⏰>  認證失敗後被，動觸發回傳 401
        @Bean
        public AuthenticationEntryPoint customAuthenticationEntryPoint() {
                return (request, response, authException) -> {
                        response.setContentType("application/json");
                        response.setStatus(HttpStatus.UNAUTHORIZED.value());

                        ObjectMapper mapper = new ObjectMapper();
                        String jsonResponse = mapper.writeValueAsString(
                                        java.util.Map.of("message", "Unauthorized: Token is invalid or expired"));

                        response.getWriter().write(jsonResponse);
                };
        }
}

// ===== 3️⃣ Web 層的通用配置: WebConfig.java =====
// 設定 一個應用 AOP 思想的Web框架 (定義規則 被以APO方式應用)

// <🔦> Spring MVC 的配置類，註冊 自定義的類型轉換器。
// <💡> Controller 的方法參數 需接收 enum類型，Spring 自動調用 Converter 進行類型轉換
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(@NonNull FormatterRegistry registry) {
        registry.addConverter(new StringToCategoryConverter());
        registry.addConverter(new StringToSortDirectionConverter());
    }
}

// ===== 4️⃣ 專屬配置: RedisSessionConfig.java =====

// <🔦> 分散式 session 管理: Spring Session 的儲存機制 從本地記憶體 切換到 Redis
/* <💡> 1.微服務架構 / 多實例部署
        多台 server 共享 session（Load Balancer 後的水平擴展），避 sticky session 的負載不均

        2.前後端分離的跨域架構
        前端（www.example.com）和後端 API（api.example.com）需共享登入狀態
        sameSite="None" 配合 CORS 實現跨域認證

        3.高併發電商系統
        購物車、結帳流程需穩定的 session，Redis 的高效能避免 DB 壓力

        4.需 session 持久化
        伺服器重啟後 session 不遺失、實現 session 的集中管理與監控
*/
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800)
public class RedisSessionConfig {

    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();

        serializer.setCookieName("JSESSIONID");
        serializer.setCookiePath("/onlineShop");
        serializer.setDomainNamePattern("^.+?\\.(\\w+\\.[a-z]+)$"); //用正則提取 主域名，讓 cookie 可跨子域名共享
        serializer.setUseSecureCookie(true); // 只在 HTTPS 傳輸，防 中間人攻擊
        serializer.setSameSite("None"); // 允許跨站請求攜帶 cookie（搭配 Secure）
        serializer.setUseHttpOnlyCookie(true); // 防 JavaScript 存取，避 XSS 攻擊

        return serializer;
    }
}
