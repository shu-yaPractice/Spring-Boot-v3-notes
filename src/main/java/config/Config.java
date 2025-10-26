/*
開發流程常「功能驅動 」->
可先開發完整的業務功能（Controller, Service, Repository, Model），再配置規則
*/

// 💻  @Bean 必return物件實例

// ===== 常先建 ApplicationConfig.java =====
@Configuration
// 🔦 Spring 容器啟動階段- 在 ApplicationContext 初始化時被掃描
// @Configuration + @Bean- 手動註冊Bean (序: 無固定 <-> 取決依賴關係)
@RequiredArgsConstructor
    /*🔦Lombok → 自動生成 含特定參數的建構子
        讓Spring 看到建構子有參數 →> DI ，建構子注入方式
    */
public class ApplicationConfig {

    private final UserRepository userRepository;
    // <定性> 成員變數，欄位/屬性 
    // 💡 DI時機: 因 @Bean 方法的實作 用到 特定Bean -> 透過建構子 自動注入此依賴項 // <-> 不DI: @Bean 方法只 new 自建物件

    @Bean
    UserDetailsService userDetailsService() {
        return username -> userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found!"));
    }
    // UserDetailsService 是函數式介面 (只一個抽象方法)
    // username ->        是 lambda 表達式,編譯器自動轉成實作類別
    // orElseThrow        Spring Security 的契約要求，拋出異常 (UsernameNotFoundException)
    // return   介面實作物件 ( lambda 編譯器生成實作)

    @Bean
    PasswordEncoder getPasswordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
    // return  new新物件(物件實例)
}

// =====  =====

// =====  =====

// =====  =====

// =====  =====

// =====  =====
