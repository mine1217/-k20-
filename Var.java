package kc;

/**
 * 変数を表すオブジェクト
 * @author 牧野 実
 * @date 2020/05/27
 */
class Var {
   
	private Type type; //型
	private String name; //変数名
	private int address; //Dseg上のアドレス
	private int size; //配列の場合、そのサイズ
    
/**
 * それぞれ代入して初期化
 * @param type
 * @param name
 * @param address
 * @param size
 */
    Var(Type type, String name, int address, int size) {
    		this.type = type;
    		this.name = name;
    		this.address = address;
    		this.size = size;
    }

   /**
    * ゲッター
    */
    Type getType() {
     return type;
    }

    /**
     * ゲッター
     */
    String getName() {
       return name;
    }

    /**
     * ゲッター
     */
    int getAddress() {
        return address;
    }

    /**
     * ゲッター
     */
    int getSize() {
       return size;
    }
}