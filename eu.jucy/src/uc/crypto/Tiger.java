package uc.crypto;



//import java.io.IOException;
//import java.security.MessageDigest;
//import java.security.NoSuchAlgorithmException;
//import java.nio.channels.FileChannel;
//import java.nio.ByteBuffer;
//import java.util.LinkedList;

import fr.cryptohash.Digest;





/**
 * 
 * Was first an implementation of TTH ...
 * now its just left overs some static convenience 
 * methods to calculate the tiger digest of given data.
 * 
 * @author Quicksilver
 *
 */
public class Tiger {

//
//	public Tiger(){
//        try {
//            // Obtain a message digest object of provider "provider" using
//            // algorithm "hashfunction".
//            md = MessageDigest.getInstance("Tiger", new CryptixCrypto());
//        }
//        catch (NoSuchAlgorithmException nsae) {
//            nsae.printStackTrace();
//        }
//        small[0]=(byte) 0;
//		
//	}
//	
//	
//	private static final int digestlength = 24;
//	
//   //  Variable containing the MD object. 
//    private MessageDigest md = null;
//    /* A Base 32 Encoder  */
////    private BASE32Encoder enc=null;
//    
//    private static final int chunksInBig=5*1024  * Runtime.getRuntime().availableProcessors();
//    /**ABuffer for Reading in large chunks of data**/
//    private ByteBuffer big=ByteBuffer.allocateDirect(1024*chunksInBig); // bytebuffer for readin of the file
//    private byte[] small=new byte[1025];             // a 1 + 1024 Byte buffer for giving to digest method
//    
//    
//    
//    private int totalTreeDepth;
//    private int storedepth;
//    
//    //all the values we still need that won't be kept
//    private Hashval[] unfinished;
//    
//    // all the values we need that will be kept..the lowest line in the tree usually the depth7 2^6 values ->64
//    private byte[][] finished;
//    private int position;
//    
//  
//    //Debug
//    int nrOfBlocks; 
//    
//    /**
//     * 
//     * @param fc
//     * @param start
//     * @param end
//     * @param root
//     * @return
//     * @deprecated use the IHashEngine instead
//     */
//    public boolean checkBlock(FileChannel fc, long start , long end , byte[] root){
//    	try{
//    		long length = end-start;
//    		nrOfBlocks= Math.max(1,(int)(length/1024) + ((length%1024)!=0  ? 1:  0 )); // number of  blocks
//        //  System.out.println("nrOfBlocks: "+nrOfBlocks );
//          
//    		totalTreeDepth = depth(nrOfBlocks);
//        //  System.out.println("Totaltreedepth: "+totalTreeDeph );
//          
//    		storedepth= 0;//Math.max(1, Math.min( 7 + (length > 500*1024*1024 ? 1 :0 ) , totalTreeDepth-7  )); //small files have 1 -7 lines  usually 7  500MB upwards makes 8
//       //  System.out.println("storedepth: "+storedepth );
//          
//    		unfinished=new Hashval[ totalTreeDepth  ];
//          
//    		finished= new byte[pow(storedepth) ][digestlength];
//          
//    		position=0;
//    		int lastremaining=0;
//    		long fileposition=start;
//    		int cur;
//    		while(-1 != ( cur = fc.read(big,fileposition))){
//    			fileposition+=cur;
//    			if(fileposition < end)
//    				big.flip();
//    			else{
//    				big.rewind();
//    				big.limit(cur+lastremaining);
//    			}
//    			
//    			while( big.remaining() > 1024  ){
//    				md.reset();
//    				big.get(small, 1 , 1024);
//    				addToTree( new Hashval(totalTreeDepth-1 ,md.digest( small )));
//    			}
//    			if(big.hasRemaining()){
//    				lastremaining=big.remaining();
//    				big.compact();
//    			}else{
//    				lastremaining=0;
//    				big.rewind();
//    			}
//    			if(fileposition >= end)
//    				break;
//    		}
//    		
//    			big.flip();
//    			if(big.hasRemaining()){
//    				md.reset();
//    				md.update((byte)0);
//    				while(big.hasRemaining()  )
//    					md.update(big.get());
//    				addToTree( new Hashval(totalTreeDepth-1 ,md.digest()));
//    			}
//      		big.rewind();
//          
//      		byte[] computedRoot= finishTree()[0];
//      		
//
//      		for(int i=0; i < root.length; i++)
//      			if(root[i]!= computedRoot[i])
//      				return false; 
//      		return true;
//      		
//      		
//        }catch(IOException ioe){
//        	ioe.printStackTrace();
//        }
//          
//    	return false;
//    }
//    
//    /*
//     * Running the specified message digest algorithm of a provider on a file.
//     *
//     * @param hashfunction String Name of the hashfunction<P>
//     *                            which should be used.
//     * @param provider String Name of the provider that should be used.
//     * @param filename String Name of the file which has to be hashed.
//     * @return true or false.
//     * @deprecated use IHashEngine and interleaves instead
//     *
//    public MerkleTree run(File file){ 
//
//    	
//    	FileInputStream fis = null;
//    	FileChannel file_input_chan = null;
//        
//    	try {
//        	
//            // Get FileInputStream for reading a few bytes from a file.
//    		fis = new FileInputStream(file);
//    		file_input_chan = fis.getChannel();
//           
//            
//    		nrOfBlocks= Math.max(1,(int)(file.length()/1024) + ((file.length()%1024)!=0  ? 1:  0 )); // number of  blocks
//          //  System.out.println("nrOfBlocks: "+nrOfBlocks );
//            
//            totalTreeDepth = depth(nrOfBlocks);
//          //  System.out.println("Totaltreedepth: "+totalTreeDeph );
//            
//            storedepth= Math.max(1, Math.min( 7 + (file.length() > 500*1024*1024 ? 1 :0 ), totalTreeDepth-7  )); //small files have 1 -7 lines  usually 7  500MB upwards makes 8
//         //  System.out.println("storedepth: "+storedepth );
//            
//            unfinished=new Hashval[ totalTreeDepth  ];
//            
//            finished= new byte[pow(storedepth) ][digestlength];
//            
//            position=0;
//           
//            while(-1 !=  file_input_chan.read(big)){
//            	big.flip(); 
//                while( big.remaining() > 1024  ){
//                	md.reset();
//                	big.get(small, 1 , 1024);
//                	addToTree( new Hashval(totalTreeDepth-1 ,md.digest( small )));
//                }
//                if(big.hasRemaining())
//                	big.compact();
//                else
//                	big.rewind();
//  
//            }
//            
//        	big.flip(); 
//        	md.reset();
//        	md.update((byte)0);
//        	while(big.hasRemaining()  )
//        		md.update(big.get());
//        	addToTree( new Hashval(totalTreeDepth-1 ,md.digest()));
//        
//            big.rewind();
//            fis.close();
//            file_input_chan.close();
//            
//            // and finishing the last level of the tree.. and printing out the hash
//            return new MerkleTree(finishTree());
//
//        }catch (IOException ioe){
//            ioe.printStackTrace();
//        }
//
//
//        
//        return null;
//
//    }
//    
//    /**
//     * checks if the info in the tree is konsistent
//     * @param mt the merkletree to check
//     * @return true if everything is ok with the tree .. false otherwise
//     * @deprecated MerkleTree is no longer in use
//     *
//    public boolean validateTree(MerkleTree mt){
//    	return true;
//    }*/
//    
//    /**
//     * Computes  the  internal hash value of to childs in the tree..
//     * @param firstchild
//     * @param secondchild
//     * @return
//     * @deprecated see IHashEngine
//     */
//    private Hashval internalHash(Hashval leftchild, Hashval rightchild){
//    	md.reset();
//    	md.update((byte)1);
//    	md.update(leftchild.val);
//    	return new Hashval( --leftchild.treedepth, md.digest(rightchild.val) );
//    }
//    
//    /**
//     * 
//     * @param a - a hashval
//     * @deprecated use IHashEngine ..
//     */
//    private void addToTree(Hashval a ){
//    	try{
//    	if(a.treedepth < storedepth )
//				finished[position++]=a.val;
//    	else
//    	if(unfinished[a.treedepth]==null )
//    		unfinished[a.treedepth]=a;
//    	else{
//    		int x=a.treedepth;
//    		addToTree( internalHash( unfinished[x], a )   );
//    		unfinished[x]= null;
//    	}
//    	}catch(Exception e){
//    		System.out.println("Totaldepth: "+totalTreeDepth+" , storedepth: "+storedepth+" , nrOfBlocks: "+ nrOfBlocks );
//    		
//    		e.printStackTrace();
//    	}
//    }
//    
//    
//    /**
//     * called after the all leafs are hashed  performs last operations on the tree..
//     * @deprecated use IHashEngine
//     */
//    private byte[][] finishTree(){
//    	for(int i=totalTreeDepth-1; i >= storedepth; i--){  // trimming unbalanced tree..
//    		if(unfinished[i]!= null){
//    			unfinished[i].treedepth--;
//    			addToTree(unfinished[i]   );
//    			unfinished[i]=null;
//    		}
//    	}
//    	position=0;
//
//    	//return an ordered array
//    	
//    	//new StringBuilder(enc.encode(current[0].val)).append(erg).toString() ;
//    	return treeFromInterleaves(finished);
//    }
//    
//    /**
//     * 
//     * @param interleaves creates a tree from interleaves..
//     * @return the tree in bytearray form
//     * @deprecated Merkletree is no longer in use
//     */
//    byte[][] treeFromInterleaves(byte[][] interleaves ){
//    	LinkedList<byte[]> retval=new LinkedList<byte[]>();
//        //	StringBuilder erg=new StringBuilder("");
//    	
//    	int counter=1;
//    	while(counter < interleaves.length)
//    		counter*=2;
//    	byte[][] current;
//    	if(counter == interleaves.length)
//    		current= interleaves;
//    	else{
//    		current= new byte[counter][digestlength];
//    		System.arraycopy(interleaves, 0 , current, 0 , interleaves.length);
//    	}
//        while(current.length > 1){  //Computing from base finished level to higher levels..
//        	byte[][] next=new byte[current.length/2 ][digestlength];
//        	for(int i=0; i < next.length; i++ ){
//        		if(current[2*i]==null)
//        			break;
//        		if(current[2*i+1]==null){
//        		//	byte[] a= new byte[digestlength];
//        			System.arraycopy(current[2*i],0,next[i],0,digestlength);
//        		//	next[i]=new Hashval( current[2*i].treedepth-1 , a).val;
//        			break;
//        		}
//        		next[i]=internalHash( new Hashval(1,current[2*i]) , new Hashval(1, current[2*i+1]) ).val;
//        	}
//        	for(int i=current.length-1;i >= 0; i-- )   //printing the output
//        		if(current[i]!= null)
//        			retval.addFirst(current[i]);
//        			//	erg= new StringBuilder(" "+ enc.encode(current[i].val) ).append(erg);
//
//        	current=next;
//        }
//        retval.addFirst(current[0]);
//        
//        return retval.toArray(new byte[0][digestlength]);
//    }
//    
//
//    
//    
//
//    
//    /**
//     * 
//     * 
//     * @depreacted use HashValue instead
//     */
//    private static class Hashval {
//    	final byte[] val;
//    	int treedepth;
//    	public Hashval(int treedepth, byte[] val){
//    		this.treedepth=treedepth;
//    		this.val=val;
//    	}
//    }
//    /**
//     * 
//     * 
//     * @deprecated .. no longer in use
//     */
//    private int depth(int blocks){
//    	if(blocks==1)
//    		return 1;
//    	return 1+ depth(blocks/2);
//    }
//    /**
//     * 
//     * 
//     * @deprecated .. no longer in use
//     */
//    private int pow(int n){
//    	if(n==0)
//    		return 1;
//    	else
//    		return 2*pow(n-1);
//    }
//    
    
    static {
    	staticmessageDigest = new fr.cryptohash.Tiger();
    }
    
    private static Digest staticmessageDigest;
   
    
    public static HashValue tigerOfString(String toHash) {
    	synchronized(staticmessageDigest){
    		staticmessageDigest.reset();  //reset the digest
    		return  HashValue.createHash(staticmessageDigest.digest( toHash.getBytes() ));  //then put in the bytes hash and reencode
    	}
    }
    
    public static HashValue tigerOfHash(HashValue toHash) {
    	synchronized(staticmessageDigest){
    		staticmessageDigest.reset();  //reset the digest
    		return  HashValue.createHash(staticmessageDigest.digest( toHash.getRaw() ));  //then put in the bytes hash and reencode
    	}
    }
    
    public static HashValue tigerOfBytes(byte... bytes) {
    	synchronized(staticmessageDigest){
    		staticmessageDigest.reset();  //reset the digest
    		return  HashValue.createHash(staticmessageDigest.digest( bytes ));  //then put in the bytes hash and reencode
    	}
    }
    
    

}
