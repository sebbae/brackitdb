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
package org.brackit.server.store.index.aries.log;

import java.nio.ByteBuffer;

import org.brackit.server.io.buffer.PageID;
import org.brackit.server.store.Field;
import org.brackit.server.store.index.IndexAccessException;
import org.brackit.server.store.index.aries.BPlusTree;
import org.brackit.server.store.index.aries.IndexOperationException;
import org.brackit.server.store.index.aries.PageType;
import org.brackit.server.store.index.aries.page.PageContext;
import org.brackit.server.tx.Tx;
import org.brackit.server.tx.log.LogException;
import org.brackit.server.tx.log.SizeConstants;
import org.brackit.xquery.util.log.Logger;

/**
 * @author Sebastian Baechle
 * 
 */
public class FormatLogOperation extends BPlusIndexLogOperation {
	private static final Logger log = Logger
			.getLogger(FormatLogOperation.class);

	private static final int SIZE = BASE_SIZE
			+ (10 * SizeConstants.BYTE_SIZE);

	private byte oldPageType;

	private byte pageType;

	private Field oldKeyType;

	private Field keyType;

	private Field oldValueType;

	private Field valueType;

	private boolean oldUnique;

	private boolean unique;

	private boolean oldCompression;

	private boolean compression;

	public FormatLogOperation(PageID pageID,
			PageID rootPageID, int oldPageType, int pageType, Field oldKeyType,
			Field keyType, Field oldValueType, Field valueType,
			boolean oldUnique, boolean unique, boolean oldCompression,
			boolean compression) {
		super(FORMAT, pageID, rootPageID);
		this.pageType = (byte) pageType;
		this.oldPageType = (byte) oldPageType;
		this.keyType = keyType;
		this.oldKeyType = oldKeyType;
		this.oldUnique = oldUnique;
		this.oldValueType = oldValueType;
		this.unique = unique;
		this.oldCompression = oldCompression;
		this.compression = compression;
		this.valueType = valueType;
	}

	@Override
	public int getSize() {
		return SIZE;
	}

	@Override
	public void toBytes(ByteBuffer bb) {
		super.toBytes(bb);
		bb.put(oldPageType);
		bb.put(pageType);
		bb.put((byte) oldKeyType.ID);
		bb.put((byte) keyType.ID);
		bb.put((byte) oldValueType.ID);
		bb.put((byte) valueType.ID);
		bb.put((byte) ((oldUnique) ? 1 : 0));
		bb.put((byte) ((unique) ? 1 : 0));
		bb.put((byte) ((oldCompression) ? 1 : 0));
		bb.put((byte) ((compression) ? 1 : 0));
	}

	@Override
	public void redo(Tx tx, long LSN) throws LogException {
		try {
			redoFormatPage(tx, pageID, rootPageID, pageType, keyType,
					valueType, unique, compression, LSN);
		} catch (IndexAccessException e) {
			throw new LogException(e, "Redo of format page %s failed.", pageID);
		}
	}

	@Override
	public void undo(Tx tx, long LSN, long undoNextLSN) throws LogException {
		try {
			undoFormatPage(tx, pageID, rootPageID, oldPageType,
					oldKeyType, oldValueType, oldUnique, oldCompression, LSN,
					undoNextLSN);
		} catch (IndexAccessException e) {
			throw new LogException(e, "Undo of format page %s failed.", pageID);
		}
	}

	public void undoFormatPage(Tx tx, PageID pageID,
			PageID rootPageID, int pageType, Field keyType, Field valueType,
			boolean unique, boolean compression, long LSN, long undoNextLSN)
			throws IndexAccessException {
		PageContext page = null;

		if (log.isTraceEnabled()) {
			log.trace("Begin undo format page");
		}

		try {
			page = new BPlusTree(tx.getBufferManager()).getPage(tx, pageID,
					true, false);

			if (((page.getPageType() != PageType.INDEX_LEAF) && (page
					.getPageType() != PageType.INDEX_TREE))
					|| (!page.getRootPageID().equals(rootPageID))) {
				page.cleanup();
				throw new IndexAccessException(
						"Undo format page %s failed because it does not belong to index %s.",
						pageID, rootPageID);
			}

			page.format(pageType, rootPageID, keyType, valueType,
					unique, compression, true, undoNextLSN);
			page.cleanup();
		} catch (IndexOperationException e) {
			throw new IndexAccessException(
					e,
					"Undo format page %s failed because it has been deleted. This must not happen in the ARIES protocol.",
					pageID);
		}

		if (log.isTraceEnabled()) {
			log.trace("End undo format page");
		}
	}

	public void redoFormatPage(Tx tx, PageID pageID,
			PageID rootPageID, int pageType, Field keyType, Field valueType,
			boolean unique, boolean compression, long LSN)
			throws IndexAccessException {
		PageContext page = null;

		if (log.isTraceEnabled()) {
			log.trace("Begin redo format page");
		}

		try {
			page = new BPlusTree(tx.getBufferManager()).getPage(tx, pageID,
					true, false);
		} catch (IndexOperationException e) {
			if (log.isDebugEnabled()) {
				log
						.trace(String
								.format(
										"Page %s has been deleted and flushed to disk. No redo necessary.",
										pageID));
			}

			return;
		}

		try {
			if (page.getLSN() < LSN) {
				if ((page.getRootPageID() != null)
						&& (!page.getRootPageID().equals(rootPageID))) {
					page.cleanup();
					throw new IndexAccessException(
							"Redo format page %s failed because it does not belong to index %s.",
							pageID, rootPageID);
				}

				page.format(pageType, rootPageID, keyType, valueType,
						unique, compression, false, -1);
			} else {
				if (log.isTraceEnabled()) {
					log
							.trace(String
									.format(
											"LSN %s of page %s is >= RedoLSN %s. No redo necessary.",
											page.getLSN(), pageID, LSN));
				}
			}

			page.cleanup();
		} catch (IndexOperationException e) {
			throw new IndexAccessException(e, "Redo format page %s failed.",
					pageID);
		}

		if (log.isTraceEnabled()) {
			log.trace("End redo format page");
		}
	}

	@Override
	public String toString() {
		return String.format("%s(%s %s->%s (%s) for index %s)", getClass()
				.getSimpleName(), pageID, keyType, valueType, unique ? "unique"
				: "non-unique", rootPageID);
	}
}
