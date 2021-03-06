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
package org.brackit.server.metadata.pathSynopsis.converter;

import java.util.Collection;
import java.util.Map;

import org.brackit.server.metadata.pathSynopsis.NsMapping;
import org.brackit.server.metadata.pathSynopsis.manager.PathSynopsisNode;
import org.brackit.server.tx.log.SizeConstants;
import org.brackit.server.util.Calc;
import org.brackit.xquery.xdm.Kind;

/**
 * 
 * @author Sebastian Baechle
 * @author Karsten Schmidt
 * 
 */
public class PSNodeRecordAccess {
	public byte[] encodeKey(PathSynopsisNode node) {
		return Calc.fromUIntVar(node.getPCR());
	}

	// TODO: encode root's parent pcr with 0 instead of "null" and skip length
	// encoding zero-length!
	public byte[] encodeValue(PathSynopsisNode node) {
		byte[] uriVocID = (node.getURIVocID() != -1 ? Calc.fromUIntVar(node.getURIVocID()) : new byte[0]);
		byte[] prefixVocID = (node.getPrefixVocID() != -1 ? Calc.fromUIntVar(node.getPrefixVocID()) : new byte[0]);
		byte[] localNameVocID = Calc.fromUIntVar(node.getLocalNameVocID());
		PathSynopsisNode parent = node.getParent();
		byte[] parentPCR = (parent != null) ? Calc.fromUIntVar(parent.getPCR())
				: null;
		
		// encode namespace mapping
		int nsMappingLength = 0;
		NsMapping nsMapping = node.getNsMapping();
		Collection<Map.Entry<Integer, Integer>> entrySet = null;
		if (nsMapping != null) {
			entrySet = nsMapping.getEntrySet();
			nsMappingLength = 2 * entrySet.size() * SizeConstants.INT_SIZE;
		}

		int vLength1 = uriVocID.length;
		int vLength2 = prefixVocID.length;
		int vLength3 = localNameVocID.length;
		int pLength = (parentPCR != null) ? parentPCR.length : 0;
		byte[] nodeBytes = new byte[2 + vLength1 + vLength2 + vLength3 + pLength + nsMappingLength];

		int pos = 2;
		System.arraycopy(uriVocID, 0, nodeBytes, pos, vLength1);
		pos += vLength1;
		System.arraycopy(prefixVocID, 0, nodeBytes, pos, vLength2);
		pos += vLength2;
		System.arraycopy(localNameVocID, 0, nodeBytes, pos, vLength3);
		pos += vLength3;

		if (parentPCR != null) {
			System.arraycopy(parentPCR, 0, nodeBytes, pos, pLength);
			pos += pLength;
		}
		
		if (nsMappingLength > 0) {
			// store vocID mapping
			for (Map.Entry<Integer, Integer> entry : entrySet) {
				Calc.fromInt(entry.getKey(), nodeBytes, pos);
				pos += SizeConstants.INT_SIZE;
				Calc.fromInt(entry.getValue(), nodeBytes, pos);
				pos += SizeConstants.INT_SIZE;
			}
		}

		nodeBytes[0] = node.getKind();
		nodeBytes[1] = encodeLengthByte(vLength1, vLength2, vLength3, pLength);

		return nodeBytes;
	}
	
	protected byte encodeLengthByte(int uriVocIdLength, int prefixVocIdLength, int localNameVocIdLength, int parentPcrLength) {
		localNameVocIdLength--;
		
		if (uriVocIdLength > 3 || prefixVocIdLength > 3 || localNameVocIdLength > 3 || parentPcrLength > 3) {
			throw new RuntimeException("VocID Overflow!");
		}
		
		int lengthByte = 0;
		lengthByte |= (uriVocIdLength << 6);
		lengthByte |= (prefixVocIdLength << 4);
		lengthByte |= (localNameVocIdLength << 2);
		lengthByte |= (parentPcrLength);
		return (byte) lengthByte;
	}
	
	protected static int[] decodeLengthByte(byte lengthByte) {
		return new int[] {
				((lengthByte >> 6) & 3),
				((lengthByte >> 4) & 3),
				((lengthByte >> 2) & 3) + 1,
				((lengthByte) & 3)
		};
	}

	public static String valueToString(byte[] value) {
		int type = value[0];
		int[] info = decodeLengthByte(value[1]);
		int offset = 2;
		int uriVocID = (info[0] == 0 ? -1 : Calc.toInt(value, offset, info[0]));
		offset += info[0];
		int prefixVocID = (info[1] == 0 ? -1 : Calc.toInt(value, offset, info[1]));
		offset += info[1];
		int localNameVocID = Calc.toInt(value, offset, info[2]);
		offset += info[2];
		int parentPCR = -1;

		if (info[3] != 0) {
			parentPCR = Calc.toInt(value, offset, info[3]);
		}

		if (type == Kind.ELEMENT.ID) {
			return String.format("(%s,%s,%s)(parent:%s)", uriVocID, prefixVocID, localNameVocID, parentPCR);
		} else {
			return String.format("@(%s,%s,%s)(parent:%s)", uriVocID, prefixVocID, localNameVocID, parentPCR);
		}
	}
}
