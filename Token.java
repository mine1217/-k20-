package kc;

/**
 * トークンを表すオブジェクト
 * @author 18-0136 牧野実
 * @date 6/3
 */
class Token {
    private Symbol symbol;//トークンの種別
    private int intValue;//整数値もしくはcharの文字コードを保持する
    private String strValue;//名前(NAME)または文字列を保持する
    
    /** 
     * 整数、文字、名前以外のトークンの時
     */
    Token(Symbol symbol) {
       this.symbol = symbol;
    }

    /** 
     * 整数、文字トークンの時
     */
    Token(Symbol symbol, int intValue) {
       this.symbol = symbol;
       this.intValue = intValue;
    }
    

    /** 
     * 名前、文字列トークンの時
     */
    Token(Symbol symbol, String strValue) {
       this.symbol = symbol;
       this.strValue = strValue;
    }


    /** 
     * 引数の種別と、このオブジェクトのフィールドで保持している種別が一緒かどうか
     * @param symbolType 種別
     * @return あったら真
     */
    boolean checkSymbol(Symbol symbolType) {  
       return this.symbol == symbolType;
    }
   
    
    /**
     * ゲッター
     * @return 種別
     */
    Symbol getSymbol() {  
    		return this.symbol;
    }

    /**
     * ゲッター
     * @return 数値
     */
    int getIntValue() {  
    		return this.intValue;
    }

    /**
     * ゲッター
     * @return 文字列
     */
    String getStrValue() {  
    		return this.strValue;
    }
}