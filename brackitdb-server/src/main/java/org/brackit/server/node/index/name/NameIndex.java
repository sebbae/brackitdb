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
package org.brackit.server.node.index.name;

import org.brackit.server.node.XTCdeweyID;
import org.brackit.server.node.index.definition.IndexDef;
import org.brackit.server.node.index.name.impl.NameDirectoryEncoderImpl.QVocID;
import org.brackit.server.node.txnode.IndexEncoderHelper;
import org.brackit.server.store.SearchMode;
import org.brackit.server.tx.Tx;
import org.brackit.xquery.node.parser.ListenMode;
import org.brackit.xquery.node.parser.SubtreeListener;
import org.brackit.xquery.xdm.DocumentException;
import org.brackit.xquery.xdm.Node;
import org.brackit.xquery.xdm.Stream;

/**
 * @author Sebastian Baechle
 * 
 */
public interface NameIndex<E extends Node<E>> {
	public SubtreeListener<? super E> createBuilder(Tx tx,
			IndexEncoderHelper<E> helper, int containerNo, IndexDef idxDef)
			throws DocumentException;

	public Stream<? extends E> open(Tx tx, IndexEncoderHelper<E> helper,
			int nameIndexNo, QVocID qVocID, SearchMode searchMode,
			XTCdeweyID deweyID) throws DocumentException;

	public SubtreeListener<? super E> createListener(Tx tx,
			IndexEncoderHelper<E> helper, ListenMode mode, IndexDef idxDef)
			throws DocumentException;

	public void drop(Tx tx, int indexNo) throws DocumentException;

	public void calculateStatistics(Tx tx, IndexDef idxDef)
			throws DocumentException;
}
