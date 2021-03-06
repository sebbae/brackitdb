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
package org.brackit.server.store.index.bracket.page;

import java.util.List;

import org.brackit.server.io.buffer.PageID;
import org.brackit.server.node.XTCdeweyID;
import org.brackit.server.node.bracket.BracketNode;
import org.brackit.server.store.index.bracket.HintPageInformation;
import org.brackit.server.store.index.bracket.IndexOperationException;
import org.brackit.server.store.index.bracket.NavigationMode;
import org.brackit.server.store.index.bracket.SubtreeDeleteListener;
import org.brackit.server.store.index.bracket.filter.BracketFilter;
import org.brackit.server.store.page.bracket.BracketNodeSequence;
import org.brackit.server.store.page.bracket.DeleteSequenceInfo;
import org.brackit.server.store.page.bracket.DeweyIDBuffer;
import org.brackit.server.store.page.bracket.ExternalValueLoader;
import org.brackit.server.store.page.bracket.RecordInterpreter;
import org.brackit.server.store.page.bracket.navigation.NavigationStatus;

/**
 * @author Martin Hiller
 * 
 */
public interface Leaf extends BPContext {

	public void format(PageID rootPageID, boolean logged,
			long undoNextLSN) throws IndexOperationException;

	/**
	 * Navigates along the navigation axis specified in the NavigationMode
	 * argument. The hintParentDeweyID is optional to make the 'previousSibling'
	 * navigation more efficient.
	 * 
	 * @param navMode
	 *            the axis to navigate on
	 * @return the Navigation Status
	 * @throws IndexAccessException
	 */
	public NavigationStatus navigate(NavigationMode navMode);

	/**
	 * Navigates to the first child. If it is clear that there is no first
	 * child (NOT_EXISTENT) and the 'returnNodeOnFailure' flag is set, we will
	 * however step to the next node that does not belong to the current
	 * subtree (i.e. the node where we expected to be the first child).
	 */
	public NavigationStatus navigateFirstChild(boolean returnNodeOnFailure);

	/**
	 * Navigates to the next sibling. If it is clear that there is no next
	 * sibling (NOT_EXISTENT) and the 'returnNodeOnFailure' flag is set, we will
	 * however step to the next node that does not belong to the current
	 * subtree (i.e. the node where we expected to be the next sibling).
	 */
	public NavigationStatus navigateNextSibling(boolean returnNodeOnFailure);

	/**
	 * Navigates along the navigation axis specified in the NavigationMode
	 * argument. The hintParentDeweyID is optional to make the 'previousSibling'
	 * navigation more efficient.
	 * 
	 * @param referenceDeweyID
	 *            the source DeweyID for the navigation operation
	 * @param navMode
	 *            the axis to navigate on
	 * @return the Navigation Status
	 * @throws IndexAccessException
	 */
	public NavigationStatus navigateContextFree(XTCdeweyID referenceDeweyID,
			NavigationMode navMode);

	public void moveBeforeFirst();

	public boolean moveNextToLastRecord() throws IndexOperationException;

	public boolean moveToSplitPosition(float occupancyRate)
			throws IndexOperationException;

	/**
	 * 
	 * @param deweyID
	 * @param record
	 * @param ancestorsToInsert
	 * @param logged
	 * @param undoNextLSN
	 * @param bulkLog
	 *            if logged and bulkLog are set to true, delayed logging will be
	 *            performed (i.e. no log record is written within this method,
	 *            but at a later point in time)
	 * @return
	 * @throws IndexOperationException
	 */
	public boolean insertRecordAfter(XTCdeweyID deweyID, byte[] record,
			int ancestorsToInsert, boolean logged, long undoNextLSN,
			boolean bulkLog) throws IndexOperationException;

	/**
	 * Writes an INSERT log for the collected nodes in the 'insertRecordAfter'
	 * or 'insertSequenceAfter' methods. May only be used in combination with
	 * the above-mentioned methods.
	 */
	public void bulkLog(boolean writeUndoNextLSN, long undoNextLSN)
			throws IndexOperationException;

	public boolean insertRecord(XTCdeweyID deweyID, byte[] record,
			int ancestorsToInsert, boolean logged, long undoNextLSN)
			throws IndexOperationException;

	public boolean insertSequenceAfter(BracketNodeSequence nodes, boolean SMO,
			boolean logged, long undoNextLSN, boolean bulkLog)
			throws IndexOperationException;

	public boolean insertSequence(BracketNodeSequence nodes, boolean SMO,
			boolean logged, long undoNextLSN) throws IndexOperationException;

	public XTCdeweyID getKey() throws IndexOperationException;

	public PageID getNextPageID() throws IndexOperationException;

	public void setNextPageID(PageID nextPageID, boolean logged,
			long undoNextLSN) throws IndexOperationException;

	public boolean isBeforeFirst();

	public boolean isFirst();

	public void setContext(XTCdeweyID deweyID, int offset);

	public void setContext(BracketContext context);

	public BracketContext getContext();

	public int getOffset();

	/**
	 * Deletes and returns all nodes following the current position.
	 * 
	 * @param logged
	 * @param undoNextLSN
	 * @return
	 * @throws IndexOperationException
	 */
	public BracketNodeSequence deleteSequenceAfter(boolean SMO, boolean logged,
			long undoNextLSN) throws IndexOperationException;

	public XTCdeweyID getLowKey();

	public byte[] getLowKeyBytes();

	public boolean setHighKey(XTCdeweyID highKey, boolean logged,
			long undoNextLSN) throws IndexOperationException;

	public boolean setHighKeyBytes(byte[] highKeyBytes, boolean logged,
			long undoNextLSN) throws IndexOperationException;

	public XTCdeweyID getHighKey();

	public byte[] getHighKeyBytes();

	/**
	 * Deletes current node/subtree. Returns true if deletion of subtree is
	 * completed, and false if the next page has to be inspected. If the
	 * externalPageIDs list is specified (not null), external values are NOT
	 * deleted within this method. Instead they are inserted into this list for
	 * later deletion.
	 * 
	 * @param deleteListener
	 * @param externalPageIDs
	 *            a list for collecting external values
	 * @param logged
	 * @return true if deletion of subtree is completed, and false if the next
	 *         page has to be inspected
	 * @throws IndexOperationException
	 * @throws EmptyLeafException
	 *             if the deletion would cause this leaf to become empty (->
	 *             deletion not yet executed)
	 */
	public boolean deleteSubtreeStart(SubtreeDeleteListener deleteListener,
			List<PageID> externalPageIDs, boolean logged)
			throws IndexOperationException;

	/**
	 * Deletes all nodes from the beginning of this page until the specified
	 * subtree ends. Returns true if subtree end was found in this page, and
	 * false if this page needs to be unchained. If the externalPageIDs list is
	 * specified (not null), external values are NOT deleted within this method.
	 * Instead they are inserted into this list for later deletion.
	 * 
	 * @param subtreeRoot
	 * @param deleteListener
	 * @param externalPageIDs
	 *            a list for collecting external values
	 * @param logged
	 * @return
	 * @throws IndexOperationException
	 */
	public boolean deleteSubtreeEnd(XTCdeweyID subtreeRoot,
			SubtreeDeleteListener deleteListener, List<PageID> externalPageIDs,
			boolean logged) throws IndexOperationException;

	/**
	 * Assigns the given DeweyIDBuffer to this leaf.
	 * 
	 * @param deweyIDBuffer
	 *            the DeweyIDBuffer that is supposed to be used by this leaf
	 */
	public void assignDeweyIDBuffer(DeweyIDBuffer deweyIDBuffer);

	/**
	 * Assigns the DeweyIDBuffer of the given leaf to this leaf.
	 * 
	 * @param other
	 *            the other leaf's DeweyIDBuffer that is supposed to be used by
	 *            this leaf
	 */
	public void assignDeweyIDBuffer(Leaf other);

	/**
	 * Deassigns the DeweyIDBuffer from this page and returns the newly
	 * available buffer. After this deassignment, this leaf will point before
	 * the first node.
	 * 
	 * @return the deassigned DeweyIDBuffer
	 */
	public DeweyIDBuffer deassignDeweyIDBuffer();

	public DeweyIDBuffer getDeweyIDBuffer();

	/**
	 * Deletes a node sequence beginning with "leftBorderDeweyID" until (and
	 * inclusive) "rightBorderDeweyID". If the deletion results in an empty
	 * page, nothing will be deleted. Instead, the tree needs to unchain this
	 * page.
	 * 
	 * @param leftBorderDeweyID
	 * @param rightBorderDeweyID
	 * @param logged
	 * @param undoNextLSN
	 * @return
	 * @throws IndexOperationException
	 */
	public DeleteSequenceInfo deleteSequence(XTCdeweyID leftBorderDeweyID,
			XTCdeweyID rightBorderDeweyID, boolean SMO, boolean logged,
			long undoNextLSN) throws IndexOperationException;

	/**
	 * Deletes all nodes from this page. Highkey and other header data will be
	 * preserved.
	 */
	public BracketNodeSequence clearData(boolean SMO, boolean logged,
			long undoNextLSN) throws IndexOperationException;

	public HintPageInformation getHintPageInformation();

	public boolean setValue(byte[] value, boolean logged, long undoNextLSN)
			throws IndexOperationException;

	/**
	 * @return true iff this page context points to an attribute node
	 */
	public boolean isAttribute();

	/**
	 * @return the level of the node this context points to (remark: attributes
	 *         are considered to be on the same level like their element)
	 */
	public int getLevel();

	/**
	 * Loads the BracketNode this context points to.
	 */
	public BracketNode load(BracketNodeLoader loader)
			throws IndexOperationException;

	/**
	 * Returns a record (interpreter) for the current context.
	 */
	public RecordInterpreter getRecord() throws IndexOperationException;

	/**
	 * Moves to the next non-attribute node within this document.
	 */
	public NavigationStatus moveNextNonAttrInDocument();

	/**
	 * Moves to the next attribute (of the current element node). Can return
	 * FOUND, NOT_EXISTENT (if the next element is reached) or AFTER_LAST (if
	 * there may be another attribute in the next page)
	 */
	public NavigationStatus moveNextAttribute();

	/**
	 * Creates a new leaf context for this page in READ mode (e.g. shared
	 * latch). The corresponding page handle will therefore be fixed and latched
	 * again.
	 */
	public NavigationStatus moveNextInDocument();

	/**
	 * Creates a new leaf context for this page in READ mode (e.g. shared
	 * latch). The corresponding page handle will therefore be fixed and latched
	 * again.
	 */
	public Leaf fork() throws IndexOperationException;

	/**
	 * Checks whether the current node (this page is pointing to) is accepted by
	 * the given filter.
	 */
	public boolean accept(BracketFilter filter) throws IndexOperationException;

	/**
	 * Returns the external value loader of this page.
	 */
	public ExternalValueLoader getExternalValueLoader();
}
