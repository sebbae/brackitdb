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
package org.brackit.server.store.list.impl;

import org.brackit.xquery.util.log.Logger;
import org.brackit.server.io.buffer.PageID;
import org.brackit.server.io.manager.BufferMgr;
import org.brackit.server.store.Field;
import org.brackit.server.store.OpenMode;
import org.brackit.server.store.SearchMode;
import org.brackit.server.store.index.IndexAccessException;
import org.brackit.server.store.index.IndexIterator;
import org.brackit.server.store.index.aries.BPlusIndex;
import org.brackit.server.store.index.aries.IndexLockService;
import org.brackit.server.store.index.aries.IndexOperationException;
import org.brackit.server.store.index.aries.page.PageContext;
import org.brackit.server.store.list.ObjectStore;
import org.brackit.server.store.list.ObjectStoreAccessException;
import org.brackit.server.tx.Tx;
import org.brackit.server.util.Calc;
import org.brackit.xquery.xdm.Stream;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class IndexObjectStore extends BPlusIndex implements ObjectStore {
	private static final Logger log = Logger.getLogger(IndexObjectStore.class);

	public IndexObjectStore(BufferMgr bufferMgr, IndexLockService lockService) {
		super(bufferMgr, lockService);
	}

	public IndexObjectStore(BufferMgr bufferMgr) {
		super(bufferMgr);
	}

	@Override
	public int add(Tx transaction, PageID pageID, byte[] obj, boolean logged)
			throws ObjectStoreAccessException {
		if (log.isTraceEnabled()) {
			log.trace("Begin insert");
		}

		PageContext leaf = null;
		int id = -1;
		try {
			leaf = tree.descendToPosition(transaction, pageID, SearchMode.LAST,
					null, null, true, true);
			byte[] prevKey = leaf.getKey();
			id = (prevKey != null) ? (Calc.toUIntVar(prevKey) + 1) : 1;
			byte[] key = Calc.fromUIntVar(id);
			leaf = tree.insertIntoLeaf(transaction, pageID, leaf, key, obj,
					true, logged, transaction.checkPrevLSN());
			leaf.cleanup();
		} catch (IndexAccessException e) {
			throw new ObjectStoreAccessException(e);
		} catch (IndexOperationException e) {
			leaf.cleanup();
			throw new ObjectStoreAccessException(e);
		}

		if (log.isTraceEnabled()) {
			log.trace("End insert");
		}

		return id;
	}

	@Override
	public PageID create(Tx transaction, int containerNo)
			throws ObjectStoreAccessException {
		try {
			return createIndex(transaction, containerNo, Field.INTEGER,
					Field.BYTEARRAY, true);
		} catch (IndexAccessException e) {
			throw new ObjectStoreAccessException(e);
		}
	}

	@Override
	public void delete(Tx transaction, PageID pageID, int id, boolean logged)
			throws ObjectStoreAccessException {
		try {
			delete(transaction, pageID, Calc.fromUIntVar(id), null);
		} catch (IndexAccessException e) {
			throw new ObjectStoreAccessException(e);
		}
	}

	@Override
	public void drop(Tx transaction, PageID pageID)
			throws ObjectStoreAccessException {
		try {
			dropIndex(transaction, pageID);
		} catch (IndexAccessException e) {
			throw new ObjectStoreAccessException(e);
		}
	}

	@Override
	public Stream<byte[]> iterator(Tx transaction, PageID pageID)
			throws ObjectStoreAccessException {
		try {
			IndexIterator iterator = open(transaction, pageID,
					SearchMode.FIRST, null, null, OpenMode.READ);
			return new IndexIteratorStream(iterator);
		} catch (IndexAccessException e) {
			throw new ObjectStoreAccessException(e);
		}
	}

	@Override
	public void replace(Tx transaction, PageID pageID, int id, byte[] obj,
			boolean logged) throws ObjectStoreAccessException {
		try {
			update(transaction, pageID, Calc.fromUIntVar(id), null, obj);
		} catch (IndexAccessException e) {
			throw new ObjectStoreAccessException(e);
		}
	}
}
