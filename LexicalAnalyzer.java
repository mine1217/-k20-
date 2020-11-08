package kc;

import java.util.Arrays;

/**
 * 字句解析プログラム(拡張済み)
 * @author 18-0136 牧野実
 * @date 6/3
 */
class LexicalAnalyzer {

	private FileScanner sourceFileScanner;//ソースファイルに対するスキャナ

    /**
     * FileScannerを初期化するだけのコンストラクタ
     *  @param ソースファイルの名前
     */
    LexicalAnalyzer(String sourceFileName) {
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

    		//++++++++++++++++++++++++++   数字だった時+++++++++++++++++++++++++++++++++
    		if (Character.isDigit(cu)) {
    			int num = Character.getNumericValue(cu);//char -> int
    			if (num == 0) { //初っ端から0だった時
    				if(this.sourceFileScanner.lookAhead() == 'x') { //0x~だった時
    					StringBuilder hex = new StringBuilder();
    					this.sourceFileScanner.nextChar();
    					while(Character.isLetterOrDigit(this.sourceFileScanner.lookAhead())) {
    							hex.append(this.sourceFileScanner.nextChar());
    					}
    					try {
    						return new Token (Symbol.INTEGER,Integer.parseInt(hex.toString(),16));
    					} catch (NumberFormatException e) { //もし16進数に変換できなかったら
    						System.out.print(e);
    						this.syntaxError();
    					}
				} else if(!Character.isLetter(this.sourceFileScanner.lookAhead())) //次が数字もしくは空白の場合
    					return new Token(Symbol.INTEGER,0);
    				else //それ以外の時
    					this.syntaxError();
    				return null;
    			} else { //1-9の時
    				while(Character.isDigit(this.sourceFileScanner.lookAhead())) { //次の文字が数値な限りぐるぐる回していく
    					num *= 10; //桁上げしてから
    					num += Character.getNumericValue(this.sourceFileScanner.nextChar()); //一の位に足していく&1文字読み込む
    				}
    				return new Token(Symbol.INTEGER, num);
    			}
    		//++++++++++++++++++++++++++++文字だった時+++++++++++++++++++++++++++++++++
    		} else if(Character.isLetter(cu) || cu == '_') {
    			StringBuilder bStr = new StringBuilder().append(cu);

    			//次の文字が英数字かアンダーバーである限りぐるぐる回す
    			while(Character.isLetterOrDigit(this.sourceFileScanner.lookAhead()) || this.sourceFileScanner.lookAhead() == '_') {
    				bStr.append(this.sourceFileScanner.nextChar()); //文字をどんどんビルダーに足していく
    			}
    			String str = bStr.toString(); //ビルダー->String

    			//***************予約語ゾーン*****************
    			if (str.equals("main"))
    				return new Token(Symbol.MAIN);
    			else if(str.equals("if"))
    				return new Token(Symbol.IF);
    			else if(str.equals("while"))
    				return new Token(Symbol.WHILE);
    			else if(str.equals("for"))
    				return new Token(Symbol.FOR);
    			else if(str.equals("inputint"))
    				return new Token(Symbol.INPUTINT);
    			else if(str.equals("inputchar"))
    				return new Token(Symbol.INPUTCHAR);
    			else if(str.equals("outputint"))
    				return new Token(Symbol.OUTPUTINT);
    			else if(str.equals("outputchar"))
    				return new Token(Symbol.OUTPUTCHAR);
    			else if(str.equals("outputstr"))
    				return new Token(Symbol.OUTPUTSTR);
    			else if(str.equals("setstr"))
    				return new Token(Symbol.SETSTR);
    			else if(str.equals("else"))
    				return new Token(Symbol.ELSE);
    			else if(str.equals("do"))
    				return new Token(Symbol.DO);
    			else if(str.equals("case"))
    				return new Token(Symbol.CASE);
    			else if(str.equals("switch"))
    				return new Token(Symbol.SWITCH);
    			else if(str.equals("break"))
    				return new Token(Symbol.BREAK);
    			else if(str.equals("continue"))
    				return new Token(Symbol.CONTINUE);
    			else if(str.equals("int"))
    				return new Token(Symbol.INT);
    			else if(str.equals("char"))
    				return new Token(Symbol.CHAR);
    			else if(str.equals("boolean"))
    				return new Token(Symbol.BOOLEAN);
    			else if(str.equals("true"))
    				return new Token(Symbol.TRUE);
    			else if(str.equals("false"))
    				return new Token(Symbol.FALSE);
    			//予約語以外
    			else {
    				return new Token(Symbol.NAME, str);
    			}
    		//++++++++++++++++++++++++++++++文字数字以外の時+++++++++++++++++++++++++++++++++
    		} else {
    			//************四則演算子ゾーン***********
    			if(cu == '+') {
    				char nC = this.sourceFileScanner.lookAhead();
    				if (nC == '=') {
    					this.sourceFileScanner.nextChar();
    					return new Token(Symbol.ASSIGNADD);
    				} else	if (nC == '+') {
    					this.sourceFileScanner.nextChar();
    					return new Token(Symbol.INC);
    				} else {
    					return new Token(Symbol.ADD);
    				}
    			} else if(cu == '-') {
    				char nC = this.sourceFileScanner.lookAhead();
    				if (nC == '=') {
    					this.sourceFileScanner.nextChar();
    					return new Token(Symbol.ASSIGNSUB);
    				} else	if (nC == '-') {
    					this.sourceFileScanner.nextChar();
    					return new Token(Symbol.DEC);
    				} else {
    					return new Token(Symbol.SUB);
    				}
    			} else if(cu == '*') {
    				char nC = this.sourceFileScanner.lookAhead();
    				if (nC == '=') {
    					this.sourceFileScanner.nextChar();
    					return new Token(Symbol.ASSIGNMUL);
    				} else {
    					return new Token(Symbol.MUL);
    				}
    			} else if(cu == '/') {
    				char nC = this.sourceFileScanner.lookAhead();
    				if (nC == '=') {
    					this.sourceFileScanner.nextChar();
    					return new Token(Symbol.ASSIGNDIV);
    					//*********コメントアウト************//
        			} else if(cu == '/') {
        				while (this.sourceFileScanner.lookAhead() != '\n' &&
        						  this.sourceFileScanner.lookAhead() != '\0') { //行末かファイル末まで読み飛ばす
        					this.sourceFileScanner.nextChar();
        				}
        				return new Token(Symbol.COMMENT);
        				//*********ここまで*******************//
    				} else {
    					return new Token(Symbol.DIV);
    				}
    			} else if(cu == '%') {
    				if (this.sourceFileScanner.lookAhead() == '=') {
    					this.sourceFileScanner.nextChar();
    					return new Token(Symbol.ASSIGNMOD);
    				} else
    					return new Token(Symbol.MOD);
    				//********比較演算子ゾーン********
    			} else if(cu == '=') {
    				char nC = this.sourceFileScanner.lookAhead();
    				if (nC == '=') {
    					this.sourceFileScanner.nextChar();
    					return new Token(Symbol.EQUAL);
    				} else {
    					return new Token(Symbol.ASSIGN);
    				}
    			} else if(cu == '<') {
    				if (this.sourceFileScanner.lookAhead() == '=') {
    					this.sourceFileScanner.nextChar();
    					return new Token(Symbol.LESSEQ);
    				} else
    					return new Token(Symbol.LESS);
    			} else if(cu == '>') {
    				if (this.sourceFileScanner.lookAhead() == '=') {
    					this.sourceFileScanner.nextChar();
    					return new Token(Symbol.GREATEQ);
    				} else
    					return new Token(Symbol.GREAT);
    			} else if(cu == '&') {
    				if ('&' == this.sourceFileScanner.lookAhead()) {
    					this.sourceFileScanner.nextChar();
    					return new Token(Symbol.AND);
    				} else {
    					this.syntaxError();
    					return null;
    				}
    			} else if(cu == '|') {
    				if ('|' == this.sourceFileScanner.lookAhead()) {
    					this.sourceFileScanner.nextChar();
    					return new Token(Symbol.OR);
    				} else {
    					this.syntaxError();
    					return null;
    				}
    			} else if(cu == '!') {
    				if (this.sourceFileScanner.lookAhead() == '=') {
    					this.sourceFileScanner.nextChar();
    					return new Token(Symbol.NOTEQ);
    				} else
    					return new Token(Symbol.NOT);
    				//*****コード実行の順序や、終了開始を制御する記号ゾーン*****
    			} else if(cu == ';') {
    				return new Token(Symbol.SEMICOLON);
    			} else if(cu == '(') {
    				return new Token(Symbol.LPAREN);
    			} else if(cu == ')') {
    				return new Token(Symbol.RPAREN);
    			} else if(cu == '{') {
    				return new Token(Symbol.LBRACE);
    			} else if(cu == '}') {
    				return new Token(Symbol.RBRACE);
    			} else if(cu == '[') {
    				return new Token(Symbol.LBRACKET);
    			} else if(cu == ']') {
    				return new Token(Symbol.RBRACKET);
    			} else if(cu == ',') {
    				return new Token(Symbol.COMMA);
    			} else if(cu == '\0') {
    				return new Token(Symbol.EOF);
    				//***************Character**************
    			} else if (cu=='\'') {
    				char ch = this.sourceFileScanner.nextChar();
    				if (ch == '\\') { //もし(\')だったらその次の文字を出力する。
    						ch = this.sourceFileScanner.nextChar();
    					if(this.sourceFileScanner.nextChar() == '\'')
    							return new Token(Symbol.CHARACTER, ch);
    					else {
    						this.syntaxError();
    						return null;
    					}
    				} else if(this.sourceFileScanner.lookAhead() == '\'') {
    					this.sourceFileScanner.nextChar();
    					return new Token(Symbol.CHARACTER, ch);
    				} else {
    					this.syntaxError();
    					return null;
    				}
    				//********************String*************************
    			} else if (cu == '"') {
    				char nc = this.sourceFileScanner.lookAhead(); //次の文字
    				StringBuilder str = new StringBuilder();
    				while (true) {
    					if (nc == '\n' || nc == '\0')
    						this.syntaxError();
    					else if(nc == '\\') { //もし次の文字が(\)だったらその次の文字を読み込む。
    						this.sourceFileScanner.nextChar();
    						if(this.sourceFileScanner.lookAhead() == 'n') { //次がnだったら改行コマンドを入れる
    							this.sourceFileScanner.nextChar();
    							str.append('\n');
    						} else
    							str.append(this.sourceFileScanner.nextChar());
    						nc = this.sourceFileScanner.lookAhead();
    						continue;
    					}else if(nc == '"')
    						break;

    					str.append(this.sourceFileScanner.nextChar());
    					nc = this.sourceFileScanner.lookAhead();
    				}
    				this.sourceFileScanner.nextChar();
    				return new Token (Symbol.STRING,str.toString());
    		    //++++++++++++++++++++それ以外+++++++++++++++++++++++
    			} else {
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