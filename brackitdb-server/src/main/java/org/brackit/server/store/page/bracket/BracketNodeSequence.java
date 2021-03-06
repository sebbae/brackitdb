/*
 * [New BSD License]
 * Copyright (c) 2011-2012, Brackit Project Team <info@brackit.org>  
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Brackit Project Team nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.brackit.server.store.page.bracket;

import java.lang.ref.PhantomReference;
import java.nio.ByteBuffer;

import org.brackit.server.node.XTCdeweyID;
import org.brackit.server.node.el.ElRecordAccess;
import org.brackit.server.node.el.index.ElPlaceHolderHelper;
import org.brackit.server.store.Field;
import org.brackit.server.store.page.bracket.BracketKey.Type;

/**
 * Class representing a sequence of bracket nodes. It consists of a LowID (which
 * is stored uncompressed) and BracketKeys with corresponding data records.
 * 
 * @author Martin Hiller
 * 
 */
public final class BracketNodeSequence {

	public static final int LOW_KEY_OFFSET = -1;

	private static final ElPlaceHolderHelper placeHolderHelper = new ElRecordAccess();

	private int collectionID;
	private XTCdeweyID lowKey;
	private XTCdeweyID highKey;

	private byte[] data;

	private int numberOfDataRecords;

	/**
	 * Returns the ancestor's DeweyID.
	 * 
	 * @param key
	 *            the key to insert
	 * @param ancestorsToInsert
	 *            the number of ancestors to be inserted implicitly
	 * @return the ancestor's DeweyID
	 */
	private static XTCdeweyID getAncestorKey(XTCdeweyID key,
			int ancestorsToInsert) {
		return (ancestorsToInsert == 0) ? key : key.getAncestor(key.getLevel()
				- ancestorsToInsert - (key.isAttribute() ? 1 : 0));
	}

	/**
	 * Constructs a BracketNodeSequence from a single node.
	 * 
	 * @param deweyID
	 *            the node's DeweyID
	 * @param value
	 *            the node's value
	 * @param externalized
	 *            is the given value an external PageID?
	 * @return the BracketNodeSequence
	 */
	public static BracketNodeSequence fromNode(XTCdeweyID deweyID,
			byte[] value, int numberOfAncestors, boolean externalized) {

		byte[] physicalLowID = null;
		BracketKey.Type lowIDType = null;
		byte[] bracketKeys = null;

		XTCdeweyID lowKey = null;

		if (numberOfAncestors == 0) {
			physicalLowID = Field.COLLECTIONDEWEYID.encode(deweyID);
			lowKey = deweyID;
			if (deweyID.isDocument()) {
				lowIDType = BracketKey.Type.DOCUMENT;
			} else if (deweyID.isAttribute()) {
				lowIDType = BracketKey.Type.ATTRIBUTE;
			} else {
				lowIDType = BracketKey.Type.DATA;
			}
			bracketKeys = new byte[0];
		} else {
			XTCdeweyID ancestorKey = BracketNodeSequence.getAncestorKey(
					deweyID, numberOfAncestors);
			physicalLowID = Field.COLLECTIONDEWEYID.encode(ancestorKey);
			lowKey = ancestorKey;
			lowIDType = ancestorKey.isDocument() ? BracketKey.Type.DOCUMENT
					: BracketKey.Type.NODATA;
			bracketKeys = BracketKey.generateBracketKeys(ancestorKey, deweyID);
		}

		int lowIDLength = physicalLowID.length;
		if (lowIDLength > 255) {
			throw new RuntimeException("StartDeweyID is too long!");
		}

		byte[] valueLength = BracketPage.getValueLengthField(value,
				externalized);

		int dataLength = 1 + lowIDLength + 1 + bracketKeys.length
				+ valueLength.length + value.length;

		byte[] data = new byte[dataLength];
		int currentOffset = 0;
		// LowID length
		data[currentOffset++] = (byte) lowIDLength;
		// LowID
		System.arraycopy(physicalLowID, 0, data, currentOffset, lowIDLength);
		currentOffset += lowIDLength;
		// LowID Type
		data[currentOffset++] = lowIDType.physicalValue;
		// Bracket keys
		System.arraycopy(bracketKeys, 0, data, currentOffset,
				bracketKeys.length);
		currentOffset += bracketKeys.length;
		// Data
		System.arraycopy(valueLength, 0, data, currentOffset,
				valueLength.length);
		currentOffset += valueLength.length;
		System.arraycopy(value, 0, data, currentOffset, value.length);

		return new BracketNodeSequence(deweyID.getDocID().getCollectionID(),
				data, 1, lowKey);
	}

	/**
	 * Creates a BracketNodeSequence from the given internal byte representation
	 * 
	 * @param internalRepresentation
	 *            internal byte representation (is generated by the bracket
	 *            page)
	 * @param numberOfDataRecords
	 *            gives the number of data records (values) included in this
	 *            sequence
	 */
	protected BracketNodeSequence(int collectionID,
			byte[] internalRepresentation, int numberOfDataRecords,
			XTCdeweyID hintLowKey) {
		this.data = internalRepresentation;
		this.numberOfDataRecords = numberOfDataRecords;
		this.collectionID = collectionID;
		this.lowKey = (hintLowKey != null ? hintLowKey : readLowKey());
	}

	/**
	 * Creates a BracketNodeSequence from the given internal byte representation
	 * 
	 * @param internalRepresentation
	 *            internal byte representation (is generated by the bracket
	 *            page)
	 */
	protected BracketNodeSequence(int collectionID,
			byte[] internalRepresentation) {
		this.data = internalRepresentation;
		this.collectionID = collectionID;
		this.lowKey = readLowKey();

		// determine number of data records
		int numberOfRecords = 0;
		int currentOffset = getStartOffset();
		int valueLength = getValueLength(LOW_KEY_OFFSET);
		if (valueLength > 0) {
			numberOfRecords++;
			currentOffset += valueLength;
		}

		BracketKey.Type currentKeyType = null;
		while (currentOffset < data.length) {

			currentKeyType = BracketKey.loadType(data, currentOffset);
			currentOffset += BracketKey.PHYSICAL_LENGTH;

			if (currentKeyType.hasDataReference) {
				numberOfRecords++;
				valueLength = BracketPage.getValueLength(currentOffset, true,
						data);
				currentOffset += valueLength;
			}
		}

		this.numberOfDataRecords = numberOfRecords;
	}

	/**
	 * Creates an empty sequence.
	 */
	public BracketNodeSequence() {
		this.data = null;
		this.collectionID = -1;
		this.lowKey = null;
		this.highKey = null;
		this.numberOfDataRecords = 0;
	}

	public boolean isEmpty() {
		return (data == null);
	}

	/**
	 * Reads the low key from the data array.
	 * 
	 * @return the low key
	 */
	private XTCdeweyID readLowKey() {

		int lowIDLength = data[0] & 255;
		return Field.CollectionDeweyIDField.decode(collectionID, data, 1,
				lowIDLength);
	}

	/**
	 * Returns the low key of this sequence.
	 * 
	 * @return the low key
	 */
	public XTCdeweyID getLowKey() {
		return lowKey;
	}

	/**
	 * Returns the highest DeweyID of this sequence.
	 * 
	 * @return the high key
	 */
	public XTCdeweyID getHighKey() {

		if (this.isEmpty()) {
			return null;
		}

		if (highKey != null) {
			return highKey;
		}

		DeweyIDBuffer buffer = new DeweyIDBuffer();
		setToLastNode(buffer);
		highKey = buffer.getDeweyID();

		return highKey;
	}

	/**
	 * @return bracket key type of the low key
	 */
	protected BracketKey.Type getLowKeyType() {
		if (this.isEmpty()) {
			return null;
		}
		int lowIDTypeField = 1 + (data[0] & 255);
		return BracketKey.Type.getByPhysicalValue(data[lowIDTypeField] & 255);
	}

	/**
	 * @return the start offset
	 */
	protected int getStartOffset() {
		if (this.isEmpty()) {
			return 0;
		}
		return 2 + (data[0] & 255);
	}

	/**
	 * @return number of data records in this sequence
	 */
	public int getNumberOfDataRecords() {
		return numberOfDataRecords;
	}

	/**
	 * @return the internal byte representation of this sequence
	 */
	protected byte[] getData() {
		return data;
	}

	/**
	 * Sets the given DeweyIDBuffer to the last node stored in this bracket node
	 * sequence.
	 * 
	 * @param deweyIDbuffer
	 *            the DeweyIDBuffer to set
	 * @return the offset of the last node
	 */
	protected int setToLastNode(DeweyIDBuffer deweyIDbuffer) {

		deweyIDbuffer.setTo(lowKey);
		int currentOffset = getStartOffset() + getValueLength(LOW_KEY_OFFSET);

		BracketKey currentKey = new BracketKey();
		int lastNodeOffset = LOW_KEY_OFFSET;
		while (currentOffset < data.length) {
			lastNodeOffset = currentOffset;

			currentKey.load(data, currentOffset);
			deweyIDbuffer.updateReduced(currentKey);

			currentOffset += BracketKey.PHYSICAL_LENGTH;

			if (currentKey.type.hasDataReference) {
				int valueLength = BracketPage.getValueLength(currentOffset,
						true, data);
				currentOffset += valueLength;
			}
		}
		if (highKey == null) {
			highKey = deweyIDbuffer.getDeweyID();
		}
		return lastNodeOffset;
	}

	/**
	 * Returns the value length a given bracket node.
	 * 
	 * @param currentOffset
	 *            the offset where the bracket key starts
	 * @return the value length of the current node
	 */
	protected int getValueLength(int currentOffset) {

		if (this.isEmpty()) {
			return 0;
		}

		int valueOffset = 0;

		if (currentOffset == LOW_KEY_OFFSET) {
			if (getLowKeyType().hasDataReference) {
				valueOffset = getStartOffset();
			} else {
				return 0;
			}
		} else {
			if (BracketKey.loadType(data, currentOffset).hasDataReference) {
				valueOffset = currentOffset + BracketKey.PHYSICAL_LENGTH;
			} else {
				return 0;
			}
		}

		return BracketPage.getValueLength(valueOffset, true, data);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		if (this.isEmpty()) {
			return "Empty Sequence";
		}

		final String outLine = "%s (%s B)\n";
		final String outLineNoData = "%s (no data)\n";
		StringBuilder out = new StringBuilder();
		int currentOffset = 0;

		// reconstruct LowID

		int lowIDLength = data[currentOffset] & 255;
		currentOffset += lowIDLength + 1;
		BracketKey.Type lowIDType = BracketKey.Type
				.getByPhysicalValue(data[currentOffset]);
		currentOffset++;

		// print nodes

		DeweyIDBuffer currentDeweyID = new DeweyIDBuffer(lowKey);
		BracketKey currentKey = new BracketKey();
		BracketKey.Type currentKeyType = lowIDType;
		boolean firstRun = true;

		while (currentOffset < data.length || firstRun) {
			if (!firstRun) {
				currentKey.load(data, currentOffset);
				currentKeyType = currentKey.type;
				currentDeweyID.updateReduced(currentKey);
				currentOffset += BracketKey.PHYSICAL_LENGTH;
			} else {
				firstRun = false;
			}

			if (currentKeyType != BracketKey.Type.OVERFLOW) {
				if (!currentKeyType.hasDataReference) {
					out.append(String.format(outLineNoData,
							currentDeweyID.getDeweyID()));
				} else {
					int valueLength = BracketPage.getValueLength(currentOffset,
							true, data);
					currentOffset += valueLength;
					out.append(String.format(outLine,
							currentDeweyID.getDeweyID(), valueLength));
				}
			}
		}

		return out.toString();
	}

	/**
	 * Reads a BracketNodeSequence from the given ByteBuffer.
	 * 
	 * @param length
	 *            the length to read from the buffer
	 * @param buffer
	 *            the byte buffer
	 * @return the constructed BracketNodeSequence
	 */
	public static BracketNodeSequence read(int collectionID, int length,
			ByteBuffer buffer) {
		byte[] internalData = new byte[length];
		buffer.get(internalData);
		return new BracketNodeSequence(collectionID, internalData);
	}

	/**
	 * @return the internal length (in Byte) of the BracketNodeSequence
	 */
	public int getLength() {
		return data == null ? 0 : data.length;
	}

	/**
	 * Writes the internal representation to the ByteBuffer.
	 * 
	 * @param buffer
	 *            the byte buffer
	 */
	public void write(ByteBuffer buffer) {
		if (this.isEmpty()) {
			return;
		}
		buffer.put(data);
	}

	/**
	 * Appends the given Sequence to this one.
	 * 
	 * @param other
	 * @param a
	 *            DeweyIDBuffer needed for processing; the original value in the
	 *            buffer will not change, but the stored Backup will be lost
	 *            after this method.
	 */
	public void append(BracketNodeSequence other, DeweyIDBuffer buffer) {

		// if the other is empty
		if (other.isEmpty()) {
			return;
		}

		// if this is empty
		if (this.isEmpty()) {
			this.data = other.data;
			this.collectionID = other.collectionID;
			this.lowKey = other.lowKey;
			this.highKey = other.highKey;
			this.numberOfDataRecords = other.numberOfDataRecords;
			return;
		}

		// backup original value
		buffer.backup();

		// determine highkey
		SimpleDeweyID highKey = null;
		if (this.highKey != null) {
			highKey = this.highKey;
		} else {
			setToLastNode(buffer);
			highKey = buffer;
		}

		// compute keys between this highkey and the other's lowkey
		byte[] keys = BracketKey.generateBracketKeys(highKey, other.lowKey);
		BracketKey.updateType(other.getLowKeyType(), keys, keys.length
				- BracketKey.PHYSICAL_LENGTH);

		// calculate new length
		int otherStartOffset = other.getStartOffset();
		int otherLength = other.data.length - otherStartOffset;
		int length = data.length + keys.length + otherLength;

		// create new internal representation
		byte[] newData = new byte[length];
		int currentOffset = 0;
		System.arraycopy(data, 0, newData, currentOffset, data.length);
		currentOffset += data.length;
		System.arraycopy(keys, 0, newData, currentOffset, keys.length);
		currentOffset += keys.length;
		System.arraycopy(other.data, otherStartOffset, newData, currentOffset,
				otherLength);

		// update fields
		this.data = newData;
		if (other.highKey != null) {
			this.highKey = other.highKey;
		} else {
			other.setToLastNode(buffer);
			this.highKey = buffer.getDeweyID();
		}
		this.numberOfDataRecords += other.numberOfDataRecords;

		buffer.restore(false);
	}

	/**
	 * Splits this BracketNodeSequence into two parts and returns the second
	 * part.
	 */
	public BracketNodeSequence split() {

		DeweyIDBuffer deweyIDbuffer = new DeweyIDBuffer();
		deweyIDbuffer.setTo(lowKey);
		int currentOffset = getStartOffset() + getValueLength(LOW_KEY_OFFSET);

		int limit = data.length / 2;

		BracketKey currentKey = new BracketKey();
		currentKey.type = getLowKeyType();
		int dataRecords1 = 0;
		int dataRecords2 = 0;
		while (true) {
			currentKey.load(data, currentOffset);
			deweyIDbuffer.updateReduced(currentKey);

			currentOffset += BracketKey.PHYSICAL_LENGTH;

			if (currentKey.type.hasDataReference) {
				dataRecords1++;
				int valueLength = BracketPage.getValueLength(currentOffset,
						true, data);
				currentOffset += valueLength;
				if (currentOffset > limit) {
					break;
				}
			}
		}

		// deweyIDBuffer contains DeweyID of the first part's last node
		highKey = deweyIDbuffer.getDeweyID();
		byte[] data1 = new byte[currentOffset];
		System.arraycopy(data, 0, data1, 0, currentOffset);
		dataRecords2 = numberOfDataRecords - dataRecords1;
		numberOfDataRecords = dataRecords1;

		// determine second part's lowkey
		while (true) {
			currentKey.load(data, currentOffset);
			deweyIDbuffer.updateReduced(currentKey);

			currentOffset += BracketKey.PHYSICAL_LENGTH;

			if (currentKey.type != BracketKey.Type.OVERFLOW) {
				break;
			}
		}
		XTCdeweyID secondLowKey = deweyIDbuffer.getDeweyID();
		byte[] secondLowKeyBytes = Field.COLLECTIONDEWEYID.encode(secondLowKey);
		if (secondLowKeyBytes.length > 255) {
			throw new RuntimeException("StartDeweyID is too long!");
		}

		// calculate data2 length
		int length2 = 2 + secondLowKeyBytes.length
				+ (data.length - currentOffset);
		// fill data2
		byte[] data2 = new byte[length2];
		int data2Offset = 0;
		data2[data2Offset++] = (byte) secondLowKeyBytes.length;
		System.arraycopy(secondLowKeyBytes, 0, data2, data2Offset,
				secondLowKeyBytes.length);
		data2Offset += secondLowKeyBytes.length;
		data2[data2Offset++] = currentKey.type.physicalValue;
		System.arraycopy(data, currentOffset, data2, data2Offset, data.length
				- currentOffset);

		this.data = data1;
		this.numberOfDataRecords = dataRecords1;
		return new BracketNodeSequence(collectionID, data2, dataRecords2, secondLowKey);
	}

	/**
	 * 
	 */
	public BracketNodeSequence split(XTCdeweyID limit,
			ExternalValueLoader extValueLoader) throws ExternalValueException {

		DeweyIDBuffer deweyIDbuffer = new DeweyIDBuffer();

		if (limit.compareReduced(lowKey) <= 0) {
			// no split necessary
			BracketNodeSequence result = new BracketNodeSequence();
			result.append(this, deweyIDbuffer);
			this.clear();
			return result;
		}

		deweyIDbuffer.setTo(lowKey);
		deweyIDbuffer.enableCompareMode(limit);
		int currentOffset = getStartOffset() + getValueLength(LOW_KEY_OFFSET);

		int dataRecords1 = 0;
		int dataRecords2 = 0;

		int previousOffset = LOW_KEY_OFFSET;
		int previousEnd = LOW_KEY_OFFSET;
		BracketKey.Type previousType = getLowKeyType();
		deweyIDbuffer.backup();

		BracketKey currentKey = new BracketKey();
		boolean splitPosFound = false;

		while (currentOffset < data.length) {
			currentKey.load(data, currentOffset);
			deweyIDbuffer.update(currentKey, false);

			if (currentKey.type != BracketKey.Type.OVERFLOW) {

				if (deweyIDbuffer.compare() >= 0) {
					// split position found
					splitPosFound = true;
					break;
				}

				// buffer information about previous node
				previousOffset = currentOffset;
				previousType = currentKey.type;
				deweyIDbuffer.backup();
			}

			currentOffset += BracketKey.PHYSICAL_LENGTH;

			if (currentKey.type.hasDataReference) {
				dataRecords1++;
				int valueLength = BracketPage.getValueLength(currentOffset,
						true, data);
				currentOffset += valueLength;
			}

			if (currentKey.type != BracketKey.Type.OVERFLOW) {
				previousEnd = currentOffset;
			}
		}

		if (!splitPosFound) {
			// reached end of BracketNodeSequence without finding the split key
			BracketNodeSequence result = new BracketNodeSequence();
			result.collectionID = this.collectionID;
			return result;
		}

		// deweyIDBuffer's backup contains DeweyID of the first part's last node
		highKey = deweyIDbuffer.getBackupDeweyID();
		deweyIDbuffer.resetBackup();

		int data1Length = previousEnd;
		byte[] placeHolder = null;
		byte[] placeHolderLength = null;
		if (previousType == Type.NODATA) {
			// placeholder needed
			placeHolder = placeHolderHelper
					.createPlaceHolderValue(getNextValue(currentOffset,
							extValueLoader));
			placeHolderLength = BracketPage.getValueLengthField(placeHolder,
					false);
			data1Length += placeHolderLength.length + placeHolder.length;
		}

		// fill buffer 1
		byte[] data1 = new byte[data1Length];
		System.arraycopy(data, 0, data1, 0, previousEnd);
		if (placeHolder != null) {
			// change type of previous node
			BracketKey.updateType(Type.DATA, data1, previousOffset);
			System.arraycopy(placeHolderLength, 0, data1, previousEnd,
					placeHolderLength.length);
			System.arraycopy(placeHolder, 0, data1, previousEnd
					+ placeHolderLength.length, placeHolder.length);
		}

		// determine number of data records
		dataRecords2 = numberOfDataRecords - dataRecords1;
		if (placeHolder != null) {
			dataRecords1++;
		}
		numberOfDataRecords = dataRecords1;

		// determine second part's lowkey
		XTCdeweyID secondLowKey = deweyIDbuffer.getDeweyID();
		byte[] secondLowKeyBytes = Field.COLLECTIONDEWEYID.encode(secondLowKey);
		if (secondLowKeyBytes.length > 255) {
			throw new RuntimeException("StartDeweyID is too long!");
		}

		currentOffset += BracketKey.PHYSICAL_LENGTH;
		// calculate data2 length
		int length2 = 2 + secondLowKeyBytes.length
				+ (data.length - currentOffset);
		// fill data2
		byte[] data2 = new byte[length2];
		int data2Offset = 0;
		data2[data2Offset++] = (byte) secondLowKeyBytes.length;
		System.arraycopy(secondLowKeyBytes, 0, data2, data2Offset,
				secondLowKeyBytes.length);
		data2Offset += secondLowKeyBytes.length;
		data2[data2Offset++] = currentKey.type.physicalValue;
		System.arraycopy(data, currentOffset, data2, data2Offset, data.length
				- currentOffset);

		this.data = data1;
		this.numberOfDataRecords = dataRecords1;
		return new BracketNodeSequence(collectionID, data2, dataRecords2, secondLowKey);
	}

	private void clear() {
		this.data = null;
		this.lowKey = null;
		this.highKey = null;
		this.numberOfDataRecords = 0;
	}

	private byte[] getNextValue(int currentOffset,
			ExternalValueLoader extValueLoader) throws ExternalValueException {

		if (this.isEmpty()) {
			return null;
		}

		int valueOffset = -1;
		if (currentOffset == LOW_KEY_OFFSET) {

			currentOffset = getStartOffset();

			if (getLowKeyType().hasDataReference) {
				valueOffset = currentOffset;
			}
		}

		if (valueOffset < 0) {
			// value not yet found
			BracketKey currentKey = new BracketKey();

			while (currentOffset < data.length) {

				currentKey.load(data, currentOffset);
				currentOffset += BracketKey.PHYSICAL_LENGTH;

				if (currentKey.type.hasDataReference) {
					valueOffset = currentOffset;
					break;
				}
			}
		}

		// value offset determined -> load value
		return BracketPage.loadValue(data, valueOffset, extValueLoader);
	}
}
