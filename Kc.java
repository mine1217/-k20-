package kc;

import java.util.ArrayList;

/**
 * @author 18-0136 牧野実
 * @date 7/29
 * 構文解析プログラム 完成（コメント機能だけ追加ずみ)
 */
public class Kc {
    private LexicalAnalyzer lexer; //,仕様する字句解析器
    private Token token; //字句解析器から受け取ったトークン
    private VarTable vT; //変数表
    private PseudoIseg iseg; //プログラムのスタック
    private ArrayList<Integer> brList; //breakのアドレスを入れるリスト　なんでリストなのかは一つのWhile文に複数のbreak文があるかも知れんから
    boolean inLoop = false;

    /**
     * コンストラクタ
     * 字句解析器を読み取るファイル名を引数に入れてインスタンス化する。
     */
    Kc(String sourceFileName) {
       lexer = new LexicalAnalyzer (sourceFileName);
       vT = new VarTable();
       iseg = new PseudoIseg();
    }

    /**
     *プログラムの大枠部分
     */
    void parseProgram() {
       token = lexer.nextToken();
       if(token.checkSymbol(Symbol.MAIN)) this.parseMain_function();
       else this.syntaxError("\"main\"が期待されます");

       if(this.token.checkSymbol(Symbol.EOF))  this.iseg.appendCode(Operator.HALT);
       else this.syntaxError("プログラムが不正に終了しました");
       }

    /**
     * main関数の大枠解析
     */
    private void parseMain_function() {
    		if (token.checkSymbol(Symbol.MAIN)) token = lexer.nextToken();
    		else this.syntaxError("\"main\"が期待されます");
    		if(token.checkSymbol(Symbol.LPAREN)) token = lexer.nextToken();
    		else this.syntaxError("'('が期待されます");
    		if(token.checkSymbol(Symbol.RPAREN)) token = lexer.nextToken();
    		else this.syntaxError("')が期待されます");
    		if (token.checkSymbol(Symbol.LBRACE)) this.parseBlock();
    		else this.syntaxError("\"{\"が期待されます");
    }

    /**
     * main関数内の解析
     * 分岐先は、フィールど部分と、処理部分
     */
    private void parseBlock() {
    		if (token.checkSymbol(Symbol.LBRACE)) this.token = this.lexer.nextToken();
    		else this.syntaxError("'{'が期待されます");
    		while (token.checkSymbol(Symbol.INT)) this.parseDecl(); //"intが検出されるとフィールド解析へ、変数宣言とか
    		while (this.isStatement()) this.parseStatement(); //フィールドの解析が終わる、若しくはない場合は、処理部分に移る。
    		if (token.checkSymbol(Symbol.RBRACE)) this.token =  this.lexer.nextToken();
    		else this.syntaxError("'}'が期待されます");
    }

   /**
    * フィールドの大枠解析
    */
	private void parseDecl() {
		if (token.checkSymbol(Symbol.INT)) this.token = this.lexer.nextToken();
		else this.syntaxError("\"int\"が期待されます");
		if (token.checkSymbol(Symbol.NAME)) this.parseName_list();
		else this.syntaxError("変数が期待されます");
		if (token.checkSymbol(Symbol.SEMICOLON)) this.token = this.lexer.nextToken();
		else this.syntaxError("';'が期待されます");
	}

	/**
	 *  様々な処理のスタート地点
	 * ここからそれぞの処理へ分岐していく
	 */
	private void parseStatement() {
		switch(token.getSymbol()) {
		case IF:
			this.parseIf_statement();
			break;
		case WHILE:
			this.parseWhile_statement();
			break;
		case FOR:
			this.parseFor_statement();
			break;
		case OUTPUTCHAR:
			this.parseOutputchar_satement();
			break;
		case OUTPUTINT:
			this.parseOutputint_statement();
			break;
		case BREAK:
			this.parseBreak_statement();
			break;
		case LBRACE: //'{'から始まるのはサブブロック部分
			this.token = this.lexer.nextToken();
			while (this.isStatement()) { //カッコ内は自分を呼び出して、カッコの中の処理を解析する。
				this.parseStatement();
			}
			if(this.token.checkSymbol(Symbol.RBRACE))
				this.token = this.lexer.nextToken();
			else
				this.syntaxError("'}'が期待されます");
			break;
		case SEMICOLON:
			this.token = this.lexer.nextToken();
			break;
		case COMMENT:
			this.token = this.lexer.nextToken();
			break;
		default :
			if (this.isEXP()) //Exp_statementは条件が多すぎて、case書いていくのはめんどくさいので、あらかじめ識別用メソッドを作っておく
				this.parseExp_statement();
			else
				this.syntaxError("何らかの命令が期待されます");
			break;
		}
	}

	/**
	 * 変数の複数宣言に対応するための記号
	 */
	private void parseName_list() {
		if (token.checkSymbol(Symbol.NAME)) this.parseName(); //絶対に一つは変数がいる
		else this.syntaxError("変数が期待されます");
		while (token.checkSymbol(Symbol.COMMA)) {
			this.token = this.lexer.nextToken();
			if((token.checkSymbol(Symbol.NAME))) this.parseName();
			else this.syntaxError("変数が期待されます");
		}
	}

	/**
	 * 変数宣言の解析
	 */
	private void parseName() {
		String name = null;
		if (token.checkSymbol(Symbol.NAME)) {
			name = this.token.getStrValue();
			this.token = this.lexer.nextToken();
		} else this.syntaxError("変数が期待されます");

		switch (this.token.getSymbol()) {
		case ASSIGN : //単純に一個の変数に代入する場合
			this.token = this.lexer.nextToken();
			if(this.isConstant()) {
				if(!this.vT.registerNewVariable(Type.INT, name, 1)) this.syntaxError("同じ名前の変数を複数定義できません.");
				this.iseg.appendCode(Operator.PUSHI,this.parseConstant());
				this.iseg.appendCode(Operator.POP, vT.getAddress(name)); //ASSIGNにしないのは後で値を使わない&命令が二個で済むから？
			}
			else this.syntaxError("整数または文字が期待されます");
			break;

		case LBRACKET: //配列を扱うとき
			this.token = this.lexer.nextToken();
			if(this.token.checkSymbol(Symbol.INTEGER)) { // int x [y]; の形のとき
				int size = this.token.getIntValue();
				this.token = this.lexer.nextToken();
				if(this.token.checkSymbol(Symbol.RBRACKET)) {
					if(!this.vT.registerNewVariable(Type.ARRAYOFINT, name, size)) this.syntaxError("同じ名前の変数を複数定義できません.");
					this.token = this.lexer.nextToken();
				}
				else this.syntaxError("']'が期待されます");

			} else if (this.token.checkSymbol(Symbol.RBRACKET)) {// int x [] = {x1,x2,x3....}の形の時
				ArrayList<Integer> list = new ArrayList<>();//配列の要素
				this.token = this.lexer.nextToken();
				if (this.token.checkSymbol(Symbol.ASSIGN)) this.token = this.lexer.nextToken();
				else this.syntaxError("'='が期待されます");
				if(this.token.checkSymbol(Symbol.LBRACE)) this.token = this.lexer.nextToken();
				else this.syntaxError("'{'が期待されます");
				if(this.isConstant()) list = this.parseConstant_list();//中身の値をリストに詰めて帰ってくるので覚えておく
				else this.syntaxError("配列が期待されます");
				if(this.token.checkSymbol(Symbol.RBRACE)) {
					this.token = this.lexer.nextToken();
					if(!this.vT.registerNewVariable(Type.ARRAYOFINT, name, list.size())) this.syntaxError("同じ名前の変数を複数定義できません.");//要素数が分からんので、全部読んでからじゃ無いと変数表に登録できない
					for (int ad = this.vT.getAddress(name), i = 0; i < list.size(); i++) {//覚えておいた要素のリストをどんどんメモリにぶち込んでいく
						this.iseg.appendCode(Operator.PUSHI, list.get(i));
						this.iseg.appendCode(Operator.POP, ad + i);

					}
				}
				else this.syntaxError("'}'が期待されます");
			} else this.syntaxError("整数若しくは']'が期待されます");
			break;

		default :
			if(!this.vT.registerNewVariable(Type.INT, name, 1)) this.syntaxError("同じ名前の変数を複数定義できません.");
			break;
		}

	}

	/**
	 * 配列の要素の列
	 * ここでは命令を積まない
	 * Constanから帰って来た値をリストにしてnameに丸投げ
	 */
	private ArrayList<Integer> parseConstant_list() {
		ArrayList<Integer> list = new ArrayList<>();
		if(this.isConstant()) {
			list.add(this.parseConstant());
		} else syntaxError("整数若しくは文字が期待されます");
		while (this.token.checkSymbol(Symbol.COMMA)) {
			 this.token = this.lexer.nextToken();
			 if(this.isConstant()) list.add(this.parseConstant());
			else syntaxError("整数若しくは文字が期待されます");
		}
		return list;
	}

	/**
	 * 配列の要素一つ一つ
	 * ここでは命令を積まない Constant_listに値渡して丸投げ
	 */
	private int parseConstant() {
		int val;
		if(this.token.checkSymbol(Symbol.SUB)) {
			this.token = this.lexer.nextToken();
			if(this.token.checkSymbol(Symbol.INTEGER)) {
				val = this.token.getIntValue();
				this.token = this.lexer.nextToken();
				return - val;
			}
			else syntaxError("整数が期待されます");
		}else if(this.token.checkSymbol(Symbol.INTEGER)) {
			val = this.token.getIntValue();
			this.token = this.lexer.nextToken();
			return val;
		} else if(this.token.checkSymbol(Symbol.CHARACTER)) {
			val = this.token.getIntValue();
			this.token = this.lexer.nextToken();
			return val;
		} else syntaxError("整数若しくは文字が期待されます");
		return 0;
	}

	/**
	 * if文の解析
	 */
	private void parseIf_statement() {
		if(this.token.checkSymbol(Symbol.IF)) this.token = this.lexer.nextToken();
		else this.syntaxError("\"if\"が期待されます");
		if(this.token.checkSymbol(Symbol.LPAREN)) this.token = this.lexer.nextToken();
		else this.syntaxError("'('が期待されます");
		if(this.isEXP()) this.parseExpression(); //条件部分はexpressionに丸投げ
		else this.syntaxError("条件文が期待されます");
		if(this.token.checkSymbol(Symbol.RPAREN)) this.token = this.lexer.nextToken();
		else this.syntaxError("')'が期待されます");

		int beqAd = this.iseg.appendCode(Operator.BEQ, -1);//どこに飛べばいいかまだわからないので-1を入れておく
		if(this.isStatement()) {
			this.parseStatement(); //処理部分はstatementへ
			this.iseg.replaceCode(beqAd, this.iseg.getLastCodeAddress() + 1);//中の処理が終わったら、飛び先を処理の最後の次の番地に置き換える

		} else this.syntaxError("何らかの処理が期待されます");
	}

	/**
	 * while文の解析
	 * だいたいifと同じ
	 */
	private void parseWhile_statement() {
		if(this.token.checkSymbol(Symbol.WHILE)) this.token = this.lexer.nextToken();
		else this.syntaxError("\"while\"が期待されます");
		if(this.token.checkSymbol(Symbol.LPAREN)) this.token = this.lexer.nextToken();
		else this.syntaxError("'('が期待されます");

		//!!!!!!!!break前処理!!!!!!!!!!
		boolean oLoop = inLoop;//このWhile文に入る前のループ真偽を保持しておく　二重ループかどうか分からんから
		inLoop = true;
		ArrayList<Integer> oBrList = brList;//番地のリストも退避
		brList = new ArrayList<Integer>();//while文に入ったので番地が入るように初期化する。



		int expAd = this.iseg.getLastCodeAddress() + 1; //while文を回す度、条件文を評価する必要があるので、評価開始番地を覚えておく
		if(this.isEXP()) this.parseExpression();
		else this.syntaxError("条件文が期待されます");
		if(this.token.checkSymbol(Symbol.RPAREN)) this.token = this.lexer.nextToken();
		else this.syntaxError("')'が期待されます");

		int beqAd = this.iseg.appendCode(Operator.BEQ, -1);//どこに飛べばいいかまだわからないので-1を入れておく
		if(this.isStatement()) {
			this.parseStatement();
			this.iseg.appendCode(Operator.JUMP, expAd);//処理が終わったら、条件文の評価開始番地に戻る
			this.iseg.replaceCode(beqAd, this.iseg.getLastCodeAddress()+1);//BEQの番地をJUMPの次の番地に修正する

			//!!!!!!!!!!break処理!!!!!!!!!!!!!
			for(int a : brList) {//breakがあったら全てのJUMPオペランドを修正する。
				this.iseg.replaceCode(a, this.iseg.getLastCodeAddress()+1);
			}

		} else this.syntaxError("何らかの処理が期待されます");

		//!!!!!!!!!!!break後処理!!!!!!!!!!!!!
		inLoop = oLoop;//退避していたループ真偽を戻す;
		brList = oBrList;//アドレスリストも同様
	}

	/**
	 * for文の解析
	 * 条件は3つ固定 初期化; 条件; カウンタ
	 * foreach的なことはできない
	 */
	private void parseFor_statement() {
		if(this.token.checkSymbol(Symbol.FOR)) this.token = this.lexer.nextToken();
		else this.syntaxError("\"for\"が期待されます");
		if(this.token.checkSymbol(Symbol.LPAREN)) this.token = this.lexer.nextToken();
		else this.syntaxError("'('が期待されます");

		//!!!!!!!!!!! for (ここ; exp; exp) {statement}!!!!!!!!!!!!!!
		if(this.isEXP()) this.parseExpression();
		else this.syntaxError("初期化文が期待されます");
		if(this.token.checkSymbol(Symbol.SEMICOLON)) this.token = this.lexer.nextToken();
		else this.syntaxError("';'が期待されます");
		int exAd = this.iseg.appendCode(Operator.REMOVE); //条件文評価開始番地 REMOVE入れるのは多分ASSIGNした後にカスがスタックに残ってるから

		//!!!!!!!!!!! for (exp; ここ; exp) {statement}!!!!!!!!!!!!!!
		if(this.isEXP()) this.parseExpression();
		else this.syntaxError("条件文が期待されます");
		if(this.token.checkSymbol(Symbol.SEMICOLON)) this.token = this.lexer.nextToken();
		else this.syntaxError("';'が期待されます");
		int beqAd = this.iseg.appendCode(Operator.BEQ, -1); //for文内の処理外に飛びたいけどわからないから-1にして番地を覚えておく
		int coAd = this.iseg.appendCode(Operator.JUMP, -1);//for文内の処理開始番地に飛びたいけど、先にカウンタ文実行しないとダメなので-1にして番地覚えておく

		//!!!!!!!!!!! for (exp; exp; ここ) {statement}!!!!!!!!!!!!!!
		if(this.isEXP()) this.parseExpression();
		else this.syntaxError("実行文(カウンタ)が期待されます");
		this.iseg.appendCode(Operator.REMOVE);
		int mainAd = this.iseg.appendCode(Operator.JUMP, exAd+1); //カウンタ文終わったら評価に飛ぶ あと処理内に飛ぶ時に使う番地を覚えておく
		this.iseg.replaceCode(coAd, this.iseg.getLastCodeAddress() + 1); //評価終わった後のJUMP命令の番地を処理開始番地に修正

		//!!!!!!!!!!! for (exp; exp; exp) {ここ}!!!!!!!!!!!!!!
		if(this.token.checkSymbol(Symbol.RPAREN)) this.token = this.lexer.nextToken();
		else this.syntaxError("')'が期待されます");
		if(this.isStatement()) this.parseStatement();
		else this.syntaxError("何らかの処理が期待されます");
		this.iseg.appendCode(Operator.JUMP, coAd+1); //for文内の処理が一回終わったら、カウンタ文の開始番地に飛ぶ.
		this.iseg.replaceCode(beqAd, this.iseg.getLastCodeAddress() + 1); //条件の範囲外になった時に、処理外に飛ぶように、処理直後の番地にBEQのオペランドを修正.
	}

	/**
	 * いろんな処理を表す記号
	 */
	private void parseExp_statement() {
		if(isEXP()) this.parseExpression();
		else this.syntaxError("何らかの処理が期待されます");
		if(this.token.checkSymbol(Symbol.SEMICOLON)) {
			this.token = this.lexer.nextToken();
			this.iseg.appendCode(Operator.REMOVE);
		}
		else this.syntaxError("';'が期待されます");
	}

	/**
	 * 文字の出力
	 */
	private void parseOutputchar_satement() {
		if(this.token.checkSymbol(Symbol.OUTPUTCHAR)) this.token = this.lexer.nextToken();
		else this.syntaxError("\"outputchar\"が期待されます");
		if(this.token.checkSymbol(Symbol.LPAREN)) this.token = this.lexer.nextToken();
		else this.syntaxError("('が期待されます");
		if(this.isEXP()) this.parseExpression();
		else this.syntaxError("出力内容が期待されます");
		if(this.token.checkSymbol(Symbol.RPAREN)) this.token = this.lexer.nextToken();
		else this.syntaxError("')'が期待されます");
		if(this.token.checkSymbol(Symbol.SEMICOLON)) this.token = this.lexer.nextToken();
		else this.syntaxError("';'が期待されます");

		this.iseg.appendCode(Operator.OUTPUTC);
		this.iseg.appendCode(Operator.OUTPUTLN);//これは改行
	}

	/**
	 * 整数の出力
	 */
	private void parseOutputint_statement() {
		if(this.token.checkSymbol(Symbol.OUTPUTINT)) this.token = this.lexer.nextToken();
		else this.syntaxError("\"outputint\"が期待されます");
		if(this.token.checkSymbol(Symbol.LPAREN)) this.token = this.lexer.nextToken();
		else this.syntaxError("('が期待されます");
		if(this.isEXP()) this.parseExpression();
		else this.syntaxError("出力内容が期待されます");
		if(this.token.checkSymbol(Symbol.RPAREN)) this.token = this.lexer.nextToken();
		else this.syntaxError("')'が期待されます");
		if(this.token.checkSymbol(Symbol.SEMICOLON)) this.token = this.lexer.nextToken();
		else this.syntaxError("';'が期待されます");

		this.iseg.appendCode(Operator.OUTPUT);
		this.iseg.appendCode(Operator.OUTPUTLN);
	}

	/**
	 * break文の解析
	 */
	private void parseBreak_statement() {
		if(this.token.checkSymbol(Symbol.BREAK)) this.token = this.lexer.nextToken();
		else this.syntaxError("\"break\"が期待されます");

		if (inLoop == false) this.syntaxError("ループ外での\"break\"は無効です"); //inLoopはフィールドにある ループ内かどうかはwhile内で更新する
		this.brList.add(this.iseg.appendCode(Operator.JUMP,-1));//どこでループ外に出るか番地が分からないので、オペランドは-1に、JUMP命令の番地を専用のリストに保存しておく。

		if(this.token.checkSymbol(Symbol.SEMICOLON)) this.token = this.lexer.nextToken();
		else this.syntaxError("';'が期待されます");
	}

	/**
	 * 変数に代入する文であれば、ここで右辺、左辺それぞれ処理を仕分ける。
	 */
	private void parseExpression() {
		boolean bool = false; //左辺値かどうか
		if(this.isEXP()) bool = this.parseExp();
		else this.syntaxError("Expression内 何らかの処理が期待されます");
		Symbol[] syb = {Symbol.ASSIGN,
								Symbol.ASSIGNADD,
								Symbol.ASSIGNDIV,
								Symbol.ASSIGNMUL,
								Symbol.ASSIGNSUB,
								Symbol.ASSIGNMOD};
		if(this.checkSymbol(syb)) { //もし変数代入するんだったらここで右辺が処理される
			Symbol ope = this.token.getSymbol();
			if (!bool) this.syntaxError("左辺値が存在しません."); //前のExpが変数じゃなかったら出る.
			this.token = this.lexer.nextToken();

			if(!(ope == Symbol.ASSIGN)) { // '='以外の代入だったら
				this.iseg.appendCode(Operator.COPY);//あとで代入するのに使うのでアドレスをコピーしておく
				this.iseg.appendCode(Operator.LOAD);//計算するために左辺値のデータをロードする
			}
			if(this.isEXP()) {
				this.parseExpression();
				switch (ope) {//代入計算の場合は、代入する前に左辺値と右辺値の計算を代入前にする必要がある
				case ASSIGNADD:
					this.iseg.appendCode(Operator.ADD);
					break;
				case ASSIGNSUB:
					this.iseg.appendCode(Operator.SUB);
					break;
				case ASSIGNDIV:
					this.iseg.appendCode(Operator.DIV);
					break;
				case ASSIGNMUL:
					this.iseg.appendCode(Operator.MUL);
					break;
				case ASSIGNMOD:
					this.iseg.appendCode(Operator.MOD);
					break;
				}
				this.iseg.appendCode(Operator.ASSGN);
			}
			else this.syntaxError("Expression内 何らかの処理が期待されます");
		}
	}

	/**
	 * 複数の条件があって ||(or)があればここで仕分けられる
	 */
	private boolean parseExp() {
		boolean bool = false; //左辺値かどうか
		if(this.isEXP()) bool = this.parseLogical_term();//左辺値bool呼び出し元にage
		else this.syntaxError("EXP内 何らかの処理が期待されます");
		if(this.token.checkSymbol(Symbol.OR))  {
			this.token = this.lexer.nextToken();
			if(this.isEXP()) this.parseExp();
			else this.syntaxError("EXP内 何らかの処理が期待されます");
			this.iseg.appendCode(Operator.OR);
		}
		return bool;
	}

	/**
	 * 複数の条件があって &&(and)があればここで仕分けられる
	 */
	private boolean parseLogical_term() {
		boolean bool = false; //左辺値かどうか
		if(this.isEXP()) bool = this.parseLogical_factor(); //左辺値bool呼び出し元にage
		else this.syntaxError("Logical_term内 何らかの条件が期待されます");
		if(this.token.checkSymbol(Symbol.AND))  {
			this.token = this.lexer.nextToken();
			if(this.isEXP()) this.parseLogical_term();
			else this.syntaxError("Logical_term内 何らかの条件が期待されます");
			this.iseg.appendCode(Operator.AND);
		}
		return bool;
	}

	/**
	 * 条件文であれば、ここで右辺と左辺の処理が仕分けられて、比較される。
	 */
	private boolean parseLogical_factor() {
		boolean bool = false; //左辺値かどうか
		Symbol ope = null; //あとで分岐条件に使う比較演算子が入る
		Symbol[] syb = {Symbol.EQUAL,
								Symbol.NOTEQ,
								Symbol.LESS,
								Symbol.GREAT};

		if(this.isEXP()) bool = this.parseArithmetic_expression(); //左辺値bool呼び出し元にage
		else this.syntaxError("Logical_factor内 何らかの条件が期待されます");

		if(this.checkSymbol(syb)) {
			ope = token.getSymbol();
			this.token = this.lexer.nextToken();
			if(this.isEXP()) {
				this.parseArithmetic_expression();

				int compAd = this.iseg.appendCode(Operator.COMP);//比較するときは、JUMP命令を使うので処理し始めた番地を覚えておく
				switch (ope) {
				case EQUAL : 
					this.iseg.appendCode(Operator.BEQ, compAd+4); //==
					break;
				case NOTEQ :
					this.iseg.appendCode(Operator.BNE, compAd+4); //!=
					break;
				case LESS :
					this.iseg.appendCode(Operator.BLT, compAd+4); //<
					break;
				case GREAT :
					this.iseg.appendCode(Operator.BGT, compAd+4); //>
					break;
				}
				this.iseg.appendCode(Operator.PUSHI, 0);
				this.iseg.appendCode(Operator.JUMP, compAd+5);
				this.iseg.appendCode(Operator.PUSHI, 1);
			} else this.syntaxError("Logical_factor内 何らかの条件が期待されます");
		}
		return bool;
	}

	/**
	 * 算術演算 和か差であれば、ここで左辺と右辺に処理されて計算する。
	 */
	private boolean parseArithmetic_expression() {
		boolean bool = false; //左辺値かどうか
		Symbol[] syb = {Symbol.ADD,
								Symbol.SUB};

		if(this.isEXP()) bool = this.parseArithmetic_term(); //左辺値boolの継承 上にあげる
		else this.syntaxError("Arithmetic_expression内 何らかの処理が期待されます");

		while(this.checkSymbol(syb)) {
			Symbol ope = this.token.getSymbol();
			this.token = this.lexer.nextToken();
			if(this.isEXP())  this.parseArithmetic_term();
			else this.syntaxError("Arithmetic_expression内 何らかの処理が期待されます");
			if (ope == Symbol.ADD) this.iseg.appendCode(Operator.ADD);
			else this.iseg.appendCode(Operator.SUB);
		}
		return bool;
	}

	/**
	 * 算術演算 積か商であれば、ここで左辺と右辺に処理されて計算する。
	 */
	private boolean parseArithmetic_term() {
		boolean bool = false; //左辺値かどうか
		Symbol[] syb = {Symbol.MUL,
								Symbol.MOD,
								Symbol.DIV};

		if(this.isEXP()) bool = this.parseArithmetic_factor(); //左辺値boolの継承 上にあげる
		else this.syntaxError("Arithmetic_term内 何らかの処理が期待されます");

		while(this.checkSymbol(syb)) {
			Symbol ope = this.token.getSymbol();
			this.token = this.lexer.nextToken();
			if(this.isEXP())  this.parseArithmetic_factor();
			else this.syntaxError("Arithmetic_term内 何らかの処理が期待されます");
			if (ope == Symbol.MUL) this.iseg.appendCode(Operator.MUL);
			else if (ope == Symbol.MOD) this.iseg.appendCode(Operator.MOD);
			else this.iseg.appendCode(Operator.DIV);
		}
		return bool;
	}

	/**
	 * 負の符号か、NOTがついてると、ここで処理される。
	 */
	private boolean parseArithmetic_factor() {
		boolean bool = false; //左辺値かどうか
		switch (this.token.getSymbol()) {
		case SUB:
			this.token = this.lexer.nextToken();
			if(this.isEXP())this.parseArithmetic_factor();
			else this.syntaxError("Arithmetic_fector内 何らかの処理が必要です.");
			this.iseg.appendCode(Operator.CSIGN);//符号を逆転させるやつ
			break;
		case NOT:
			this.token = this.lexer.nextToken();
			if(this.isEXP())this.parseArithmetic_factor();
			else this.syntaxError("Arithmetic_fector内 何らかの処理が必要です.");
			this.iseg.appendCode(Operator.NOT);
			break;
		default :
			if(this.isUF()) bool = this.parseUnsigned_factor();
			else this.syntaxError("Arithmetic_factor内 何らかの処理が期待されます");
			break;
		}
		return bool;
	}

	/**
	 * いろんな処理の終着点
	 */
	private boolean parseUnsigned_factor() {
		boolean bool = false; //左辺値かどうか
		Symbol syb[] = { //もし次のトークンが代入系ならば、解析した変数は左辺値と言うことになる 判別用の配列
				Symbol.ASSIGN,
				Symbol.ASSIGNADD,
				Symbol.ASSIGNSUB,
				Symbol.ASSIGNDIV,
				Symbol.ASSIGNMUL,
				Symbol.ASSIGNMOD};
		Symbol ope = this.token.getSymbol();//変数の前に処理が来た時用の変数　保持しとかないとわかりまへん

		switch(this.token.getSymbol()) {
		case NAME: //変数の処理　変数単体でも受け入れる。
			String name = this.token.getStrValue();
			if(!this.vT.exist(name)) this.syntaxError("指定された名前の変数は存在しません.");
			int ad = this.vT.getAddress(name);
			boolean isArray = false; //配列かどうか　エラー検出に使う
			isArray = (this.vT.getType(name) == Type.ARRAYOFINT);
			this.token = this.lexer.nextToken();

			switch (token.getSymbol()) {
			//!!!!!!!!!!!ここは"後置"inc,decなので、元の値をコピーしてから処理したものに値を置き換える!!!!!!!!!!!!!!!
			case INC:
				this.token = this.lexer.nextToken();
				if(this.checkSymbol(syb)) this.syntaxError("左辺値にinc,decを使わないでください。"); // val++ = 1とか意味わからんのでエラー出す
				this.iseg.appendCode(Operator.PUSH,ad);
				this.iseg.appendCode(Operator.COPY,ad); //コピーすると元の値が残る
				this.iseg.appendCode(Operator.INC,ad);
				this.iseg.appendCode(Operator.POP,ad); //POPしても元のやつが残る ややこい
				break;
			case DEC:
				this.token = this.lexer.nextToken();
				if(this.checkSymbol(syb)) this.syntaxError("左辺値にinc,decを使わないでください。"); // val++ = 1とか意味わからんのでエラー出す
				this.iseg.appendCode(Operator.PUSH,ad);
				this.iseg.appendCode(Operator.COPY,ad);
				this.iseg.appendCode(Operator.DEC,ad);
				this.iseg.appendCode(Operator.POP,ad);
				break;

			case LBRACKET:
				if(!isArray) this.syntaxError("指定した変数は配列ではありません。インデックスを記述しないでください。");
				this.token = this.lexer.nextToken();
				this.iseg.appendCode(Operator.PUSHI,ad);
				if(this.isEXP()) this.parseExpression();
				else this.syntaxError("配列のインデックスを指定してください.");
				
				if(this.token.checkSymbol(Symbol.RBRACKET)) {
					this.token = this.lexer.nextToken();
					this.iseg.appendCode(Operator.ADD); //配列の頭の番地 + 添字をする
					if (this.checkSymbol(syb)) {
						bool = true;//左辺値だったら何もしない
					} else  this.iseg.appendCode(Operator.LOAD); //右辺値ならば、計算したアドレスのデータをスタックにプッシュ
					break;
				}
				else this.syntaxError("']'が期待されます");
			default:
				if(isArray) this.syntaxError("指定した変数は配列です。インデックスを指定してください。");
				if (this.checkSymbol(syb)) {
					this.iseg.appendCode(Operator.PUSHI,ad);
					bool = true;//左辺値ならば、アドレスをスタックにプッシュ
				} else  this.iseg.appendCode(Operator.PUSH,ad); //右辺値ならば、要素をスタックにプッシュ
				break;
			}
			break;

		case INC: // ++ -- の処理
		case DEC:
			ad = -1;
			this.token = this.lexer.nextToken();
			if(this.token.checkSymbol(Symbol.NAME)) {
				ad = this.vT.getAddress(this.token.getStrValue()); //後でPUSHするためのアドレス保管
				this.iseg.appendCode(Operator.PUSHI,ad);
				this.token = this.lexer.nextToken();
			} else this.syntaxError("'変数'が期待されます");

			if(this.token.checkSymbol(Symbol.LBRACKET)) { //++a[x]とかの時
				this.token = this.lexer.nextToken();
				if(this.isEXP()) this.parseExpression();
				else this.syntaxError("添字を指定してください");
				if(this.token.checkSymbol(Symbol.RBRACKET)) this.token = this.lexer.nextToken();
				else this.syntaxError("']'が期待されます");
				this.iseg.appendCode(Operator.ADD);
				this.iseg.appendCode(Operator.COPY);
				this.iseg.appendCode(Operator.LOAD);
			} else
				this.iseg.appendCode(Operator.PUSH,ad);

			if(this.checkSymbol(syb)) this.syntaxError("左辺値にinc,decを使わないでください。"); // val++ = 1とか意味わからんのでエラー出す
			if(ope == Symbol.INC)  this.iseg.appendCode(Operator.INC);
			else if(ope == Symbol.DEC) this.iseg.appendCode(Operator.DEC);
			this.iseg.appendCode(Operator.ASSGN);
			break;

		case INTEGER: //整数単体で受け入れる
			this.iseg.appendCode(Operator.PUSHI, this.token.getIntValue());
			this.token = this.lexer.nextToken();
			break;
		case CHARACTER: //文字単体で受け入れる
			this.iseg.appendCode(Operator.PUSHI, this.token.getIntValue());
			this.token = this.lexer.nextToken();
			break;
		case INPUTCHAR: //文字の入力
			this.token = this.lexer.nextToken();
			this.iseg.appendCode(Operator.INPUTC);
			break;
		case INPUTINT: //整数の入力
			this.token = this.lexer.nextToken();
			this.iseg.appendCode(Operator.INPUT);
			break;
		case ADD: //+(x,x2,x3..)の形の時
			this.parseSum_function();
			break;
		case MUL: //*(x,x2,x3..)の形
			this.parseProduct_function();
			break;
		case LPAREN: // (かっこ内の処理があったら,またexpressionに戻って解析する)
			this.token = this.lexer.nextToken();
			if(this.isEXP()) this.parseExpression();
			if(this.token.checkSymbol(Symbol.RPAREN)) this.token = this.lexer.nextToken();
			else this.syntaxError("')'が期待されます");
			break;
		default:
			this.syntaxError("Unsigned_factor内 何らかの処理が期待されます");
			break;
		}
		return bool;
	}

	/**
	 * +(x,x2,x3.....)の形、カッコの中を全部足していく
	 */
	private void parseSum_function() {
		int count = 0; //要素数
		if(this.token.checkSymbol(Symbol.ADD)) this.token = this.lexer.nextToken();
		else this.syntaxError("'+'が期待されます");
		if(this.token.checkSymbol(Symbol.LPAREN)) this.token = this.lexer.nextToken();
		else this.syntaxError("'('が期待されます");
		if(this.isEXP()) count = this.parseExpression_list(); //ここでは要素数を数えようが無いのでExpression_listからもらってくる
		else this.syntaxError("Sum_function内 何らかの処理が期待されます");
		if(this.token.checkSymbol(Symbol.RPAREN)) this.token = this.lexer.nextToken();
		else this.syntaxError("')'が期待されます");
		
		while(count > 0) {  //要素数-1回文ADDを入れる 
			this.iseg.appendCode(Operator.ADD);
			count--;
		}
	}

	/**
	 * *(x,x2,x3.....)の形、カッコの中を全部かけていく
	 */
	private void parseProduct_function() {
		int count = 0; //要素数
		if(this.token.checkSymbol(Symbol.MUL)) this.token = this.lexer.nextToken();
		else this.syntaxError("'*'が期待されます");
		if(this.token.checkSymbol(Symbol.LPAREN)) this.token = this.lexer.nextToken();
		else this.syntaxError("'('が期待されます");
		if(this.isEXP()) count = this.parseExpression_list();
		else this.syntaxError("Product_function内 何らかの処理が期待されます");
		if(this.token.checkSymbol(Symbol.RPAREN)) this.token = this.lexer.nextToken();
		else this.syntaxError("')'が期待されます");
		
		while(count > 0) {  //要素数-1回文MULを入れる 
			this.iseg.appendCode(Operator.MUL);
			count--;
		}
	}

	/**
	 * +(int,int,.....)と*(int,int,.....)の形で使う
	 * @return 中に入ってるintの数
	 */
	private int parseExpression_list() {
		int count = 0; //要素数
		if(this.isEXP()) this.parseExpression();
		else this.syntaxError("Expression_list内 何らかの処理が期待されます");
		while(this.token.checkSymbol(Symbol.COMMA)) {
			this.token = this.lexer.nextToken();
			if(this.isEXP()) this.parseExpression();
			else this.syntaxError("Expression_list内 何らかの処理が期待されます");
			count++;
		}
		return count;
	}

	private boolean checkSymbol (Symbol[] syb) {
        for (Symbol s : syb) {
        	if(token.getSymbol() == s)
        		return true;
        }
        return false;
	}

	/**
	 * Constantの識別用
	 * @return 真偽
	 */
	private boolean isConstant() {
		Symbol[] syb = {
				Symbol.SUB,
				Symbol.INTEGER,
				Symbol.CHARACTER
		};
		return this.checkSymbol(syb);
	}
    /**
     * Statementの識別用
     * @return 真偽
     */
	private boolean isStatement() {
		Symbol[] syb= {
				Symbol.IF,
				Symbol.WHILE,
				Symbol.FOR,
				Symbol.SUB,
				Symbol.NOT,
				Symbol.NAME,
				Symbol.INC,
				Symbol.DEC,
				Symbol.INTEGER,
				Symbol.CHARACTER,
				Symbol.LPAREN,
				Symbol.ADD,
				Symbol.MUL,
				Symbol.INPUTCHAR,
				Symbol.INPUTINT,
				Symbol.OUTPUTCHAR,
				Symbol.OUTPUTINT,
				Symbol.BREAK,
				Symbol.LBRACE,
				Symbol.COMMA,
				Symbol.SEMICOLON,
				Symbol.COMMENT};
		return this.checkSymbol(syb);
	}

	/**
	 * EXPの識別用
	 * @return 真偽
	 */
	private boolean isEXP() {
		Symbol[] syb = {
				Symbol.SUB,
				Symbol.NOT,
				Symbol.NAME,
				Symbol.INC,
				Symbol.DEC,
				Symbol.INTEGER,
				Symbol.CHARACTER,
				Symbol.LPAREN,
				Symbol.ADD,
				Symbol.MUL,
				Symbol.INPUTCHAR,
				Symbol.INPUTINT};
		return this.checkSymbol(syb);
	}

	/**
	 * Unsigned_factorの識別用
	 * @return 真偽
	 */
	private boolean isUF() {
		Symbol[] syb = {
				Symbol.NAME,
				Symbol.INC,
				Symbol.DEC,
				Symbol.INTEGER,
				Symbol.CHARACTER,
				Symbol.LPAREN,
				Symbol.ADD,
				Symbol.MUL,
				Symbol.INPUTCHAR,
				Symbol.INPUTINT};
		return this.checkSymbol(syb);
	}


	  //以降、必要なparse...メソッドを追記する。




	/**
	 * 現在読んでいるファイルを閉じる (lexerのcloseFile()に委譲)
	 */
	void closeFile() {
		lexer.closeFile();
	}

	/**
	 * アセンブラコードをファイルに出力する (isegのdump2file()に委譲)
	 */
	void dump2file() {
		iseg.dump2file();
	}

	/**
	 * アセンブラコードをファイルに出力する (isegのdump2file()に委譲)
	 *
	 * @param fileName 出力ファイル名
	 */
	void dump2file(String fileName) {
		iseg.dump2file(fileName);
	}

	/**
	 * エラーメッセージを出力しプログラムを終了する
	 *
	 * @param message 出力エラーメッセージ
	 */
	private void syntaxError(String message) {
		System.out.print(lexer.analyzeAt());
		//下記の文言は自動採点で使用するので変更しないでください。
		System.out.println("で構文解析プログラムが構文エラーを検出");
		System.out.println(message);
		closeFile();
		System.exit(1);
	}

	/**
	 * 引数で指定したK20言語ファイルを解析する 読み込んだファイルが文法上正しければアセンブラコードを出力する
	 */
	public static void main(String[] args) {
		Kc parser;

		if (args.length == 0) {
			System.out.println("Usage: java kc.Kc20 file [objectfile]");
			System.exit(0);
		}

		parser = new Kc(args[0]);

		parser.parseProgram();
		parser.closeFile();


		if (args.length == 1)
			parser.dump2file();
		else
			parser.dump2file(args[1]);

	}
}