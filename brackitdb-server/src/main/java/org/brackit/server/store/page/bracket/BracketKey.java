/*
 * [New BSD License]
 * Copyright (c) 2011, Brackit Project Team <info@brackit.org>  
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.brackit.server.store.page.bracket;

/**
 * 
 * Represents the logical view on the bracket-based key format.
 * 
 * <p>
 * The physical storage format looks as follows:<br />
 * <b>Byte 0:</b> #roundBrackets<br />
 * <b>Byte 1:</b><br />
 * <div style="margin-left:2em"> <b>Bit 0-5:</b> #angleBrackets<br />
 * <b>Bit 6-7:</b> key type<br />
 * </div> <b>Byte 2:</b> #idGaps
 * </p>
 * 
 * @author Martin Hiller
 * 
 */
public class BracketKey {

	/**
	 * Constant for the physical length (in bytes) of one bracket key.
	 */
	public static final int PHYSICAL_LENGTH = 3;
	/**
	 * Constant for the length of a data reference which potentially follows a
	 * bracket key (depending on type).
	 */
	public static final int DATA_REF_LENGTH = 2;

	/**
	 * Enumerates the different bracket key types.
	 */
	public enum Type {
		DATA((byte) 0), ATTRIBUTE((byte) 1), NODATA((byte) 2), OVERFLOW(
				(byte) 3);

		private static final Type[] reverseMap;
		static {
			Type[] values = Type.values();
			reverseMap = new Type[values.length];
			for (Type keyType : values) {
				reverseMap[keyType.physicalValue] = keyType;
			}
		}
		private byte physicalValue;

		private Type(byte b) {
			this.physicalValue = b;
		}

		/**
		 * Returns the value (0-3) that is used to store the type physically.
		 * 
		 * @return the physical value
		 */
		public int getPhysicalValue() {
			return physicalValue;
		}

		/**
		 * Returns the length of the following data reference, depending on this
		 * type.
		 * 
		 * @return data reference length
		 */
		public int getDataReferenceLength() {
			return (physicalValue / 2 == 0) ? BracketKey.DATA_REF_LENGTH : 0;
		}

		/**
		 * @return true if a data reference has to follow this bracket key
		 */
		public boolean hasDataReference() {
			return (physicalValue / 2 == 0);
		}

		/**
		 * Returns the key type that is related to the given physical value.
		 * 
		 * @param physicalValue
		 *            the physical value (0-3)
		 * @return corresponding key type
		 */
		public static Type getByPhysicalValue(int physicalValue) {
			return reverseMap[physicalValue];
		}

		/**
		 * Checks whether this bracket key opens a new subtree (type is DATA or
		 * NODATA).
		 * 
		 * @return true if a new logical subtree is created
		 */
		public boolean opensNewSubtree() {
			return (physicalValue % 2 == 0);
		}
	}

	/**
	 * number of closing round brackets
	 */
	protected int roundBrackets;
	private static final int ROUND_BRACKETS_MAX = 255;
	/**
	 * number of closing angle brackets (for overflow areas)
	 */
	protected int angleBrackets;
	private static final int ANGLE_BRACKETS_MAX = 63;
	/**
	 * number of DeweyID gaps
	 */
	protected int idGaps;
	private static final int ID_GAPS_MAX = 255;
	/**
	 * type of this bracket key
	 */
	protected Type type;

	/**
	 * Returns the binary string representation of the given byte.
	 * 
	 * @param b
	 *            byte
	 * @return the binary string representation
	 */
	private static String getBinaryRepresentation(byte b) {
		char[] result = new char[8];
		for (int i = 0; i < result.length; i++) {
			int mask = 1 << (7 - i);
			result[i] = ((b & mask) == 0) ? '0' : '1';
		}
		return new String(result);
	}

	/**
	 * Loads the type of the bracket key starting at the given offset.
	 * 
	 * @param storage
	 *            the byte array to load the type from
	 * @param keyOffset
	 *            the offset where the bracket key starts
	 * @return type of the specified bracket key
	 */
	public static Type loadType(byte[] storage, int keyOffset) {
		return Type.reverseMap[storage[keyOffset + 1] & 3];
	}

	/**
	 * Checks whether a data reference has to follow the given bracket key.
	 * 
	 * @param storage
	 *            the byte array containing the bracket key
	 * @param keyOffset
	 *            the offset where the bracket key starts
	 * @return
	 */
	public static boolean hasDataReference(byte[] storage, int keyOffset) {
		return (storage[keyOffset + 1] & 3) / 2 == 0;
	}

	/**
	 * Generate the bracket key that represents the difference between the
	 * previous and the current DeweyID.
	 * 
	 * @param previousID
	 *            the previous DeweyID
	 * @param currentID
	 *            the current DeweyID
	 * @return the bracket key "between" the previous and current DeweyID
	 * @throws IllegalArgumentException
	 *             if more than one bracket key is needed to express the gap
	 *             between the two DeweyIDs (in this case, use
	 *             {@link #generateBracketKeys(SimpleDeweyID, SimpleDeweyID)}
	 */
	public static BracketKey generateBracketKey(SimpleDeweyID previousID,
			SimpleDeweyID currentID) {

		byte[] bracketKeyBytes = generateBracketKeys(previousID, currentID);

		if (bracketKeyBytes.length > PHYSICAL_LENGTH) {
			throw new IllegalArgumentException(
					"Method yields more than one BracketKey!");
		}

		BracketKey result = new BracketKey();
		result.load(bracketKeyBytes, 0);
		return result;

	}

	/**
	 * Generates and stores a list of bracket keys for the current DeweyID in
	 * relation to the previous DeweyID.
	 * 
	 * @param previousID
	 *            first DeweyID
	 * @param currentID
	 *            second DeweyID
	 * @return encoded bracket keys for the current DeweyID in relation to the
	 *         previous DeweyID
	 */
	public static byte[] generateBracketKeys(SimpleDeweyID previousID,
			SimpleDeweyID currentID) {

		int[] divisions1 = previousID.getDivisionValues();
		int length1 = previousID.getNumberOfDivisions();
		int[] divisions2 = currentID.getDivisionValues();
		int length2 = currentID.getNumberOfDivisions();

		int commonPrefix = getCommonPrefixLength(divisions1, length1,
				divisions2, length2);

		boolean previousIsAttribute = previousID.isAttribute();
		boolean currentIsAttribute = currentID.isAttribute();
		boolean attributeForSameNode = (previousIsAttribute
				&& currentIsAttribute && commonPrefix == length1 - 1);
		byte[] result = new byte[(length2 - commonPrefix - ((currentIsAttribute && (!previousIsAttribute || !attributeForSameNode)) ? 1
				: 0))
				* PHYSICAL_LENGTH];
		int resultOffset = 0;

		int closingRoundBrackets = 0;
		int closingAngleBrackets = 0;

		// attribute nodes don't need to be closed by brackets
		int upperBound = previousIsAttribute ? length1 - 2 : length1;

		// for each differing division of previousID:
		// check whether division is odd or even
		// count closing round and angle brackets
		for (int i = commonPrefix; i < upperBound; i++) {
			if (divisions1[i] % 2 == 0) {
				closingAngleBrackets++;
			} else {
				closingRoundBrackets++;
			}
		}

		// calculate number of DeweyID gaps for the first bracket key
		int firstIdGaps = 0;
		if (commonPrefix == length1) {
			// previousID is an ancestor of currentID
			firstIdGaps = calcIdGaps(1, divisions2[commonPrefix]);
		} else {
			firstIdGaps = calcIdGaps(divisions1[commonPrefix],
					divisions2[commonPrefix]);
		}

		// for each differing division of currentID:
		// create a new bracket key
		boolean firstRun = true;
		BracketKey currentKey = new BracketKey();
		for (int i = commonPrefix; i < length2; i++) {

			// skip the attribute division
			if (divisions2[i] != 1) {

				// determine correct node type
				Type type = null;
				if (divisions2[i] % 2 == 0) {
					// overflow node
					type = Type.OVERFLOW;
				} else if (i == length2 - 1) {
					// last cycle
					type = currentIsAttribute ? Type.ATTRIBUTE : Type.DATA;
				} else {
					// inner node
					type = Type.NODATA;
				}

				// create actual key
				if (firstRun) {
					currentKey.set(closingRoundBrackets, closingAngleBrackets,
							firstIdGaps, type);
				} else {
					currentKey.set(0, 0, calcIdGaps(1, divisions2[i]), type);
				}

				resultOffset = currentKey.store(result, resultOffset);
			}

			firstRun = false;
		}
		return result;
	}

	/**
	 * Returns the length (in number of divisions) of the common prefix.
	 * 
	 * @param divisions1
	 *            division values of first DeweyID
	 * @param length1
	 *            actual number of first DeweyID's divisions
	 * @param divisions2
	 *            division values of second DeweyID
	 * @param length2
	 *            actual number of second DeweyID's divisions
	 * @return length of common prefix
	 */
	private static int getCommonPrefixLength(int[] divisions1, int length1,
			int[] divisions2, int length2) {
		int upperBound = Math.min(length1, length2);

		int commonPrefix = 0;
		while (commonPrefix < upperBound) {
			if (divisions1[commonPrefix] != divisions2[commonPrefix]) {
				break;
			} else {
				commonPrefix++;
			}
		}

		return commonPrefix;
	}

	/**
	 * Calculates how many id gaps are needed to jump from divisionValue1 to
	 * divisionValue2.
	 * 
	 * @param divisionValue1
	 * @param divisionValue2
	 * @return number of needed id gaps
	 */
	private static int calcIdGaps(int divisionValue1, int divisionValue2) {
		// set nextDivValue to the next odd number greater than divisionValue1
		int nextDivValue = divisionValue1 + (divisionValue1 % 2) + 1;
		return (divisionValue2 + 1 - nextDivValue) / 2;
	}

	/**
	 * Creates a bracket key with the given parameter values.
	 * 
	 * @param roundBrackets
	 *            number of closing round brackets
	 * @param angleBrackets
	 *            number of closing angle brackets (for overflow areas)
	 * @param idGaps
	 *            number of DeweyID gaps
	 * @param Type
	 *            the type of this bracket key
	 * @throws BracketKeyOverflowException
	 *             if the resulting bracket key does not fit into the physical
	 *             storage format.
	 */
	public BracketKey(int roundBrackets, int angleBrackets, int idGaps,
			Type type) throws BracketKeyOverflowException {
		set(roundBrackets, angleBrackets, idGaps, type);
	}

	/**
	 * Creates an empty bracket key.
	 */
	public BracketKey() {
	}

	private void set(int roundBrackets, int angleBrackets, int idGaps, Type type) {

		// check input
		if (roundBrackets > ROUND_BRACKETS_MAX
				|| angleBrackets > ANGLE_BRACKETS_MAX || idGaps > ID_GAPS_MAX) {
			// overflow occurred
			throw new BracketKeyOverflowException();
		}

		this.roundBrackets = roundBrackets;
		this.angleBrackets = angleBrackets;
		this.idGaps = idGaps;
		this.type = type;
	}

	/**
	 * Stores this bracket key in a byte array at the specified position.
	 * 
	 * @param storage
	 *            the storage byte array
	 * @param position
	 *            the position in the array at which the first byte of the key
	 *            is stored
	 * @return the position of the last used byte + 1
	 */
	public int store(byte[] storage, int position) {
		return store(storage, position, false);
	}

	/**
	 * Stores this bracket key in a byte array at the specified position. If
	 * 'ignoreKeyType' is true, the two bits for the key type will not be
	 * changed in the storage array.
	 * 
	 * @param storage
	 *            the storage byte array
	 * @param position
	 *            the position in the array at which the first byte of the key
	 *            is stored
	 * @param ignoreKeyType
	 *            see description above
	 * @return the position of the last used byte + 1
	 */
	public int store(byte[] storage, int position, boolean ignoreKeyType) {

		// byte 0
		storage[position] = (byte) roundBrackets;
		position++;

		// byte 1
		if (ignoreKeyType) {
			storage[position] = (byte) ((angleBrackets << 2) | (storage[position] & 3));
		} else {
			storage[position] = (byte) ((angleBrackets << 2) | type
					.getPhysicalValue());
		}
		position++;

		// byte 2
		storage[position] = (byte) idGaps;
		position++;

		return position;
	}

	/**
	 * Loads a bracket key from the given byte array beginning at the specified
	 * position.
	 * 
	 * @param storage
	 *            the byte array to load the bracket key from
	 * @param position
	 *            starting point in the byte array
	 */
	public void load(byte[] storage, int position) {

		// byte 0
		roundBrackets = storage[position] & 0xFF;
		position++;

		// byte 1
		int currentByte = storage[position] & 0xFF;
		angleBrackets = currentByte / 4;
		type = Type.reverseMap[currentByte % 4];
		position++;

		// byte 2
		idGaps = storage[position] & 0xFF;

	}

	/**
	 * Loads a bracket key from the given byte array beginning at the specified
	 * position, but leaves the idGap-field untouched.
	 * 
	 * @param storage
	 *            the byte array to load the bracket key from
	 * @param position
	 *            starting point in the byte array
	 */
	public void loadWithoutGaps(byte[] storage, int position) {

		// byte 0
		roundBrackets = storage[position] & 0xFF;
		position++;

		// byte 1
		int currentByte = storage[position] & 0xFF;
		angleBrackets = currentByte / 4;
		type = Type.reverseMap[currentByte % 4];
	}

	/**
	 * Loads only the number of DeweyID-Gaps from the bracket key in the
	 * storage-array starting at the given position.
	 * 
	 * @param storage
	 *            the byte array to load the DeweyID-Gaps from
	 * @param position
	 *            starting point in the byte array
	 */
	public void loadGaps(byte[] storage, int position) {
		// byte 2
		idGaps = storage[position + 2] & 0xFF;
	}

	/**
	 * Creates and loads a bracket key from the given byte array beginning at
	 * the specified position.
	 * 
	 * @param storage
	 *            the byte array to load the bracket key from
	 * @param position
	 *            starting point in the byte array
	 * @return the loaded bracket key
	 */
	public static BracketKey loadNew(byte[] storage, int position) {
		BracketKey result = new BracketKey();
		result.load(storage, position);
		return result;
	}

	/**
	 * Returns the physical format of this bracket key as byte array.
	 * 
	 * @return the physical format
	 */
	public byte[] getPhysicalFormat() {

		byte[] result = new byte[PHYSICAL_LENGTH];
		store(result, 0);
		return result;

	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {

		StringBuilder result = new StringBuilder();

		result.append("Logical Format:\n");
		result.append("\troundBrackets = " + roundBrackets + "\n");
		result.append("\tangleBrackets = " + angleBrackets + "\n");
		result.append("\tidGaps = " + idGaps + "\n");
		result.append("\ttype = " + type + "\n");
		result.append("Physical Format:\n\t");

		byte[] physicalFormat = getPhysicalFormat();
		for (int i = 0; i < physicalFormat.length; i++) {
			if (i > 0) {
				result.append(" ");
			}
			result.append(getBinaryRepresentation(physicalFormat[i]));
		}

		return result.toString();
	}

	/**
	 * Returns the bracket string representation of this bracket key.
	 * 
	 * @return bracket string representation
	 */
	public String getBracketString() {

		StringBuilder result = new StringBuilder();

		if (type == Type.ATTRIBUTE) {
			result.append("Attribute: ");
		}

		for (int i = 0; i < roundBrackets; i++) {
			result.append(")");
		}
		for (int i = 0; i < angleBrackets; i++) {
			result.append(">");
		}
		for (int i = 0; i < idGaps; i++) {
			result.append("()");
		}

		if (type != Type.ATTRIBUTE) {
			result.append(type == Type.OVERFLOW ? "<" : "(");
		}

		return result.toString();
	}

	/**
	 * Returns the type of this bracket key.
	 * 
	 * @return the type
	 */
	public Type getType() {
		return type;
	}

	/**
	 * Returns the number of closing round brackets of this bracket key.
	 * 
	 * @return number of round brackets
	 */
	public int getRoundBrackets() {
		return roundBrackets;
	}

	/**
	 * Returns the number of closing angle brackets of this bracket key.
	 * 
	 * @return number of angle brackets
	 */
	public int getAngleBrackets() {
		return angleBrackets;
	}

	/**
	 * Returns the number of DeweyID gaps of this bracket key.
	 * 
	 * @return number of DeweyID gaps
	 */
	public int getIdGaps() {
		return idGaps;
	}

	/**
	 * Returns the length of the following data reference, depending on the type
	 * of this bracket key.
	 * 
	 * @return data reference length
	 */
	public int getDataReferenceLength() {
		return type.getDataReferenceLength();
	}

	/**
	 * Updates the key type of the bracket key at position 'keyPosition'.
	 * 
	 * @param newType
	 *            the new bracket key type
	 * @param storage
	 *            the byte array to change
	 * @param keyPosition
	 *            the position where the bracket key starts
	 * @return keyPosition + physical format length
	 */
	public static int updateType(BracketKey.Type newType, byte[] storage,
			int keyPosition) {

		// load correct byte
		keyPosition++;
		int typeByte = storage[keyPosition];

		// change key type
		storage[keyPosition] = (byte) ((typeByte & 252) | newType
				.getPhysicalValue());

		return keyPosition + 2;
	}

	/**
	 * Sets the number of round and angle brackets.
	 * 
	 * @param roundBrackets
	 *            the number of round brackets
	 * @param angleBrackets
	 *            the number of angle brackets
	 */
	public void setBrackets(int roundBrackets, int angleBrackets) {
		this.roundBrackets = roundBrackets;
		this.angleBrackets = angleBrackets;
	}
}
