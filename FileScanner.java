package kc;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * プログラムファイルを読み込むプログラム
 * @author 牧野 実
 * @date 2020/05/20
 */
class FileScanner {

	private BufferedReader sourceFile; //入力ファイルの参照
	private String line; //行バッファ
	private int lineNumber; //行カウンタ
	private int columnNumber; //列カウンタ
	private char currentCharacter; //読み取り文字
	private char nextCharacter; //先読み文字

	/**
	 * 引数 sourceFileName で指定されたファイルを開き, sourceFile で参照する．
	 * 教科書 p. 210 ソースコード 10.1 ではtry-with-resources 文を用いて
	 * ファイルの 参照と読み取りを一度に行っているが，このコンストラクタでは
	 * ファイルの参照 だけを行う．
	 * また lineNumber, columnNumber, currentCharacter, nextCharacter を初期化する
	 *
	 * @param sourceFileName ソースプログラムのファイル名
	 */
	FileScanner(String sourceFileName) {

		Path path = Paths.get(sourceFileName);
		// ファイルのオープン
		try {
			sourceFile = Files.newBufferedReader(path);
		} catch (IOException err_mes) {
			System.out.println(err_mes);
			System.exit(1);
		}

		// 各フィールドの初期化
		lineNumber  = 0;
		columnNumber = -1;
		nextCharacter = '\n';

		nextChar(); //最初の一文字読み込む
	}

	/**
	 * sourceFileで参照しているファイルを閉じる
	 */
	void closeFile() {
		try {
			sourceFile.close();
		} catch (IOException err_mes) {
			System.out.println(err_mes);
			System.exit(1);
		}
	}

	/**
	 * sourceFile で参照しているファイルから一行読み, フィールド line(文字列変数) に
	 * その行を格納する 教科書 p. 210 ソースコード10.1 では while文で全行を読み取って
	 * いるが，このメソッド内では while文は使わず1行だけ読み取りフィールドline に格納する．
	 */
	void readNextLine() {
		try {
			if (sourceFile.ready()) { // sourceFile中に未読の行があるかを確認 (例外:IllegalStateException)
				line = sourceFile.readLine() + '\n'; //一行読み込んで末尾に改行コマンド
			} else {
				line = null; //最後まできたらヌル
			}
		} catch (IOException err_mes) { // 例外は Exception でキャッチしてもいい
			// ファイルの読み出しエラーが発生したときの処理
			System.out.println(err_mes);
			closeFile();
			System.exit(1);
		}
	}

	/**
	 * 文字を返す
	 */
	char lookAhead () {
		return nextCharacter;
	}

	/**
	 *文字列を返す
	 */
	String getLine() {
		return line;
	}

	/**
	 * 1文字を進めて読み取る
	 * @return 現在の文字
	 */
	char nextChar() {
		currentCharacter = nextCharacter;

		switch(nextCharacter) { //次の文字によって分岐

		case '\0' : //ファイル末
			break;

		case '\n': //行末
			this.readNextLine();
			if(this.getLine() == null) { //次行がファイル末
				nextCharacter = '\0';
			}else {//次行が存在する
				nextCharacter = this.getLine().charAt(0);
				lineNumber++;
				columnNumber = 0;
			}
			break;
		default : //次に文字がある
			columnNumber++;
			nextCharacter = this.getLine().charAt(columnNumber);
			break;
		}
		return currentCharacter;
	}

	/**
	 * 現在の読み取り位置を示す
	 * @return 出力String
	 */
	String scanAt() {
		return "現在" + lineNumber + "行" + columnNumber + "列";
	}

	/**
	 * テスト用
	 * @param args
	 */
	public static void main(String args[]) {
		FileScanner scanner = new FileScanner("bsort.k");

		scanner.readNextLine();
		while(scanner.getLine() != null) {
			//scanner.readNextLine();
			scanner.nextChar();
			System.out.print(scanner.lookAhead());
			//System.out.println(scanner.scanAt());
		}
		scanner.closeFile();
	}
}