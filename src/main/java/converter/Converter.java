// ===== 💻 Code template: Converter.java =====

@Component
public class StringToEnumNameConverter implements Converter<String, EnumName> {
                                                // 泛型介面<S (source type), T (轉換成target type)>

    @Override
    public EnumName convert(@NonNull String source) {
        try {
            return EnumName.valueOf(source.trim().toUpperCase());
                // 參數      來源 去空白、轉大寫
                // valueOf() 轉換成enum
        } catch (IllegalArgumentException e) {
            return null;
            //  輸入值 不符enum定義
        }
    }
}
// <🔦> 請求參數的型別(String) -自動轉換成-> Enum型別
// <⏰> Controller 接收 @RequestParam 或 @PathVariable
