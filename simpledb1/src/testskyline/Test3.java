package testskyline;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import simpledb.materialize.TempTable;
import simpledb.metadata.MetadataMgr;
import simpledb.multibuffer.ChunkScan;
import simpledb.query.IntConstant;
import simpledb.query.Plan;
import simpledb.query.Scan;
import simpledb.query.TablePlan;
import simpledb.query.TableScan;
import simpledb.query.UpdateScan;
import simpledb.record.RID;
import simpledb.record.Schema;
import simpledb.record.TableInfo;
import simpledb.server.SimpleDB;
import simpledb.tx.Transaction;

/*
 * -----> DOUBLE <-------  SKYLINE " >1 ITERATION " 
 * 
 * */

public class Test3 {
	
	static Transaction tx;
	static TableInfo ti;
	static ArrayList<RID> notSkylineList;
	static Schema sch;
	static int numberOfIteration=0;
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		SimpleDB.BUFFER_SIZE=4;
		int WINDOWSIZE=1; // en fazla "buffer size-3" olabilir.
		
		InitData.initData("skyline100"); 
		
		tx = new Transaction();
		MetadataMgr md = SimpleDB.mdMgr();
		ti = md.getTableInfo("input", tx);
		sch = ti.schema();

	
		/*
		 * input tablosunundaki kay�tlar� s�rayla okuyoruz. Burda 1 tampon(page)
		 * kullan�yor.
		 */

		Plan p = new TablePlan("input", tx);
		Scan input = p.open();
//		input.next();
		/*
		 * Ana hafizadaki WINDOW b�lgesinin set edilmesi: 5 tampon yer kapliyor.
		 * Icerisinde formatlanm�� bo� slotlar var. Artik bu bolgeyi
		 * kullanabiliriz.
		 */
		ChunkScan window = new ChunkScan(WINDOWSIZE, sch, tx);
		/*
		 * TEMP dosyam�z: Window'a s��mayanlar� buraya yazacagiz..Geni�lerken
		 * ayn� anda 2 tampon yer kapl�yor..
		 */
		TempTable outputfile = new TempTable(ti.schema(), tx);
		TableScan output = (TableScan) outputfile.open();

		doAnIteration(input, window,output);		
		
		input.close();
		window.close();
		output.close();

	}
	
	static void doAnIteration(Scan input,ChunkScan window, TableScan output){
		numberOfIteration++;
		input.beforeFirst();
		
		boolean tempfileExist=false;
		notSkylineList=new ArrayList<RID>();
		while (input.next()) {
			double A = input.getDouble("aind");
			double B = input.getDouble("bind");
			window.beforeFirst();
			boolean willBeInwindow = true;
			int myTest=-1;
			while (window.next()) {
				double wA = window.getDouble("aind");
				double wB = window.getDouble("bind");
				if ((A <= wA && B < wB) || (A < wA && B <= wB)) { // better at
																	// least one
																	// dim. ==>
																	// dominate
																	// in
																	// the
																	// window
					window.delete(); // do not break. May delete other records in window. (If we are here, we are impossible to enter the following else if)
					myTest=0;
					// willBeInwindow=true;
				} else if ((wA <= A && wB < B) || (wA < A && wB <= B)) { // better
																			// at
																			// least
																			// one
																			// dim.
																			// ==>
																			// dominate
																			// the
																			// input.
					willBeInwindow = false;
					if(myTest ==0) System.err.print("impossible error");
					break;
				}
			}
			if (willBeInwindow) {
				if (window.insertFromScan(input)) {
					RID wRID=window.getRid();
					if(tempfileExist && !notSkylineList.contains(wRID))
						notSkylineList.add(wRID);
				}
				else{
					tempfileExist=true;
					transferBwScans(input,output);
				}
			}
		}	
		System.out.println("ITERATION "+numberOfIteration);
		System.out.println("-----------------------------");
		System.out.println("WINDOW AREA: ");
		window.printChunk();
		removeSkylinePointsFromWindow(window);
		System.out.println("TEMP AREA: ");
		output.beforeFirst();
		while(output.next()){
			double oA=output.getDouble("aind");
			double oB=output.getDouble("bind");
			System.out.println(oA + ", " + oB);
		}
		if(!tempfileExist) return;
		input.close(); // eski input'u kapatmazsak buffer yetmez..
		input=output;
		TempTable outputfile = new TempTable(ti.schema(), tx);
		output = (TableScan) outputfile.open();
		
		doAnIteration(input,window,output);
	}
	static boolean transferBwScans(Scan s1, UpdateScan s2) {
		s2.insert();
		for (String fldname : sch.fields())
			s2.setVal(fldname, s1.getVal(fldname));
		return true;
	}
	static void removeSkylinePointsFromWindow(ChunkScan window){
		window.beforeFirst();

		while (window.next()) {
			if(!notSkylineList.contains(window.getRid())){
				int id=window.getInt("id");
				double wA=window.getDouble("aind");
				double wB=window.getDouble("bind");
				System.out.println("SKYLINE POINT: "+ id + " "+ wA + ", " + wB);
				window.delete();
			}
				
		}
	}
}
