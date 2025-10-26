/*
JWT (JSON Web Token) 
<✨> 無狀態 (因毋庸存進DB)
<組成>  Header(含聲明類型+加密方法).Payload(傳遞內容,Reserved+Public+Private).Signature(Payload+Private+secret) 均base64 編碼產生
<驗證>  透過 Signature 
*/

// ===== 💻 JwtFilter.java =====

if(條件){
    filterChain.doFilter(request, response);
    return;
}
// <🔦> 讓請求續傳，並 return 結束當前 Filter 的執行

// <🔦> 攔截 請求，先驗證 是否夾帶 有效的JWT
/* 
詳:  請求屬於公開路徑 -> 跳過檢查
 <-> 其餘 ->
            s1. 檢查 token     存在?  取得並判斷 請求的 Authorization Header，是否 "有" 夾帶 Bearer 前綴的 token?
            s2. 驗證 token "有效性"?  驗證項目: 簽名、時效性
            s3-1. 驗成，建立 認證(Authentication)物件
                    -> 從 token 解析 使用者資訊
                       透過 UserDetailsService 取得完整的 UserDetails 物件
                       封裝成 UsernamePasswordAuthenticationToken，設定到 SecurityContextHolder
                        ✨ 實現 無狀態的授權，讓後續的 Spring Security 機制 識別 當前請求的使用者 身份 + 權限
            s3-2. 驗敗->清除 SecurityContextHolder 
                        續執，將錯誤處理邏輯  委派給 Spring Security 的統一入口點 ( AuthenticationEntryPoint)
*/

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
						// <🔦> 確保 每個請求只執行一次
						/* 
						詳: 首次執行，給 request 物件加特殊標記（如，".FILTERED" 屬性） <-> 後續執行中，先檢查請求有標記? 有就跳過不再執行。 
						*/

    private final JwtUtil jwtUtil;

    private final UserDetailsService userDetailsService;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) throws ServletException {
        String path = request.getServletPath();

        if (path.startsWith("/api")) {
            path = path.substring(4);
        }

        final String finalPath = path;

        boolean isPublicPath = Arrays.stream(PathConstantData.API_PUBLIC_ALL)
                .anyMatch(pathPattern -> pathMatcher.match(pathPattern, finalPath));
        
        return isPublicPath;
		/* 
		🪲 若未禁用自動註冊: 首次執行 (由 Servlet 容器觸發): 特定條件下（回傳 true），結果不設定標記
							第2次執行  (由 Spring Security 觸發): 沒標記，再次執行
			-> 不同上下文重複執行，干擾 Spring Security 建立安全上下文 -> 認定是需保護的未知請求，回傳browser 預設的index.html
		*/
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String bearerToken = request.getHeader("Authorization");

        if (!StringUtils.hasText(bearerToken) || !bearerToken.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String authToken = bearerToken.substring(7);

            if (jwtUtil.validateAccessToken(authToken)) {
                // Use unique email as username
                String username = jwtUtil.getEmailFromToken(authToken);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            // throw e // Allow request to continue, avoid returning 302
        }

        filterChain.doFilter(request, response);
    }
}

// ===== 💻 細節: JwtUtil.java =====

/*
🆚
Access Token  短效憑證（如15分鐘），攜帶完整的用戶資訊（id, email, roles），用於每次 API 請求的身份驗證
Refresh Token 長效憑證（如7天）  ，僅包含 UUID，                        存於 Redis (實現即時撤銷 + 自動過期清理) 做 server-side 驗證，用於 換發新的 Access Token
-> 降低 Access Token 被竊取風險
*/

@Component
public class JwtUtil {

	@Value("${jwt.secret}")
	private String secret;

	@Value("${jwt.access-token-expiration}")
	private long accessTokenExpiration;

	@Value("${jwt.refresh-token-expiration}")
	private long refreshTokenExpiration;

	private final SecretKey key;

	private final StringRedisTemplate redisTemplate;

	public JwtUtil(@Value("${jwt.secret}") String secret,
			@Value("${jwt.access-token-expiration}") long accessTokenExpiration,
			@Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration,
			StringRedisTemplate redisTemplate) {
		try {
			if (secret == null || secret.trim().isEmpty()) {
				throw new IllegalArgumentException("JWT secret is empty");
			}
			this.key = Keys.hmacShaKeyFor(secret.getBytes());
			this.accessTokenExpiration = accessTokenExpiration;
			this.refreshTokenExpiration = refreshTokenExpiration;
			this.redisTemplate = redisTemplate;
		} catch (Exception e) {
			throw e;
		}
	}

	public String generateAccessToken(Long id, UUID uuid, String email, Set<String> roles) {
		try {
			String accessToken = Jwts.builder()
					.setSubject(uuid.toString())
					.claim("id", id)
					.claim("email", email)
					.claim("roles", roles)
					.setIssuedAt(new Date())
					.setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
					.signWith(key, SignatureAlgorithm.HS256)
					.compact();

			return accessToken;
		} catch (Exception e) {
			throw e;
		}
	}

	public String generateRefreshToken(UUID uuid) {
		try {
			String refreshToken = Jwts.builder()
					.setSubject(uuid.toString())
					.setIssuedAt(new Date())
					.setExpiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
					.signWith(key, SignatureAlgorithm.HS256)
					.compact();

			redisTemplate.opsForValue().set("refresh_token:" + uuid, refreshToken, refreshTokenExpiration,
					TimeUnit.MILLISECONDS);

			return refreshToken;
		} catch (Exception e) {
			throw e;
		}
	}

	public boolean validateAccessToken(String token) {
		try {
			Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);

			return true;
		} catch (JwtException | IllegalArgumentException e) {

			return false;
		}
	}

	public boolean validateRefreshToken(UUID uuid, String refreshToken) {
		try {
			String stored = redisTemplate.opsForValue().get("refresh_token:" + uuid.toString());

			if (stored == null || !stored.equals(refreshToken)) {
				return false;
			}

			Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(refreshToken);
			return true;
		} catch (JwtException | IllegalArgumentException e) {
			return false;
		} catch (Exception e) {
			throw e;
		}
	}

	public String refreshAccessToken(Long id, UUID uuid, String email, Set<String> roles, String refreshToken) {
		if (!validateRefreshToken(uuid, refreshToken))
			throw new RuntimeException("Invalid refresh token");

		return generateAccessToken(id, uuid, email, roles);
	}

	public void revokeRefreshToken(UUID uuid) {
		redisTemplate.delete("refresh_token:" + uuid.toString());
	}

	public long getRefreshTokenExpiration() {
		return refreshTokenExpiration;
	}

	public String getEmailFromToken(String token) {
		return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().get("email",
				String.class);
	}

	public String getUuidFromRefreshToken(String refreshToken) {
		return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(refreshToken).getBody().getSubject();
	}
}
