package simpledb.tx.recovery;

import simpledb.file.Block;
import simpledb.server.SimpleDB;
import simpledb.tx.Transaction;

/*
 * Bu test case  ANCAK Test4.java dan SONRA çalıştırılablilir. Mevcut "testrecovery" veritabanını SİLMEYİN. Sistem ilk başlarken recover yapacak o kadar. 
 * Sonra TX2 doğru recover yaptığını kontrol ediyor. Bu Programın FAIL!!!  yazmadan hatasiz sonlanması gerekiyor..
 * Bununla beraber log dosyasının son hali aşağıdaki gibi olmalıdır...
 * 
 */

public class Test5 {

	private static Block blk = new Block("aFile4", 0);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("BEGIN RECOVERY PACKAGE TEST: ## 5 ");
		SimpleDB.init("testrecovery");
		// tx:2 starting
		Transaction tx = new Transaction();  // sadece okuma yapan kontrol amacli bir hareket..
		tx.pin(blk);
		if (tx.getInt(blk, 0) != 100 || tx.getInt(blk, 4) == 200)
			System.out.println("*****RecoveryTest: FAIL!!!");
		tx.commit();
		tx.listLog();	 
		
	}
}

/*
 * .........
<SETINT 1 [file fldcat.tbl, block 1] 276 0>
<SETINT 1 [file fldcat.tbl, block 1] 252 0>
<SETINT 1 [file fldcat.tbl, block 1] 248 0>
<COMMIT 1>
<START 2>
<SETINT 2 [file aFile4, block 0] 0 1>
<COMMIT 2>
<CHECKPOINT>
<START 3>
<SETINT 3 [file aFile4, block 0] 4 8>     // TEST4 buraya kadar yazmıştı..
<START 1>
<CHECKPOINT>    // Sistem recover yapti!!!..
<COMMIT 1>
<START 2>
<COMMIT 2>

*/
