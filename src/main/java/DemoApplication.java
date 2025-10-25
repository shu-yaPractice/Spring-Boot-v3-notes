
@SpringBootApplication
@EnableScheduling
public class 自定義 extends SpringBootServletInitializer {
					// 🔦應用 可部署到外部 Servlet 容器

	/*
	🔦 總結: main方法    作為Java 程式的進入點
		  	 執行run方法  *略*
	*/	
	public static void main(String[] args也可自定義) {
	// 	public	: 公開，讓 JVM 可從外部呼叫
	//  static	: 靜態，讓 JVM 不用建立物件實例 就能執行
	//  void	: 不回傳值
	//  String args[] : 接收 命令列參數 的陣列

		SpringApplication.run(DemoApplication.class, args也可自定義);
	}

	/*
	🔦 總結: configure 方法  接收外部容器建立的空白建造者
	         執行sources方法 指定配置
			 返回            配置好的建構者，讓外部容器(Tomcat) 完成啟動
	*/
	@Override
	// 父類是 SpringBootServletInitializer // 🔦 覆寫，返回時 告知應用的入口點
	protected SpringApplicationBuilder configure(SpringApplicationBuilder 建造者對象) {
	// protected : 只被 Servlet 容器的內部機制調用
	// SpringApplicationBuilder : Spring Boot 提供的建造者模式（Builder Pattern）類，配置+構建 SpringApplication
	// configure  : Servlet 容器 自動調用這個方法
	// 參數: 傳入外部容器 (Tomcat) 建立的建造者對象

		return 建造者對象.sources(DemoApplication.class);
		// .sources() : 設定 Spring Boot 應用的配置來源
		// return : 從 DemoApplication.class 開始掃描+配置完成的SpringApplicationBuilder 對象，繼續構建 Spring Context
	}
}