//Copyright 2013, Nathan Tippy
//See LICENSE file for BSD license details.
//Send support requests to http://www.ociweb.com/contact
package com.ociweb.jfast.catalog.loader;

import com.ociweb.jfast.field.LocalHeap;
import com.ociweb.jfast.primitive.PrimitiveReader;
import com.ociweb.jfast.primitive.PrimitiveWriter;

/**
 * Holds count of how many of each type of field is required and what the
 * default values are. Default(initial) values are sparse so only the index and
 * the value are kept in this class. Upon request this class will generate the
 * needed dictionary arrays fully populated.
 * 
 * 
 * @author Nathan Tippy
 * 
 */
public class DictionaryFactory { 

    /*
     * Dictionary: Max count of all fields for dictionary shared across
     * templates. With default values.
     * 
     * Catalog: tokens lookup and IDs span across templates and dictionary and
     * belong to the catalog. hold app opional return values for each field.
     * 
     * Template: templates hold ordered list of fields/tokens.
     * 
     * 
     * catalog template dictionary (shared between templates?)
     */

    char[][] appTypes = null;// appType identifiers used in XML

    private static final int INIT_GROW_STEP = 16;

    private int integerCount;
    private int longCount;
    private int bytesCount;

    private int integerInitCount;
    private int[] integerInitIndex;
    private int[] integerInitValue;

    private int longInitCount;
    private int[] longInitIndex;
    private long[] longInitValue;

    private int byteInitCount;
    private int[] byteInitIndex;
    private byte[][] byteInitValue;
    private int byteInitTotalLength;
    
    private int singleBytesSize;
    private int gapBytesSize;
    
    LocalHeap byteHeap;    

    public DictionaryFactory() {
    	
        this.singleBytesSize= 64;
        this.gapBytesSize = 8;

        this.integerInitCount = 0;
        this.integerInitIndex = new int[INIT_GROW_STEP];
        this.integerInitValue = new int[INIT_GROW_STEP];

        this.longInitCount = 0;
        this.longInitIndex = new int[INIT_GROW_STEP];
        this.longInitValue = new long[INIT_GROW_STEP];

        this.byteInitCount = 0;
        this.byteInitIndex = new int[INIT_GROW_STEP];
        this.byteInitValue = new byte[INIT_GROW_STEP][];
    }

    public void setTypeCounts(int integerCount, int longCount, int bytesCount, int bytesGap, int bytesNominalLength) {
        this.integerCount = integerCount;
        this.longCount = longCount;
        this.bytesCount = bytesCount;
        this.gapBytesSize = bytesGap;
        
        if (bytesNominalLength<1) {
            throw new UnsupportedOperationException("Length must be 1 or more.");
        }
        this.singleBytesSize = bytesNominalLength;
    }

    public DictionaryFactory(PrimitiveReader reader) {

    	this.singleBytesSize = PrimitiveReader.readIntegerUnsigned(reader);
    	this.gapBytesSize = PrimitiveReader.readIntegerUnsigned(reader);
    	
        this.integerCount = PrimitiveReader.readIntegerUnsigned(reader);
        this.longCount = PrimitiveReader.readIntegerUnsigned(reader);
        this.bytesCount = PrimitiveReader.readIntegerUnsigned(reader);

        this.integerInitCount = PrimitiveReader.readIntegerUnsigned(reader);
        this.integerInitIndex = new int[integerInitCount];
        this.integerInitValue = new int[integerInitCount];
        int c = integerInitCount;
        while (--c >= 0) {
            integerInitIndex[c] = PrimitiveReader.readIntegerUnsigned(reader);
            integerInitValue[c] = PrimitiveReader.readIntegerSigned(reader);
        }

        this.longInitCount = PrimitiveReader.readIntegerUnsigned(reader);
        this.longInitIndex = new int[longInitCount];
        this.longInitValue = new long[longInitCount];
        c = longInitCount;
        while (--c >= 0) {
            longInitIndex[c] = PrimitiveReader.readIntegerUnsigned(reader);
            longInitValue[c] = PrimitiveReader.readLongSigned(reader);
        }


        this.byteInitCount = PrimitiveReader.readIntegerUnsigned(reader);
        this.byteInitIndex = new int[byteInitCount];
        this.byteInitValue = new byte[byteInitCount][];
        c = byteInitCount;
        while (--c >= 0) {
            byteInitIndex[c] = PrimitiveReader.readIntegerUnsigned(reader);
            int len = PrimitiveReader.readIntegerUnsigned(reader);
            if (len>0) {
            	byte[] value = new byte[len];
            	PrimitiveReader.readByteData(value, 0, len, reader);
            	byteInitValue[c] = value;
            } else {
            	if (len<0) {
            		byteInitValue[c]=null;
            	} else {
            		byteInitValue[c]=new byte[0];
            	}
            }
        }
        byteInitTotalLength = PrimitiveReader.readIntegerUnsigned(reader);

    }

    public void save(PrimitiveWriter writer) {

    	PrimitiveWriter.writeIntegerUnsigned(singleBytesSize, writer);
    	PrimitiveWriter.writeIntegerUnsigned(gapBytesSize, writer);
    	
        PrimitiveWriter.writeIntegerUnsigned(integerCount, writer);
        PrimitiveWriter.writeIntegerUnsigned(longCount, writer);
        PrimitiveWriter.writeIntegerUnsigned(bytesCount, writer);

        PrimitiveWriter.writeIntegerUnsigned(integerInitCount, writer);
        int c = integerInitCount;
        while (--c >= 0) {
            PrimitiveWriter.writeIntegerUnsigned(integerInitIndex[c], writer);
            PrimitiveWriter.writeIntegerSigned(integerInitValue[c], writer);
        }

        PrimitiveWriter.writeIntegerUnsigned(longInitCount, writer);
        c = longInitCount;
        while (--c >= 0) {
            PrimitiveWriter.writeIntegerUnsigned(longInitIndex[c], writer);
            PrimitiveWriter.writeLongSigned(longInitValue[c], writer);
        }

        PrimitiveWriter.writeIntegerUnsigned(byteInitCount, writer);
        c = byteInitCount;
        while (--c >= 0) {
            PrimitiveWriter.writeIntegerUnsigned(byteInitIndex[c], writer);
            byte[] value = byteInitValue[c];
            PrimitiveWriter.writeIntegerUnsigned(null==value? -1 :value.length, writer);
            if (null!=value && value.length>0) {
            	PrimitiveWriter.writeByteArrayData(value, 0, value.length, writer);
            }
        }
        PrimitiveWriter.writeIntegerUnsigned(byteInitTotalLength, writer);

        /*
         * Fastest searialize deserialize however its more verbose and there is
         * no object dectection and construction.
         * 
         * These files can be deleted and modified but those changes are only
         * refelected on startup. New templates can be added but an explicit
         * call must be made to load them. The new templates will be loaded
         * dynamicaly on first use but this is not recommended.
         */

    }

    public void addInitInteger(int idx, int value) {

        integerInitIndex[integerInitCount] = idx;
        integerInitValue[integerInitCount] = value;
        if (++integerInitCount >= integerInitValue.length) {
            int newLength = integerInitValue.length + INIT_GROW_STEP;
            int[] temp1 = new int[newLength];
            int[] temp2 = new int[newLength];
            System.arraycopy(integerInitIndex, 0, temp1, 0, integerInitValue.length);
            System.arraycopy(integerInitValue, 0, temp2, 0, integerInitValue.length);
            integerInitIndex = temp1;
            integerInitValue = temp2;
        }

    }

    public void addInitLong(int idx, long value) {

        longInitIndex[longInitCount] = idx;
        longInitValue[longInitCount] = value;
        if (++longInitCount >= longInitValue.length) {
            int newLength = longInitValue.length + INIT_GROW_STEP;
            int[] temp1 = new int[newLength];
            long[] temp2 = new long[newLength];
            System.arraycopy(longInitIndex, 0, temp1, 0, longInitIndex.length);
            System.arraycopy(longInitValue, 0, temp2, 0, longInitIndex.length);
            longInitIndex = temp1;
            longInitValue = temp2;
        }

    }

    public void addInit(int idx, byte[] value) {

        byteInitIndex[byteInitCount] = idx;
        byteInitValue[byteInitCount] = value;
        byteInitTotalLength +=  (null==value ? 0 :value.length);
        if (++byteInitCount >= byteInitValue.length) {
            int newLength = byteInitValue.length + INIT_GROW_STEP;
            int[] temp1 = new int[newLength];
            byte[][] temp2 = new byte[newLength][];
            System.arraycopy(byteInitIndex, 0, temp1, 0, byteInitValue.length);
            System.arraycopy(byteInitValue, 0, temp2, 0, byteInitValue.length);
            byteInitIndex = temp1;
            byteInitValue = temp2;
        }

    }

    public static int nextPowerOfTwo(int value) {
        int temp = value;
        int result = 0;
        while (0 != temp) {
            temp = temp >> 1;
            result++;
        }
        // System.err.println(value+" -> "+(1<<result));

        return 1 << result;
    }

    public int[] integerDictionary() {
        int[] array = new int[nextPowerOfTwo(integerCount)];
        int i = integerInitCount;
        while (--i >= 0) {
            array[integerInitIndex[i]] = integerInitValue[i];
        }
        return array;
    }

    public long[] longDictionary() {
        long[] array = new long[nextPowerOfTwo(longCount)];
        int i = longInitCount;
        while (--i >= 0) {
            array[longInitIndex[i]] = longInitValue[i];
        }
        return array;
    }
    
    public LocalHeap byteDictionary() {
        if (bytesCount == 0) {
            return null;
        }
        if (null==byteHeap) {
        	byteHeap = new LocalHeap(singleBytesSize, gapBytesSize, nextPowerOfTwo(bytesCount), byteInitTotalLength, byteInitIndex, byteInitValue);
            LocalHeap.reset(byteHeap);
        }
        
        return byteHeap;
    }

    public void reset(int[] values) {
        int i = integerCount;
        while (--i >= 0) {
            values[i] = 0;
        }
        i = integerInitCount;
        while (--i >= 0) {
            values[integerInitIndex[i]] = integerInitValue[i];
        }
    }

    public void reset(long[] values) {
        int i = longCount;
        while (--i >= 0) {
            values[i] = 0;
        }
        i = longInitCount;
        while (--i >= 0) {
            values[longInitIndex[i]] = longInitValue[i];
        }
    }

    public void reset(LocalHeap heap) {
        if (null != heap) {
            LocalHeap.reset(heap);
        }
    }

    public static byte[] initConstantByteArray(DictionaryFactory dcr) {
        if (null!=dcr) {
            LocalHeap byteHeap = dcr.byteDictionary();
            if (null!=byteHeap) {
                          
                return LocalHeap.rawInitAccess(byteHeap);  
                //System.err.println("constByteBufferLen:"+this.constByteBuffer.length);
                
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

}
