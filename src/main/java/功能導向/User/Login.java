// ===== 📍FEATURE: 📍 =====

// ===== 📍FEATURE: 📍 =====

// ===== 📍FEATURE: 📍 =====

// ===== 📍FEATURE: 📍 =====

// ======================================================================

// ===== 📌Model📌 =====

// equals and hashCode ❓只比較主鍵
    // 理由: 比較 不可變 & 唯一欄位  ->確保 物件在生命週期的一致性
    /*  
    詳: 業務邏輯正確性：同 ID 是同實體
        JPA/Hibernate 要求：持久化前後的物件相等性
        集合操作穩定性：放入 Set/Map 時，不因欄位變更而改變 hash 值
    */
   /*
   💡 (有@ManyToMany 或 @OneToMany)用 Set 集合做關聯
      用 HashSet/HashMap 存放 User
      在 Service 層  比較不同來源的 User

      ❌ 不需要
        - 沒 @ManyToMany 或 @OneToMany 使用 Set
        - 不跨 session 比較實體
        - 沒用 contains/remove 等集合操作 
   */
    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;

        if (object == null || getClass() != object.getClass())
            return false;

        User user = (User) object;

        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

// ===== 📌Repository 層📌 =====

// ===== 📌Service 層📌 =====

// ===== 📌含 Mapper ===== 

// ===== 📌Controller 層📌 =====

// ===== 📌含 DTO ===== 
