package kc;

import java.util.ArrayList;

/**
 * 複数の変数を管理するプログラム
 * @author 牧野 実
 * @date 2020/05/27
 */
class VarTable {

	private ArrayList<Var> varList; //varリスト
	private int nextAddress; //次に登録される変数のアドレス

	/**
	 * コンストラクタ
	 * それぞれ初期化
	 */
    VarTable() {
        varList = new ArrayList<Var>();
        nextAddress = 0;
    }

    /**
     * 名前でvarを探す
     * もしなかったらnull返す
     * @param name 探したいvarの名前
     * @return ヒットしたvar
     */
    private Var getVar(String name) {
       for (Var v : varList) { //拡張forでくるくる
    	   if (v.getName().equals(name)) //名前がヒットしたら
    		   return v;
       }
       return null;
    }

    /**
     * 名前で検索して有無をbooleanで返す
     * @param name
     * @return 真偽
     */
    boolean exist(String name) {
    		if (this.getVar(name) != null) //名前検索でヒットしたら
    			return true;
    		else //しなかったら
    			return false;
    }

   /**
    * 新しい変数の追加
    * @param type 種類
    * @param name 名前
    * @param size 大きさ
    * @return 追加できたかどうかの真偽
    */
    boolean registerNewVariable(Type type, String name, int size) {
    		if (!this.exist(name)) {//もし同じ名前の変数なかったら
    			varList.add(new Var(type, name, nextAddress, size)); //新しいVarを作成しアドレスをインクリメントしつつ追加
    			nextAddress += size;
    			return true;
    		}else { //同名変数があったら
    			return false;
    		}
    }

    /**
     * 変数を名前で探してそのアドレスを返す
     * @param name 名前
     * @return アドレス
     */
    int getAddress(String name) {
    		return this.getVar(name).getAddress();
    }

    /**
     * 変数を名前で探してその型を返す
     * @param name 名前
     * @return 型
     */
    Type getType(String name) {
    		return this.getVar(name).getType();
    }

    /**
     * 変数の型があっとるかどうか
     * @param name 名前
     * @param type 型
     * @return 真偽
     */
    boolean checkType(String name, Type type) {
    		return this.getVar(name).getType().equals(type);
    }

    /**
     * 変数を名前で探してサイズを返す
     * @param name 名前
     * @return サイズ
     */
    int getSize(String name) {
    		return this.getVar(name).getSize();
    }

    /**
     * 動作確認用のメインメソッド
     * int型変数およびint型配列を表に登録し、その後登録された変数を表示する
     */
    public static void main(String[] args) {
    		VarTable varTable = new VarTable();
    		
    		for (int i = 0; i < 4; i++) { //4つの変数を追加
    			varTable.registerNewVariable(Type.INT, "var" + i, 1);
    		}
    		
    		varTable.registerNewVariable(Type.ARRAYOFINT, "var4", 10);//一つの配列を追加
    		
    		for (int i = 0; i < 5; i++) { //確認フェーズ
    			if(varTable.checkType("var" + i, Type.INT)) //もしINTだったら
    				System.out.println("var" + i + "のtype: " + varTable.getType("var" + i) + ", address: " + varTable.getAddress("var" + i));
    			else if (varTable.checkType("var" + i, Type.ARRAYOFINT))//もしINTじゃなくてARRAYOFINTだったら
    				System.out.println("var" + i + "のtype: " + varTable.getType("var" + i) 
    					+ ", address: " + varTable.getAddress("var" + i) + ", size: " + varTable.getSize("var" + i));
    			else { //それ以外の場合
    				System.out.println("お宅なんか間違えてますよ");
    			}
    		}
    }
}