// ===== 📍FEATURE📍 =====
   /* 
   <執行流程> 📁  Controller ，含使用DTO
               -> Service，含操作 Mapper
                          ->調用 Repository，操作Model         
   */


// ===== 📌Model📌 =====

// ===== 📌Repository 層📌 =====
public interface 實體Repository extends JpaRepository<實體, 主鍵型別> {
                            /* 首參 - Entity 類型     ，指定  操作的實體類別 ❗️需是 @Entity 標註的 domain model
                               二參 - Primary Key 類型，指定  主鍵 @Id 的資料型別 */
                            // <🔦> 自動產生 CRUD 方法 、影 findById(), deleteById()等參數型別
                            // <❗實務注意>  泛型 型別  ↔ Entity 的 @Id 型別 需一致 （否則 編譯錯誤）
    
    //❓ 方法選用 List 或 Optional 的考量    
    /*
    Optional<T> → 預期 0~1 筆結果  💡 唯一性
    List<T>     → 預期 0~N 筆結果  💡 關聯 @OneToMany、@ManyToOne

    ❗避 Optional<List<T>> 反模式
    */                    
}
// ===== 📌Service 層📌 =====

// ===== 📌含 Mapper ===== 

// ===== 📌Controller 層📌 =====

// ===== 📌含 DTO ===== 
