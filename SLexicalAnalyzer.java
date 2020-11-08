package kc;

import java.util.Arrays;

/**
 * 字句解析プログラム
 * @author 18-0136 牧野実
 * @date 6/3
 */
class SLexicalAnalyzer {	
	
	private FileScanner sourceFileScanner;//ソースファイルに対するスキャナ
	
    /**
     * FileScannerを初期化するだけのコンストラクタ
     *  @param ソースファイルの名前
     */
    SLexicalAnalyzer(String sourceFileName) {
        this.sourceFileScanner = new FileScanner(sourceFileName);
    }

    /**
     * 
     */
    	Token nextToken() {
    		char cu = this.sourceFileScanner.nextChar();//一発目読み込む
    		
    		//スペースとか改行だったら呼び飛ばす
    		Character[] cL =  {' ' , '\t', '\n'}; //検索用のリスト リストなのでcharじゃなくてCharacterにしないとおかしくなるらしい
    		while(Arrays.asList(cL).contains(cu)) { //条件いっぱいめんどくさいのでリストに変換してから要素を比較するメソッドを使う
    			cu = this.sourceFileScanner.nextChar();
    		}
    		
    		//数字だったら
    		if (Character.isDigit(cu)) {
    			int num = Character.getNumericValue(cu);//char -> int
    			if (num == 0) { //初っ端から0だった時
    				return new Token(Symbol.INTEGER,0);
    			}else { //それ以外だった時
    				while(Character.isDigit(this.sourceFileScanner.lookAhead())) { //次の文字が数値な限りぐるぐる回していく
    					num *= 10; //桁上げしてから
    					num += Character.getNumericValue(this.sourceFileScanner.nextChar()); //一の位に足していく&1文字読み込む
    				}
    				return new Token(Symbol.INTEGER, num);
    			}
    		}else { //文字だった時
    			if(cu == '+') { 
    				return new Token(Symbol.ADD);
    			}else if(cu == '!') {
    				char nC = this.sourceFileScanner.lookAhead();
    				if(nC == '=') {
    					this.sourceFileScanner.nextChar();
    					return new Token(Symbol.NOTEQ);
    				} else {
    					return new Token(Symbol.NOT);
    				}
    			}else if(cu == '=') {
    				char nC = this.sourceFileScanner.lookAhead();
    				if (nC == '=') {
    					this.sourceFileScanner.nextChar();
    					return new Token(Symbol.EQUAL);
    				} else {
    					return new Token(Symbol.ASSIGN);
    				}
    			}else if(cu == '\0') {
    				return new Token(Symbol.EOF);
    			} else { //オペランド以外
    				char nC = this.sourceFileScanner.lookAhead();
    				this.syntaxError(); //今回は全部エラー処理とする
    				return null; //これ書かないと怒られる
    			}
    		}
     }

    /**
     *  ファイルを閉じる
     */
    void closeFile() {
        sourceFileScanner.closeFile();
    }

    /**
     * 現在の解析場所を返す(行列
     */
    	String analyzeAt() {
    		return this.sourceFileScanner.scanAt();
    	}

    /** 
     * 
     */
    private void syntaxError() {
        System.out.print (sourceFileScanner.scanAt());
        //下記の文言は自動採点で使用するので変更しないでください。
        System.out.println ("で字句解析プログラムが構文エラーを検出");
        closeFile();
        System.exit(1);
    }
}